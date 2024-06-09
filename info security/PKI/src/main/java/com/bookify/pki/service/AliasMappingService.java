package com.bookify.pki.service;

import com.bookify.pki.model.CertificateAssociation;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AliasMappingService {

    @Autowired
    ResourceLoader resourceLoader;

    private String ALIAS_MAP_FILENAME = "src/main/resources/certificates/alias_mapping.json";

    public String getCertificateAlias(Long certificateId){

        Map<Long, CertificateAssociation> certificates = convertJsonFileToHashMap(ALIAS_MAP_FILENAME);
        if(certificates == null) return null;
        return certificates.get(certificateId).getAlias();

    }

    public List<Long> getSignedCertificateIds(Long issuerCertificateId){

        Map<Long, CertificateAssociation> certificates = convertJsonFileToHashMap(ALIAS_MAP_FILENAME);
        if(certificates == null) return null;
        return certificates.get(issuerCertificateId).getSignedCertificates();

    }

    public Long getCertificatesIssuerId(Long certificateId){
        Map<Long, CertificateAssociation> certificates = convertJsonFileToHashMap(ALIAS_MAP_FILENAME);
        if(certificates == null) return null;
        return certificates.get(certificateId).getIssuerId();
    }

    public void deleteCertificateFromFile(Long certificateId){
        HashMap<Long, CertificateAssociation> certificates = convertJsonFileToHashMap(ALIAS_MAP_FILENAME);
        if(certificates == null) return;
        certificates.remove(certificateId);
        serializeHashMapToJsonFile(certificates, ALIAS_MAP_FILENAME);
    }

    public void deleteFromParentList(Long certificateId){
        HashMap<Long, CertificateAssociation> certificates = convertJsonFileToHashMap(ALIAS_MAP_FILENAME);
        if(certificates == null) return;
        Long issuerId = certificates.get(certificateId).getIssuerId();
        certificates.get(issuerId).getSignedCertificates().remove(certificateId);
        serializeHashMapToJsonFile(certificates, ALIAS_MAP_FILENAME);
    }

    public void addSignedCertificate(Long issuerId,Long subjectId,String alias){

        HashMap<Long, CertificateAssociation> certificates = convertJsonFileToHashMap(ALIAS_MAP_FILENAME);
        if(certificates == null) return;

        certificates.put(subjectId,new CertificateAssociation(issuerId,alias,new ArrayList<>()));

        CertificateAssociation temp=certificates.get(issuerId);
        temp.getSignedCertificates().add(subjectId);

        serializeHashMapToJsonFile(certificates,"src/main/resources/certificates/alias_mapping.json");

    }

    public static boolean serializeHashMapToJsonFile(HashMap<Long, CertificateAssociation> hashMap, String jsonFilePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Convert HashMap to JSON string
            // Write JSON string to file
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File(jsonFilePath),hashMap);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public HashMap<Long, CertificateAssociation> convertJsonFileToHashMap(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File(filename);
            if (file.exists()) {
                return mapper.readValue(file, new TypeReference<HashMap<Long, CertificateAssociation>>() {});
            } else {
                System.out.println("File not found: " + filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

}
