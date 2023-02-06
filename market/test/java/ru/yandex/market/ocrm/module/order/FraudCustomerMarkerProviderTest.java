package ru.yandex.market.ocrm.module.order;

import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.antifraud.orders.entity.UserMarkerDto;
import ru.yandex.market.antifraud.orders.entity.UserMarkerType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlacklistType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.RefundPolicy;
import ru.yandex.market.ocrm.module.order.impl.BuyerInfoCache;
import ru.yandex.market.ocrm.module.order.impl.FraudCustomerMarkerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class FraudCustomerMarkerProviderTest {

    private BuyerInfoCache buyerInfoCache;
    private FraudCustomerMarkerProvider provider;

    private static List<Arguments> buyerInfoCases() {
        return List.of(
                Arguments.of(
                        getBuyerInfo("", false, BlacklistType.NOT_BLACKLISTED, RefundPolicy.FULL),
                        List.of("refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("", false, BlacklistType.NOT_BLACKLISTED, RefundPolicy.SIMPLE),
                        List.of("refundPolicy_SIMPLE", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("", false, BlacklistType.NOT_BLACKLISTED, RefundPolicy.UNKNOWN),
                        List.of("refundPolicy_UNKNOWN", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("", false, BlacklistType.DIRECT, RefundPolicy.FULL),
                        List.of("buyerFraud", "refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("", false, BlacklistType.GLUE, RefundPolicy.FULL),
                        List.of("buyerFraud", "refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("", false, BlacklistType.UNKNOWN, RefundPolicy.FULL),
                        List.of("buyerFraud", "refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("", true, BlacklistType.NOT_BLACKLISTED, RefundPolicy.FULL),
                        List.of("VIP", "refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("whitelist", false, BlacklistType.NOT_BLACKLISTED, RefundPolicy.FULL),
                        List.of("buyerRole_whitelist", "refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("yandexoid", false, BlacklistType.NOT_BLACKLISTED, RefundPolicy.FULL),
                        List.of("buyerRole_yandexoid", "refundPolicy_FULL", "fraud_test")),
                Arguments.of(
                        getBuyerInfo("whitelist", true, BlacklistType.DIRECT, RefundPolicy.SIMPLE),
                        List.of("buyerRole_whitelist", "VIP", "buyerFraud", "refundPolicy_SIMPLE", "fraud_test"))
        );
    }

    private static BuyerInfoDto getBuyerInfo(String roleName, boolean isVip, BlacklistType blacklistType,
                                             RefundPolicy refundPolicy) {
        return new BuyerInfoDto(1L, roleName, "", isVip, blacklistType != BlacklistType.NOT_BLACKLISTED,
                blacklistType, refundPolicy, List.of(), Set.of(
                        UserMarkerDto.builder()
                                .name("test")
                                .showName("show_test")
                                .description("description_test")
                                .type(UserMarkerType.BAD)
                                .build()
        ));
    }

    @BeforeEach
    public void setUp() {
        buyerInfoCache = mock(BuyerInfoCache.class);
        provider = new FraudCustomerMarkerProvider(buyerInfoCache);
    }

    /**
     * https://testpalm2.yandex-team.ru/ocrm/testsuite/5f61a1ba8807112718f2756d?testcase=1364 маркеры клиента в
     * зависимости от ответа антифрода
     */
    @ParameterizedTest
    @MethodSource("buyerInfoCases")
    public void getMarkers_byBuyerInfo_shouldReturnExpectedMarkers(BuyerInfoDto buyerInfo, List<String> expected) {
        when(buyerInfoCache.getBuyer(buyerInfo.getUid())).thenReturn(buyerInfo);

        List<String> markers = provider.getMarkers(() -> "customer@123", buyerInfo.getUid());

        var expectedMarkers = expected.toArray(String[]::new);
        assertThat(markers, hasSize(expectedMarkers.length));
        assertThat(markers, Matchers.hasItems(expectedMarkers));
    }
}
