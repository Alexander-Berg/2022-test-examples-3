package ru.yandex.market.deepmind.common.utils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;

public class YamlTestUtil {
    private YamlTestUtil() {
    }

    public static <T> T readFromResources(String resourceName, Class<T> cls) {
        ObjectMapper mapper = createObjectMapper();
        try {
            return mapper.readValue(YamlTestUtil.class.getClassLoader().getResource(resourceName), cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFromResources(String resourceName, TypeReference<T> typeReference) {
        ObjectMapper mapper = createObjectMapper();
        try {
            URL resource = YamlTestUtil.class.getClassLoader().getResource(resourceName);
            if (resource == null) {
                throw new IllegalStateException("Failed to find resource by file: " + resourceName);
            }
            return mapper.readValue(resource, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ServiceOfferReplica> readOffersFromResources(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<ServiceOfferReplica>>() {
        });
    }

    public static List<Category> readCategoriesFromResources(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<Category>>() {
        });
    }

    public static List<Supplier> readSuppliersFromResource(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<Supplier>>() {
        });
    }

    public static String readAsString(String resourceName) {
        try (var stream = YamlTestUtil.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new RuntimeException("failed to formulat stream");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
