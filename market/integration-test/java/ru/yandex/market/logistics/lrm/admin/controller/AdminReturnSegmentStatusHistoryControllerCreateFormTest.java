package ru.yandex.market.logistics.lrm.admin.controller;

import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.admin.LrmPlugin;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@ParametersAreNonnullByDefault
@DisplayName("Форма создания статуса возвратного сегмента")
@DatabaseSetup("/database/admin/create-segment-status-form/before/prepare.xml")
class AdminReturnSegmentStatusHistoryControllerCreateFormTest extends AbstractIntegrationTest {
    private static final LocalDateTime FIXED_LOCAL_DATE_TIME = LocalDateTime.of(2021, 11, 11, 11, 12, 13);
    private static final String GET_CREATE_FORM_SLUG =
        "/admin/" + LrmPlugin.SLUG_RETURN_SEGMENTS_STATUS_HISTORY + "/new?parentId=";

    @BeforeEach
    void setUp() {
        clock.setFixed(FIXED_LOCAL_DATE_TIME.atZone(DateTimeUtils.MOSCOW_ZONE).toInstant(), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Получение формы создания статуса")
    void getCreationForm() {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.get(GET_CREATE_FORM_SLUG + "1"),
            "json/admin/create-segment-status/response/creation_form.json"
        );
    }

    @Test
    @DisplayName("Получение формы создания статуса для несуществующего сегмента")
    void getCreationFormForNonExistingSegment() {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.get(GET_CREATE_FORM_SLUG + "1234567"),
            "Failed to find RETURN_SEGMENT with ids [1234567]"
        );
    }
}
