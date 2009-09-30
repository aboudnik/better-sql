/*
 * Copyright (c) 2009 Alexandre Boudnik (shr). All rights reserved.
 */

package org.boudnik.better.sql.qa;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.CipherInputStream;
import java.io.*;
import java.security.*;

/**
 * @author shr
 * @since Mar 9, 2006 8:47:53 AM
 */
public class Crypto {
    private static KeyPairGenerator keyGen;
    private static byte[] secretMessage = "XEP.OTOPBAH".getBytes();

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException, NoSuchPaddingException, IOException {
        init();
//        signAndVerify();
        encAndDec();
    }

    private static void encAndDec() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException {
        KeyPair pair = keyGen.generateKeyPair();
        Cipher co = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        co.init(Cipher.ENCRYPT_MODE, pair.getPublic());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherOutputStream cos = new CipherOutputStream(bos, co);
        cos.write(secretMessage);
        cos.close();
        Cipher ci = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        ci.init(Cipher.DECRYPT_MODE, pair.getPrivate());
        CipherInputStream cis = new CipherInputStream(new ByteArrayInputStream(bos.toByteArray()), ci);
        for (int c; (c = cis.read()) >= 0;)
            System.out.write(c);
        System.out.println();
    }

    private static void signAndVerify() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        KeyPair pair = keyGen.generateKeyPair();
        Signature signer = Signature.getInstance("SHA1withRSA");

        signer.initSign(pair.getPrivate());

        signer.update(secretMessage);
        byte[] signature = signer.sign();
        signer.initVerify(pair.getPublic());
        signer.update(secretMessage);
        boolean verifies = signer.verify(signature);

        System.out.println("signature verifies: " + verifies);
    }

    private static void init() throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {
        keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        random.setSeed("jopa".getBytes());
        keyGen.initialize(1024, random);
        signAndVerify();
    }
}
