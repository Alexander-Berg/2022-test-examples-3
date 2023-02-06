package ru.yandex.market.ff4shops.api.json.outbound;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.model.SearchOutboundsFilter;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Получение данных отправок")
public class SearchOutboundsTest extends AbstractOutboundsTest {

    @Test
    @DbUnitDataSet(
            before = "getOutbounds.before.csv",
            after = "getOutbounds.before.csv"
    )
    @DisplayName("Отправок не существует")
    void testGetOutboundsNotExist() throws JsonProcessingException {
        ResponseEntity<String> response = getOutbounds(List.of("100"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertResponseBody(
                response.getBody(),
                "ru/yandex/market/ff4shops/api/json/outbound/getOutboundsEmpty.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "getOutbounds.before.csv",
            after = "getOutbounds.before.csv"
    )
    @DisplayName("Отправки существуют в БД: 3-й отправки нет в БД")
    void testGetOutboundsAllExists() throws JsonProcessingException {
        ResponseEntity<String> response = getOutbounds(List.of("1", "2", "3"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertResponseBody(
                response.getBody(),
                "ru/yandex/market/ff4shops/api/json/outbound/getOutbounds.json"
        );
    }

    @Test
    @DbUnitDataSet(before = {
            "getOutbounds.before.csv",
            "getOutboundsFiles.before.csv",
    })
    @DisplayName("Файлы отправок")
    void outboundFiles() throws JsonProcessingException {
        ResponseEntity<String> response = getOutbounds(List.of("2", "3"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertResponseBody(
                response.getBody(),
                "ru/yandex/market/ff4shops/api/json/outbound/getOutboundsFiles.json"
        );
    }

    @Override
    @Nonnull
    public String getUrl() {
        return urlBuilder.url("partner", "outbounds", "search");
    }

    @Nonnull
    private ResponseEntity<String> getOutbounds(List<String> yandexIds) throws JsonProcessingException {
        return FunctionalTestHelper.putForEntity(
            getUrl(),
            MAPPER.writeValueAsString(new SearchOutboundsFilter().setOutboundYandexIds(yandexIds)),
            FunctionalTestHelper.jsonHeaders()
        );
    }
}
