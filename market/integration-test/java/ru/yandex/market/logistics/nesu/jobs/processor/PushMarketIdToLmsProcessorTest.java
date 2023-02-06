package ru.yandex.market.logistics.nesu.jobs.processor;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.marketId.MarketIdDto;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPartnerIdPayload;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Пуш информации о marketId в LMS")
@DatabaseSetup("/jobs/processor/push_market_id/before.xml")
public class PushMarketIdToLmsProcessorTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private PushMarketIdToLmsProcessor processor;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @ExpectedDatabase(
        value = "/jobs/processor/push_market_id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        processor.processPayload(new ShopIdPartnerIdPayload("1", 2L, 1L));
        verify(lmsClient).setBusinessWarehouseMarketId(2L, MarketIdDto.of(400L));
    }
}
