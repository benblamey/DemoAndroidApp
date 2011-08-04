package com.ml4d.ohow;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.google_import.Base64;

/**
 * Performs symmetric encryption/decryption with a fixed key.
 * 
 * This type is not inside the com.ml4d.core package because it contains the symmetric key in source code.
 */
public class CryptUtility {

	/**
	 * Including the key in this way is obviously fairly weak, it just provides a simple defence.
	 */
	private static final byte[] _keyBytes = new byte[] { 0xA, -0x09, 0x0a, 0x0b, -0x1D, 0x15, 0x29, 0x77, 0x51, 0x21,
			0x02, -0x03, 0x2C, -0x0D, 0x4E, 0x3D, 0x43, -0x11, 0x7C, 0x63, 0x6A, 0x15, 0x16, 0x17 };

	/**
	 * Encrypts the specified string and returns the cipher-text, itself Base64 encoded. 
	 * @param data
	 * @return
	 */
	public static String encrypt(String data) {
		try {
			SecretKeySpec key = new SecretKeySpec(_keyBytes, "AES");
			byte[] input = data.getBytes("UTF16");

			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, key);

			byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
			int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
			ctLength += cipher.doFinal(cipherText, ctLength);

			String base64ct = Base64.encodeToString(cipherText, Base64.DEFAULT);

			return base64ct;
		} catch (UnsupportedEncodingException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (IllegalBlockSizeException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (ShortBufferException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (BadPaddingException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchProviderException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchPaddingException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (InvalidKeyException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
	}

	/**
	 * Decrypts data that was previously encrypted using this class.
	 * @param data
	 * @return
	 */
	public static String decrypt(String data) {
		try {
			SecretKeySpec key = new SecretKeySpec(_keyBytes, "AES");
			byte[] ct = Base64.decode(data, Base64.DEFAULT);

			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainText = new byte[cipher.getOutputSize(ct.length)];
			int ptLength = cipher.update(ct, 0, ct.length, plainText, 0);
			ptLength += cipher.doFinal(plainText, ptLength);
			return new String(plainText, 0, ptLength, "UTF16");

		} catch (UnsupportedEncodingException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (IllegalBlockSizeException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (ShortBufferException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (BadPaddingException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchProviderException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchPaddingException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (InvalidKeyException e) {
			throw new ImprobableCheckedExceptionException(e);
		}

	}

}