package com.bookify.pki.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

//Ova klasa sadrži statične metode koje se koriste za generisanje i čitanje ključeva
@Component
public class KeyUtils {

    @Autowired
    private ResourceLoader resourceLoader;

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(2048, random);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PrivateKey readPrivateKey(String filePath){

        StringBuilder privateKeyContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                privateKeyContent.append(line).append("\n");
            }

            // Remove PEM header and footer
            String privateKeyPEM = privateKeyContent.toString()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode Base64-encoded data
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyPEM);

            // Convert to PrivateKey object
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(keySpec);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read private key file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: RSA", e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Invalid Key Spec", e);
        }

    }
}
