package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteDetailDto;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteDetailDto.PartnerRouteDetailDtoBuilder;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteNewDto;
import ru.yandex.market.logistics.management.domain.dto.front.partnerRoute.PartnerRouteNewDto.PartnerRouteNewDtoBuilder;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

final class Helper {
    static final String METHOD_URL = "/admin/lms/partner-route";
    static final String READ_ONLY = LMSPlugin.AUTHORITY_ROLE_PARTNER_ROUTE;
    static final String READ_WRITE = LMSPlugin.AUTHORITY_ROLE_PARTNER_ROUTE_EDIT;
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Helper() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static MockHttpServletRequestBuilder getGrid() {
        return get(METHOD_URL);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getDetail(long partnerRouteId) {
        return get(METHOD_URL + "/{partnerRouteId}", partnerRouteId);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getNew() {
        return get(METHOD_URL + "/new");
    }

    @Nonnull
    static MockHttpServletRequestBuilder create() {
        return post(METHOD_URL).contentType(MediaType.APPLICATION_JSON);
    }

    @Nonnull
    static MockHttpServletRequestBuilder update(long partnerRouteId) {
        return put(METHOD_URL + "/{partnerRouteId}", partnerRouteId).contentType(MediaType.APPLICATION_JSON);
    }

    @Nonnull
    static MockHttpServletRequestBuilder delete(long partnerRouteId) {
        return MockMvcRequestBuilders.delete(METHOD_URL + "/{partnerRouteId}", partnerRouteId);
    }

    @Nonnull
    static MockHttpServletRequestBuilder deleteMultiple() {
        return post(METHOD_URL + "/delete").contentType(MediaType.APPLICATION_JSON);
    }

    @Nonnull
    static MockHttpServletRequestBuilder downloadTemplate() {
        return get(METHOD_URL + "/download/template");
    }

    @Nonnull
    static MockHttpServletRequestBuilder downloadAll() {
        return get(METHOD_URL + "/download/all");
    }

    @Nonnull
    static MockMultipartHttpServletRequestBuilder uploadUpsert() {
        return multipart(METHOD_URL + "/upload/upsert");
    }

    @Nonnull
    @SneakyThrows
    static byte[] toContent(Object object) {
        return OBJECT_MAPPER.writeValueAsBytes(object);
    }

    @Nonnull
    static PartnerRouteNewDtoBuilder<?, ?> partnerRouteNewDto() {
        return PartnerRouteNewDto.builder()
            .partner(3000L)
            .locationFrom(163)
            .locationTo(162);
    }

    @Nonnull
    static PartnerRouteDetailDtoBuilder<?, ?> partnerRouteDetailDto() {
        return PartnerRouteDetailDto.builder()
            .partner(new ReferenceObject("3000", "partner-3000", "partner"))
            .locationFrom(163)
            .locationTo(162);
    }

    @NotNull
    @SneakyThrows
    static MockMultipartFile file() {
        return file("partner\n1\n");
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
