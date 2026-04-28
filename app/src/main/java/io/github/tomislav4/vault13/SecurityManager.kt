package io.github.tomislav4.vault13

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {
    private const val KEY_ALIAS = "Vault13SecretKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    
    private var sessionKey: SecretKey? = null

    fun init(context: Context) {
        generateHardwareKeyIfNeeded(context)
    }

    private fun generateHardwareKeyIfNeeded(context: Context) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val hasStrongBox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
                if (hasStrongBox) {
                    builder.setIsStrongBoxBacked(true)
                }
            }

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
    }

    private fun getHardwareKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun deriveSessionKey(password: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        sessionKey = SecretKeySpec(hash, "AES")
    }

    fun clearSession() {
        sessionKey = null
    }

    fun isLocked(): Boolean = sessionKey == null

    fun isPasswordSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences("vault_auth", Context.MODE_PRIVATE)
        return prefs.contains("pwd_hash")
    }

    fun setPassword(password: String, context: Context) {
        val prefs = context.getSharedPreferences("vault_auth", Context.MODE_PRIVATE)
        val hash = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(password.toByteArray()),
            Base64.DEFAULT
        )
        prefs.edit().putString("pwd_hash", hash).apply()
        deriveSessionKey(password)
    }

    fun encrypt(data: String): String {
        val key = sessionKey ?: throw IllegalStateException("Cryptographic Warden: Session locked.")
        
        // Layer 1: App Password (Volatile Session Key)
        val cipher1 = Cipher.getInstance(TRANSFORMATION)
        cipher1.init(Cipher.ENCRYPT_MODE, key)
        val iv1 = cipher1.iv
        val encrypted1 = cipher1.doFinal(data.toByteArray(Charsets.UTF_8))
        
        // Layer 2: Hardware-Backed Key (TEE/StrongBox)
        val cipher2 = Cipher.getInstance(TRANSFORMATION)
        cipher2.init(Cipher.ENCRYPT_MODE, getHardwareKey())
        val iv2 = cipher2.iv
        val encrypted2 = cipher2.doFinal(encrypted1)
        
        // Format: [IV2][IV1][EncryptedData]
        val combined = ByteArray(iv2.size + iv1.size + encrypted2.size)
        System.arraycopy(iv2, 0, combined, 0, iv2.size)
        System.arraycopy(iv1, 0, combined, iv2.size, iv1.size)
        System.arraycopy(encrypted2, 0, combined, iv2.size + iv1.size, encrypted2.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedDataWithIv: String): String {
        val key = sessionKey ?: throw IllegalStateException("Cryptographic Warden: Session locked.")
        val combined = Base64.decode(encryptedDataWithIv, Base64.DEFAULT)
        
        val ivSize = 12
        val iv2 = combined.sliceArray(0 until ivSize)
        val iv1 = combined.sliceArray(ivSize until ivSize * 2)
        val encrypted2 = combined.sliceArray(ivSize * 2 until combined.size)
        
        // Decrypt Layer 2 (Hardware)
        val cipher2 = Cipher.getInstance(TRANSFORMATION)
        cipher2.init(Cipher.DECRYPT_MODE, getHardwareKey(), GCMParameterSpec(128, iv2))
        val decrypted2 = cipher2.doFinal(encrypted2)
        
        // Decrypt Layer 1 (Password)
        val cipher1 = Cipher.getInstance(TRANSFORMATION)
        cipher1.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv1))
        
        return String(cipher1.doFinal(decrypted2), Charsets.UTF_8)
    }
    
    fun verifyPassword(password: String, context: Context): Boolean {
        val prefs = context.getSharedPreferences("vault_auth", Context.MODE_PRIVATE)
        val savedHash = prefs.getString("pwd_hash", null) ?: return false
        
        val currentHash = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(password.toByteArray()),
            Base64.DEFAULT
        )
        
        return if (savedHash == currentHash) {
            deriveSessionKey(password)
            true
        } else {
            false
        }
    }
}
