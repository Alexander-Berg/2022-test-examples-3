package ru.yandex.market.replenishment.autoorder.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

/**
 * Базовый класс для всех контроллеров. На нем не должно быть аннотаций {@link WithMockLogin}
 * или {@link ru.yandex.market.replenishment.autoorder.security.WithMockTvm}.
 * Так как аннотации конфликтуют друг с другом и разработчик должен выбирать какую аннотацию использовать.
 */
@AutoConfigureMockMvc
public abstract class ControllerTest extends FunctionalTest {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @SneakyThrows
    public String readFile(String fileName) {
        InputStream inputStream = this.getClass().getResourceAsStream(fileName);
        return IOUtils.toString(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8.name());
    }

    protected <T> T readJson(String content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content, cls);
    }

    protected <T> T readJson(MvcResult content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content.getResponse().getContentAsString(), cls);
    }

    protected <T> List<T> readJsonList(String content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content, new TypeReference<List<T>>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{cls};
                    }

                    @Override
                    public Type getRawType() {
                        return List.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                };
            }
        });
    }

    protected <T> List<T> readJsonList(MvcResult content, Class<T> cls) throws IOException {
        return readJsonList(content.getResponse().getContentAsString(), cls);
    }

    protected <T> T readJson(String content, TypeReference<T> cls) throws IOException {
        return objectMapper.readValue(content, cls);
    }

    protected <T> T readJson(MvcResult result, TypeReference<T> cls) throws IOException {
        return readJson(result.getResponse().getContentAsString(), cls);
    }
}
