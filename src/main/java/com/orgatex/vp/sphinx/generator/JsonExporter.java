package com.orgatex.vp.sphinx.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orgatex.vp.sphinx.model.NeedsFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;

/**
 * Exports diagram data to sphinx-needs compatible JSON format.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonExporter {

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public static void exportToFile(NeedsFile needsFile, File outputFile) throws IOException {
        if (needsFile == null) {
            throw new IllegalArgumentException("NeedsFile cannot be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        // Ensure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        objectMapper.writeValue(outputFile, needsFile);
    }

    public static String exportToString(NeedsFile needsFile) throws IOException {
        if (needsFile == null) {
            throw new IllegalArgumentException("NeedsFile cannot be null");
        }
        return objectMapper.writeValueAsString(needsFile);
    }
}
