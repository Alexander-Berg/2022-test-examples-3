package ru.yandex.direct.intapi.entity.display.canvas;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeUploadData;

public class CreativeLoader {
    private CreativeLoader() {
    }

    static Collection<CreativeUploadData> loadCreatives(String filename) throws IOException {
        ObjectMapper m = new ObjectMapper();
        TypeReference<List<CreativeUploadData>> typeRef = new TypeReference<List<CreativeUploadData>>() {
        };
        try (InputStream is = CreativeLoader.class.getResourceAsStream(filename)) {
            return m.readValue(is, typeRef);
        }
    }
}
