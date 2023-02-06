package ru.yandex.market.wms.api.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JsonToObject {
    public <T> T loadJsonToObject(String path, Class<T> generic) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        return mapper.readValue(inputStream, generic);
    }
}
