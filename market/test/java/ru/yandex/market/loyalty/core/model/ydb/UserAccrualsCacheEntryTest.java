package ru.yandex.market.loyalty.core.model.ydb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserAccrualsCacheEntryTest extends MarketLoyaltyCoreMockedDbTestBase {

    private final static TypeReference<List<UserAccrualsCacheEntry.AccrualStatusesWithAmount>> typeReference =
            new TypeReference<>() {
    };
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldConvertAccrualsFromObjectToJsonAndBack() throws JsonProcessingException {
        List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> accruals = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            accruals.add(new UserAccrualsCacheEntry.AccrualStatusesWithAmount(
                            YandexWalletTransactionStatus.CONFIRMED,
                            YandexWalletRefundTransactionStatus.NOT_QUEUED,
                            BigDecimal.valueOf(i + 100),
                            i,
                            Timestamp.from(clock.instant())
                    )
            );
        }
        String accrualsJson = objectMapper.writeValueAsString(accruals);
        List<UserAccrualsCacheEntry.AccrualStatusesWithAmount> accrualStatusesWithAmounts =
                objectMapper.readValue(accrualsJson, typeReference);
        Assert.assertEquals(5, accrualStatusesWithAmounts.size());
    }
}
