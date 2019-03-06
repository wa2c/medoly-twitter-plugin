package com.wa2c.android.medoly.plugin.action.tweet

import android.content.Context
import android.util.Base64

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

/**
 * Created by wa2c on 2015/04/09.
 */
object Token {

    /**
     * AES
     */
    private const val AES = "AES"

    /**
     * Get api key.
     */
    fun getConsumerKey(): String? {
        return try {
            val k = BuildConfig.K1 + "__" + BuildConfig.K2
            decrypt(BuildConfig.T1, k)
        } catch (e: Exception) {
            null
        }

    }

    /**
     * Get secret key.
     */
    fun getConsumerSecret(): String? {
        return try {
            val k = BuildConfig.K1 + "__" + BuildConfig.K2
            decrypt(BuildConfig.T2, k)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypt key.
     */
    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, IllegalBlockSizeException::class, BadPaddingException::class)
    private fun encrypt(originalString: String, secretKey: String): String {

        val originalBytes = originalString.toByteArray()
        val secretKeyBytes = secretKey.toByteArray()

        val secretKeySpec = SecretKeySpec(secretKeyBytes, AES)
        val cipher = Cipher.getInstance(AES)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encryptBytes = cipher.doFinal(originalBytes)
        val encryptBytesBase64 = Base64.encode(encryptBytes, Base64.DEFAULT)
        return String(encryptBytesBase64)
    }

    /**
     * Decrypt key.
     */
    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, IllegalBlockSizeException::class, BadPaddingException::class)
    private fun decrypt(encryptBytesBase64String: String, secretKey: String): String {

        val encryptBytes = Base64.decode(encryptBytesBase64String, Base64.DEFAULT)
        val secretKeyBytes = secretKey.toByteArray()

        val secretKeySpec = SecretKeySpec(secretKeyBytes, AES)
        val cipher = Cipher.getInstance(AES)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val originalBytes = cipher.doFinal(encryptBytes)
        return String(originalBytes)
    }

}
