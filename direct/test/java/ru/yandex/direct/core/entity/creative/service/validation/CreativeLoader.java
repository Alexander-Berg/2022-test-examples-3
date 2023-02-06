package ru.yandex.direct.core.entity.creative.service.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.direct.core.entity.creative.model.Creative;

public class CreativeLoader {
    private CreativeLoader() {
    }

    public static Collection<Creative> loadCreatives(String filename) throws IOException {
        ObjectMapper m = new ObjectMapper();
        TypeReference<List<Creative>> typeRef = new TypeReference<List<Creative>>() {
        };
        try (InputStream is = CreativeLoader.class.getResourceAsStream(filename)) {
            return m.readValue(is, typeRef);
        }
    }
}
