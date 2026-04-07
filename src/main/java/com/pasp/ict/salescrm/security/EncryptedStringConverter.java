package com.pasp.ict.salescrm.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter for automatic encryption/decryption of sensitive string fields.
 * Automatically encrypts data when saving to database and decrypts when loading.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        EncryptedStringConverter.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (encryptionService == null) {
            // Fallback for cases where Spring context is not available
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (encryptionService == null) {
            // Fallback for cases where Spring context is not available
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}