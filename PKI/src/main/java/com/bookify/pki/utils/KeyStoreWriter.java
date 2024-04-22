package com.bookify.pki.utils;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


@Component
public class KeyStoreWriter {

    //Ova klasa se koristi za manipulaciju (čitanje, pisanje, brisanje) podataka u Java KeyStore fajlu

    private KeyStore keyStore;

    //Prilikom inicijalizacije, kreira instancu KeyStore objekta za pisanje u .jks datoteku
    public KeyStoreWriter() {
        try {
            keyStore = KeyStore.getInstance("JKS", "SUN");
        } catch (KeyStoreException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    //Učitava keystore iz datoteke sa datim imenom i šifrom.
    public void loadKeyStore(String fileName, char[] password) {
        try {
            if(fileName != null) {
                keyStore.load(new FileInputStream(fileName), password);
            } else {
                keyStore.load(null, password);
            }
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }
    }
    //Čuva keystore u datoteku sa datim imenom i šifrom.
    public void saveKeyStore(String fileName, char[] password) {
        try {
            keyStore.store(new FileOutputStream(fileName), password);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }
    }

    //Briše sertifikat iz keystore fajla na osnovu datog aliasa.
    public void deleteCertificate(String alias){
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

//    public void write(String alias, PrivateKey privateKey, char[] password, Certificate certificate) {
//        try {
//            keyStore.setKeyEntry(alias, privateKey, password, new Certificate[] {certificate});
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        }
//    }

    public void write(String alias, X509Certificate certificate){
        try {
            keyStore.setCertificateEntry(alias, certificate);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

    }
}
