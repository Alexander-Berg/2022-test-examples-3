package ru.yandex.market.checkout.checkouter.antifraud.buyer;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.RefundPolicy;
import ru.yandex.market.checkout.checkouter.degradation.AbstractDegradationTest;
import ru.yandex.market.checkout.checkouter.degradation.strategy.antifraud.AntifraudBuyerInfoDegradationStrategy;
import ru.yandex.market.checkout.checkouter.order.Buyer;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {
        AntifraudBuyerInfoDegradationStrategy.class,
        AntifraudUserServiceImpl.class
})
class AntifraudUserServiceImplTest extends AbstractDegradationTest {

    @Autowired
    private RestTemplate antifraudRestTemplate;
    @Autowired
    private AntifraudUserService antifraudUserService;

    @Test
    void falseForInvalidArguments() {
        assertThat(antifraudUserService.isBuyerTrusted(null)).isFalse();
        assertThat(antifraudUserService.isBuyerTrusted(new Buyer())).isFalse();
    }

    @Test
    void trueForTrustedUser() {
        Buyer buyer = initBuyer();
        mockAntifraudResponse(buyer, RefundPolicy.SIMPLE);

        assertThat(antifraudUserService.isBuyerTrusted(buyer)).isTrue();
    }

    @Test
    void falseForUntrustedUser() {
        Buyer buyer = initBuyer();
        mockAntifraudResponse(buyer, RefundPolicy.FULL);

        assertThat(antifraudUserService.isBuyerTrusted(buyer)).isFalse();
    }

    @Test
    void trueForUnknownUser() {
        Buyer buyer = initBuyer();
        mockAntifraudResponse(buyer, RefundPolicy.UNKNOWN);

        assertThat(antifraudUserService.isBuyerTrusted(buyer)).isTrue();
    }

    @Test
    void trueIfAntifraudRequestFinishedWithException() {
        Buyer buyer = initBuyer();
        Mockito.when(antifraudRestTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(BuyerInfoDto.class)))
                .thenThrow(new RuntimeException("some excetion"));

        assertThat(antifraudUserService.isBuyerTrusted(buyer)).isTrue();
    }

    private Buyer initBuyer() {
        Buyer buyer = new Buyer();
        buyer.setUid(1L);
        return buyer;
    }

    private void mockAntifraudResponse(Buyer buyer, RefundPolicy refundPolicy) {
        BuyerInfoDto buyerInfoDto = new BuyerInfoDto(buyer.getUid(), "", "", false, null, null, refundPolicy,
                Collections.emptyList(), Collections.emptySet());
        Mockito.when(antifraudRestTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(BuyerInfoDto.class)))
                .thenReturn(buyerInfoDto);
    }


}
