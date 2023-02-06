package ru.yandex.market.logistics.tarifficator.jobs.processor;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.model.TariffDestinationPartnerPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Интеграционный тест TariffDestinationPartnerBinderService")
class TariffDestinationPartnerBinderServiceTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private TariffDestinationPartnerBinderService tariffDestinationPartnerBinderService;

    @Test
    @DisplayName("Генерация нового поколения c MARKET_DELIVERY тарифами")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup(
        value = "/tms/revision/before/generate-courier-tariff.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-market-delivery-courier-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateMarketDeliverySuccess() {
        mockGetPartner();
        tariffDestinationPartnerBinderService.processPayload(createPayload(1L));
    }

    @Nonnull
    private TariffDestinationPartnerPayload createPayload(Long tariffId) {
        return new TariffDestinationPartnerPayload("1", tariffId);
    }

    private void mockGetPartner() {
        when(lmsClient.searchPartners(any()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).name("partner_" + 1L).build(),
                PartnerResponse.newBuilder().id(2L).name("partner_" + 2L).build(),
                PartnerResponse.newBuilder().id(3L).name("partner_" + 3L).build(),
                PartnerResponse.newBuilder().id(4L).name("partner_" + 4L).build()
        ));
    }
}
