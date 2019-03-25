package org.iii.sso;

import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES {

	public static Optional<byte[]> Encrypt(String sSrc, String sKey) throws Exception {
		if (sKey == null) {
			System.out.println("key is null");
			return Optional.empty();
		}
		// check key size
		if (sKey.length() < 24) {
			System.out.println(" key size must be length greater then 24");
			return Optional.empty();
		}
		byte[] raw = sKey.getBytes();
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

		return Optional.of(cipher.doFinal(sSrc.getBytes("UTF-8")));
	}

}
