package ru.yandex.market.ocrm.module.loyalty.test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.loyalty.api.model.perk.PerkStat;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.ocrm.module.loyalty.MarketLoyaltyService;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;
import ru.yandex.market.ocrm.module.loyalty.impl.PerkStatus;
import ru.yandex.market.ocrm.module.loyalty.impl.YandexPlusCustomerMarkerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = ModuleLoyaltyTestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class YandexPlusCustomerMarkerProviderTest {

    @Inject
    private GidService gidService;
    private MarketLoyaltyService marketLoyaltyService;

    private YandexPlusCustomerMarkerProvider provider;

    @BeforeEach
    public void setUp() {
        marketLoyaltyService = mock(MarketLoyaltyService.class);
        provider = new YandexPlusCustomerMarkerProvider(marketLoyaltyService, gidService);
    }

    @Test
    public void getMarkers_withNullUid_shouldReturnEmpty() {
        List<String> markers = provider.getMarkers(() -> "customer@123", null);
        assertThat(markers, hasSize(0));
    }

    /**
     * https://testpalm2.yandex-team.ru/ocrm/testsuite/5f61a1ba8807112718f2756d?testcase=1364 ставит маркер если
     * запрошен для customer (для order нужна отдельная логика, сейчас ее нет) и у клиента куплен я+
     */
    @ParameterizedTest
    @CsvSource({
            "customer@123, true, true",
            "customer@123, false, false",
            "order@123, true, false"
    })
    public void getMarkers_shouldReturnYandexPlusIfPurchased(String gid, boolean isPurchased, boolean markerExpected) {
        HasGid entity = () -> gid;
        var perks = List.of(PerkType.YANDEX_PLUS);

        var perkStatus = mock(PerkStatus.class);
        var perk = PerkStat.builder()
                .setType(PerkType.YANDEX_PLUS)
                .setPurchased(isPurchased)
                .setEmitAllowed(false)
                .setSpendAllowed(false)
                .setFreeDelivery(false)
                .setOrderId("")
                .setBalance(BigDecimal.ZERO)
                .setMaxPromoDiscountPercent(BigDecimal.ZERO)
                .setThreshold(BigDecimal.ZERO)
                .setCashback(BigDecimal.ZERO)
                .build();
        when(perkStatus.get(PerkType.YANDEX_PLUS)).thenReturn(Optional.of(perk));
        when(marketLoyaltyService.getPerkStatus(perks, 1L, 213)).thenReturn(perkStatus);

        List<String> markers = provider.getMarkers(entity, 1L);

        if (markerExpected) {
            assertThat(markers, hasSize(1));
            assertThat(markers.get(0), equalTo("yandexPlus"));
        } else {
            assertThat(markers, hasSize(0));
        }
    }
}
