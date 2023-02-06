package ru.yandex.market.logistics.management.controller.admin.partnerTransport;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

public class Helper {

    static final String METHOD_URL = "/admin/lms/partner-transport";
    static final String READ_ONLY = LMSPlugin.AUTHORITY_ROLE_PARTNER_TRANSPORT;
    static final String READ_WRITE = LMSPlugin.AUTHORITY_ROLE_PARTNER_TRANSPORT_EDIT;
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Helper() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static MockHttpServletRequestBuilder getDetail(long transportId) {
        return get(METHOD_URL + "/{transportId}", transportId);
    }

    @Nonnull
    static MockHttpServletRequestBuilder downloadTemplate() {
        return get(METHOD_URL + "/download/template");
    }

    @Nonnull
    static MockHttpServletRequestBuilder download() {
        return get(METHOD_URL + "/download/all");
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
    static MockMultipartFile file(String content) {
        return new MockMultipartFile(
            "request",
            "originalFileName.csv",
            MediaTypes.TEXT_CSV_UTF8_VALUE,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
    }
}
