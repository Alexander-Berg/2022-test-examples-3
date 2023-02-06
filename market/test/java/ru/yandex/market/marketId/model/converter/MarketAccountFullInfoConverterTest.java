package ru.yandex.market.marketId.model.converter;


import java.time.Instant;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import ru.yandex.market.id.LegalInfoType;
import ru.yandex.market.marketId.model.bo.MarketAccountFullInfo;
import ru.yandex.market.marketId.model.dto.MarketAccountFullInfoDTO;
import ru.yandex.market.marketId.model.entity.MarketAccountEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketAccountFullInfoConverterTest {
    private MarketAccountFullInfoConverter marketAccountFullInfoConverter =
            new MarketAccountFullInfoConverter();

    @Test
    void testConvert() {
        MarketAccountFullInfo marketAccountFullInfo = new MarketAccountFullInfo(getTestMarketAccount(), new ArrayList<>());
        MarketAccountFullInfoDTO dto = marketAccountFullInfoConverter.convert(marketAccountFullInfo);
        assertEquals(dto.getLegalInfoMap().size(), LegalInfoType.values().length-1);
    }


    private MarketAccountEntity getTestMarketAccount() {
        MarketAccountEntity marketAccount = new MarketAccountEntity();
        marketAccount.setMarketId(1);
        marketAccount.setCreationTs(Instant.now());
        return marketAccount;
    }

}
