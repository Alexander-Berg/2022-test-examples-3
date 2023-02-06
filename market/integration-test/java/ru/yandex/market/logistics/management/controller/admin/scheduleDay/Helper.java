package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.domain.dto.filter.DeliveryIntervalScheduleDayFilter;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

public final class Helper {
    static final String METHOD_URL = "/admin/lms/delivery-interval-schedule-day";
    static final String READ_ONLY = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SCHEDULE_DAY;
    static final String READ_WRITE = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SCHEDULE_DAY_EDIT;

    private Helper() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static MockHttpServletRequestBuilder getGrid() {
        return get(METHOD_URL);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getDetail(long scheduleDayId) {
        return get(METHOD_URL + "/{scheduleDayId}", scheduleDayId);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getIds(DeliveryIntervalScheduleDayFilter filter, Pageable pageable) {
        return get(METHOD_URL, filter, pageable);
    }

    @Nonnull
    static MockHttpServletRequestBuilder downloadTemplate() {
        return get(METHOD_URL + "/download/template");
    }

    @Nonnull
    static MockMultipartHttpServletRequestBuilder uploadAdd() {
        return multipart(METHOD_URL + "/upload/add");
    }

    @Nonnull
    static MockMultipartHttpServletRequestBuilder uploadReplace() {
        return multipart(METHOD_URL + "/upload/replace");
    }

    @NotNull
    @SneakyThrows
    public static MockMultipartFile multipartFile(String content) {
        return new MockMultipartFile(
            "request",
            "originalFileName.csv",
            MediaTypes.TEXT_CSV_UTF8_VALUE,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
    }

}
