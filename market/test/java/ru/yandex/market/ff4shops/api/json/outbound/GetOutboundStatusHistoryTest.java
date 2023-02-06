package ru.yandex.market.ff4shops.api.json.outbound;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Получение истории статусов отправки")
public class GetOutboundStatusHistoryTest extends AbstractOutboundsTest {
    @Test
    @DbUnitDataSet(
        before = "getOutbounds.before.csv",
        after = "getOutbounds.before.csv"
    )
    @DisplayName("Все отправки существуют в БД")
    void getOutboundStatusHistory() {
        makeCallAndCheckResponse(
            "ru/yandex/market/ff4shops/api/json/outbound/getOutboundStatusHistoryResponse.json"
        );
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Некоторые отправки отсутствуют в БД")
    void getOutboundStatusHistoryEmptyResponse() {
        makeCallAndCheckResponse(
            "ru/yandex/market/ff4shops/api/json/outbound/getOutboundStatusResponseEmpty.json"
        );
    }

    @Override
    @Nonnull
    public String getUrl() {
        return urlBuilder.url("partner", "outbounds", "statusHistory");
    }
}
