package ru.yandex.market.logistics.lrm.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;

@DisplayName("Получение запросов на изменение сегмента")
@DatabaseSetup("/database/admin/get-return-segment-changes/before/prepare.xml")
class GetReturnSegmentChangesTest extends AbstractIntegrationTest {

    private static final String PATH = "/admin/returns/boxes/segments/changes";

    @Test
    @DisplayName("Сегмент не найден")
    void segmentNotFound() {
        RestAssuredTestUtils.assertNotFoundError(
            getChanges(2),
            "Failed to find RETURN_SEGMENT with ids [2]"
        );
    }

    @Test
    @DisplayName("Успех")
    void success() {
        RestAssuredTestUtils.assertJsonResponse(
            getChanges(1),
            "json/admin/get-return-segment-changes/success.json"
        );
    }

    @Nonnull
    private Response getChanges(long segmentId) {
        return RestAssured.given().param("segmentId", segmentId).get(PATH);
    }

}
