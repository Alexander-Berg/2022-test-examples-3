package ru.yandex.market.ff4shops.api.json.outbound;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@ParametersAreNonnullByDefault
@DisplayName("Получение статусов нескольких отправок")
public class GetOutboundStatusTest extends AbstractOutboundsTest {

    @Test
    @DbUnitDataSet(
        before = "getOutbounds.before.csv",
        after = "getOutbounds.before.csv"
    )
    @DisplayName("Все отправки существуют в БД")
    void getOutboundStatus() {
        makeCallAndCheckResponse(
            "ru/yandex/market/ff4shops/api/json/outbound/getOutboundStatusResponse.json"
        );
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Отправки отсутствуют в БД")
    void getOutboundStatusEmptyResponse() {
        makeCallAndCheckResponse(
            "ru/yandex/market/ff4shops/api/json/outbound/getOutboundStatusResponseEmpty.json"
        );
    }

    @Override
    @Nonnull
    public String getUrl() {
        return urlBuilder.url("partner", "outbounds", "status");
    }
}
