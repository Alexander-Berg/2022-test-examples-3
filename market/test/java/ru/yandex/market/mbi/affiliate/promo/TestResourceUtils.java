package ru.yandex.market.mbi.affiliate.promo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.util.JsonFormat;

import ru.yandex.market.mbi.affiliate.promo.service.AffiliatePromoServiceTest;

public class TestResourceUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public static <T> T loadDto(String fileName, Class<T> clazz) throws IOException {
        try (InputStream stream =
                     TestResourceUtils.class
                             .getClassLoader()
                             .getResourceAsStream(fileName)) {
            return MAPPER.readerFor(clazz).readValue(stream);
        }
    }

    public static <T extends com.google.protobuf.GeneratedMessageV3.Builder<T>> void loadProto(String fileName, T builder) throws IOException {
        try (InputStream stream =
                AffiliatePromoServiceTest.class
                        .getClassLoader()
                        .getResourceAsStream(fileName)) {
            JsonFormat.parser().merge(new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8), builder);
        }
    }
}
