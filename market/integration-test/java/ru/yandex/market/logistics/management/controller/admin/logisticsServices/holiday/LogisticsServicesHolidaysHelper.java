package ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday;

import java.util.Map;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.management.controller.admin.partnerHoliday.AdminPartnerHolidayFileControllerTest.multipartFile;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
class LogisticsServicesHolidaysHelper {
    static final String METHOD_URL = "/admin/lms/logistic-services/day-off";

    @Nonnull
    static MockHttpServletRequestBuilder create(long parentId) {
        return post(METHOD_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .params(toParams(buildComponentProperties(parentId)));
    }

    @Nonnull
    static MockHttpServletRequestBuilder delete(long holidayId, long parentId) {
        return MockMvcRequestBuilders.delete(METHOD_URL + "/{holidayId}", holidayId)
            .params(toParams(buildComponentProperties(parentId)));
    }

    @Nonnull
    static MockHttpServletRequestBuilder deleteMultiple(long parentId) {
        return post(METHOD_URL + "/delete")
            .params(toParams(buildComponentProperties(parentId)))
            .contentType(MediaType.APPLICATION_JSON);
    }

    @Nonnull
    static MockHttpServletRequestBuilder getTemplate() {
        return get(METHOD_URL + "/download/template");
    }

    @Nonnull
    static MockMultipartHttpServletRequestBuilder uploadFileForCreate(String jsonPath) {
        return multipart(METHOD_URL + "/upload/add")
            .file(multipartFile(pathToJson(jsonPath)));
    }

    @Nonnull
    static MockMultipartHttpServletRequestBuilder uploadFileForDelete(String jsonPath) {
        return multipart(METHOD_URL + "/upload/delete")
            .file(multipartFile(pathToJson(jsonPath)));
    }

    @Nonnull
    private static Map<String, String> buildComponentProperties(long parentId) {
        return Map.of(
            "parentSlug", "logistic-services",
            "parentId", String.valueOf(parentId),
            "idFieldName", "id"
        );
    }
}
