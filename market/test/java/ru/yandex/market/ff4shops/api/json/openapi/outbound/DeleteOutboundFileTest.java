package ru.yandex.market.ff4shops.api.json.openapi.outbound;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.openapi.AbstractOpenApiTest;

import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Удаление файла отправки")
@DbUnitDataSet(before = "deleteOutboundFile.before.csv")
public class DeleteOutboundFileTest extends AbstractOpenApiTest {

    @Autowired
    private TestableClock clock;

    @Test
    @DisplayName("Файл не найден")
    void fileNotFound() {
        assertResponseBody(
            deleteOutboundFile(1, 2, HttpStatus.NOT_FOUND.value()),
            "ru/yandex/market/ff4shops/api/json/openapi/outbound/deleteOutboundFile.fileNotFound.json"
        );
    }

    @Test
    @DisplayName("Файл принадлежит другой отправке")
    void wrongFileOutbound() {
        assertResponseBody(
            deleteOutboundFile(2, 10, HttpStatus.NOT_FOUND.value()),
            "ru/yandex/market/ff4shops/api/json/openapi/outbound/deleteOutboundFile.wrongFileOutbound.json"
        );
    }

    @Test
    @DisplayName("Успех")
    @DbUnitDataSet(after = "deleteOutboundFile.after.csv")
    void success() {
        clock.setFixed(Instant.parse("2021-08-01T10:11:12Z"), ZoneId.systemDefault());

        deleteOutboundFile(1, 10, HttpStatus.OK.value());
    }

    @Nonnull
    private String deleteOutboundFile(long outboundId, long fileId, int expectedHttpCode) {
        return apiClient.outboundFiles().deleteOutboundFile()
            .fileIdPath(fileId)
            .outboundYandexIdPath(outboundId)
            .execute(validatedWith(shouldBeCode(expectedHttpCode)))
            .jsonPath()
            .prettify();
    }

}
