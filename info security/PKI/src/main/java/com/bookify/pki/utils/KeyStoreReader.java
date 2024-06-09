package com.bookify.pki.utils;

import com.bookify.pki.model.Issuer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

@Component
public class KeyStoreReader {

    //sertifikate cuvamo u key-store fajlovima (ili kljuceve??) tj sertifikati koji ukljucuju javni kljuc
    //ili privatni kljuc
    //a sta mi cuvamo??
    //KeyStore je java klasa za citanje tih datoteka
    private KeyStore keyStore;

    @Autowired
    private ResourceLoader resourceLoader;

    public KeyStoreReader() {
        try {
            keyStore = KeyStore.getInstance("JKS", "SUN");
        } catch (KeyStoreException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    /**
     * Zadatak ove funkcije jeste da ucita podatke o izdavaocu i odgovarajuci privatni kljuc.
     * Ovi podaci se mogu iskoristiti da se novi sertifikati izdaju.
     *
     * @param keyStoreFile - datoteka odakle se citaju podaci
     * @param alias - alias putem kog se identifikuje sertifikat izdavaoca
     * @param password - lozinka koja je neophodna da se otvori key store
     * @param keyPass - lozinka koja je neophodna da se izvuce privatni kljuc
     * @return - podatke o izdavaocu i odgovarajuci privatni kljuc
     *
     *
     *
     * Ova metoda se koristi za čitanje podataka o izdavaocu (issuer) i odgovarajućem privatnom ključu
     * iz keystore fajla.
     * Učitava keystore iz datoteke, izvlači sertifikat na osnovu datog aliasa, izvlači privatni ključ
     * koji odgovara tom aliasu i vraća objekat Issuer koji sadrži privatni ključ, javni ključ
     * i ime izdavaoca.
     */
    public Issuer readIssuerFromStore(String keyStoreFile, String alias, char[] password, char[] keyPass) {
        try {
            //Datoteka se ucitava
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile));
            keyStore.load(in, password);

            //Iscitava se sertifikat koji ima dati alias
            Certificate cert = keyStore.getCertificate(alias);

            //Iscitava se privatni kljuc vezan za javni kljuc koji se nalazi na sertifikatu sa datim aliasom
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPass);

            X500Name issuerName = new JcaX509CertificateHolder((X509Certificate) cert).getSubject();
            return new Issuer(privateKey, cert.getPublicKey(), issuerName);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ucitava sertifikat is KS fajla
     */
    public Certificate readCertificate(String keyStoreFile, String keyStorePass, String alias) {
        try {
            // Create an instance of KeyStore
            KeyStore ks = KeyStore.getInstance("JKS", "SUN");

            // Load the keystore from the file
            try (InputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile))) {
                ks.load(in, keyStorePass.toCharArray());
            }

            // Check if the alias exists and return the certificate
            if (ks.containsAlias(alias)) {
                return ks.getCertificate(alias);
            }
        } catch (KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ucitava privatni kljuc is KS fajla
     *
     * Ova metoda se koristi za čitanje privatnog ključa iz keystore fajla na osnovu datog aliasa i lozinke.
     * Učitava keystore iz datoteke, provjerava da li postoji privatni ključ povezan s datim aliasom, a zatim ga vraća.
     */
    public PrivateKey readPrivateKey(String keyStoreFile, String keyStorePass, String alias, String pass) {
        try {
            //kreiramo instancu KeyStore
            KeyStore ks = KeyStore.getInstance("JKS", "SUN");
            //ucitavamo podatke

            Resource resource = resourceLoader.getResource(keyStoreFile);
            try (InputStream in = new BufferedInputStream(resource.getInputStream())) {
                ks.load(in, keyStorePass.toCharArray());
            }

            Enumeration<String> aliases = ks.aliases();

            if(ks.isKeyEntry(aliases.nextElement())) {
                PrivateKey pk = (PrivateKey) ks.getKey(alias, pass.toCharArray());
                return pk;
            }
        } catch (KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException |
                 IOException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
}
