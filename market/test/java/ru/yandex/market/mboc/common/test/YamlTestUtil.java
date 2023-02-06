package ru.yandex.market.mboc.common.test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.modelform.CachedModelForm;
import ru.yandex.market.mboc.common.notifications.model.Notification;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledge;

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

    public static List<Offer> readOffersFromResources(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<Offer>>() {
        });
    }

    public static List<Offer> readOffersWithServiceFromResources(String resourceName,
                                                                 Function<Integer, Supplier> supplierProvider) {
        List<Offer> offers = readFromResources(resourceName, new TypeReference<>() {
        });
        offers.forEach(o -> {
            Supplier supplier = supplierProvider.apply(o.getBusinessId());
            o.addNewServiceOfferIfNotExistsForTests(supplier);
            o.updateAcceptanceStatusForTests(supplier.getId(), o.getAcceptanceStatus());
        });
        return offers;
    }

    public static List<Category> readCategoriesFromResources(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<Category>>() {
        });
    }

    public static List<Notification> readNotificationsFromResources(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<Notification>>() {
        });
    }

    public static List<Supplier> readSuppliersFromResource(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<Supplier>>() {
        });
    }
    public static List<CategoryKnowledge> readKnowledge(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<CategoryKnowledge>>() {
        });
    }

    public static List<CachedModelForm> readModelForms(String resourceName) {
        return readFromResources(resourceName, new TypeReference<List<CachedModelForm>>() {
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
