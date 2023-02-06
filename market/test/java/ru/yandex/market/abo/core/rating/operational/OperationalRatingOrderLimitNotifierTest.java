package ru.yandex.market.abo.core.rating.operational;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.message.Flag;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.abo.core.rating.partner.order_limit.PartnerRatingLimitRange;
import ru.yandex.market.abo.core.rating.partner.order_limit.notification.DropshipsMessageTemplateResolver;
import ru.yandex.market.abo.core.rating.partner.order_limit.notification.RatingMessageTemplateResolverFactory;
import ru.yandex.market.abo.core.shop.ShopFlagService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit;
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 14.07.2020
 */
class OperationalRatingOrderLimitNotifierTest {

    private static final long SHOP_ID = 123L;
    private static final String SHOP_NAME = "Ромашка";
    private static final int ORDER_LIMIT = 100;

    private static final CpaOrderLimit CREATED_LIMIT = new CpaOrderLimit(
            SHOP_ID, PartnerModel.DSBB, CpaOrderLimitReason.OPERATIONAL_RATING,
            ORDER_LIMIT, DateUtil.asDate(LocalDateTime.now().plusYears(1)), null
    );

    @InjectMocks
    private OperationalRatingOrderLimitNotifier operationalRatingOrderLimitNotifier;

    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ShopFlagService shopFlagService;
    @Mock
    private RatingMessageTemplateResolverFactory ratingMessageTemplateResolverFactory;
    @Mock
    private PartnerInfoDTO partnerInfo;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(partnerInfo.getId()).thenReturn(SHOP_ID);
        when(partnerInfo.getName()).thenReturn(SHOP_NAME);

        when(mbiApiService.getPartnerInfo(SHOP_ID)).thenReturn(partnerInfo);
        when(mbiApiService.sendMessageToSupplier(eq(SHOP_ID), anyInt(), anyString())).thenReturn(true);
        when(ratingMessageTemplateResolverFactory.resolver(any())).thenReturn(new DropshipsMessageTemplateResolver());
    }

    @Test
    void doNotSendNotificationForUnknownShop() {
        when(mbiApiService.getPartnerInfo(SHOP_ID)).thenReturn(null);

        operationalRatingOrderLimitNotifier.notify(SHOP_ID, PartnerModel.DSBB, CREATED_LIMIT, noLimitRange(), ordersLimitRange());

        verify(mbiApiService, never()).sendMessageToSupplier(eq(SHOP_ID), anyInt(), anyString());
        verifyNoMoreInteractions(shopFlagService);
    }

    @Test
    void doNotSendNotificationForAlreadySentTemplate() {
        when(shopFlagService.shopFlagExists(eq(Flag.RATING_MESSAGE_SENT), eq(SHOP_ID), any(Date.class))).thenReturn(true);

        operationalRatingOrderLimitNotifier.notify(SHOP_ID, PartnerModel.DSBB, CREATED_LIMIT, noLimitRange(), ordersLimitRange());

        verify(mbiApiService, never()).sendMessageToSupplier(eq(SHOP_ID), anyInt(), anyString());
    }

    @Test
    void updateFlagsAfterSentTest() {
        when(shopFlagService.shopFlagExists(eq(Flag.RATING_MESSAGE_SENT), eq(SHOP_ID), any(Date.class))).thenReturn(false);

        operationalRatingOrderLimitNotifier.notify(SHOP_ID, PartnerModel.DSBB, CREATED_LIMIT, noLimitRange(), ordersLimitRange());

        verify(mbiApiService).sendMessageToSupplier(eq(SHOP_ID), eq(Messages.MBI.RATING_BELOW_HIGH), anyString());
        verify(shopFlagService).addShopFlag(Flag.RATING_MESSAGE_SENT, SHOP_ID);
    }

    @ParameterizedTest(name = "resolve_template_test_{index}")
    @MethodSource("resolveTemplateMethodSource")
    void resolveTemplateTest(CpaOrderLimit orderLimit,
                             PartnerRatingLimitRange previousLimitRange,
                             PartnerRatingLimitRange actualLimitRange,
                             Integer expectedTemplateId) {
        assertEquals(
                expectedTemplateId,
                operationalRatingOrderLimitNotifier.resolveTemplateByRatingRanges(
                        orderLimit, previousLimitRange, actualLimitRange, PartnerModel.DSBB
                )
        );
    }

    static Stream<Arguments> resolveTemplateMethodSource() {
        return Stream.of(
                Arguments.of(mockOrderLimit(), noLimitRange(), ordersLimitRange(), 1600000011),
                Arguments.of(mockOrderLimit(), noLimitRange(), switchOffRange(), 1600000012),
                Arguments.of(mockOrderLimit(), ordersLimitRange(), ordersLimitRange(), 1600000011),
                Arguments.of(mockOrderLimit(), ordersLimitRange(), switchOffRange(), 1600000012),
                Arguments.of(mockOrderLimit(), switchOffRange(), switchOffRange(), 1600000012),
                Arguments.of(mockOrderLimit(), switchOffRange(), ordersLimitRange(), 1600000013),
                Arguments.of(mockOrderLimit(), null, switchOffRange(), 1600000012),
                Arguments.of(mockOrderLimit(), null, ordersLimitRange(), 1600000011),
                Arguments.of(null, null, ordersLimitRange(), null),
                Arguments.of(null, ordersLimitRange(), ordersLimitRange(), null),
                Arguments.of(mockOrderLimit(), ordersLimitRange(), noLimitRange(), 1600000014)
        );
    }

    private static CpaOrderLimit mockOrderLimit() {
        var mock = mock(CpaOrderLimit.class);
        when(mock.getShopId()).thenReturn(SHOP_ID);
        when(mock.getPartnerModel()).thenReturn(PartnerModel.DSBB);

        return mock;
    }

    @Test
    void testXmlBody() {
        String expectedBody = "" +
                "<abo-info>\n" +
                " <shop-name>Ромашка</shop-name>\n" +
                " <order-limit>100</order-limit>\n" +
                " <quality-index-min-limit>40</quality-index-min-limit>\n" +
                "</abo-info>\n";

        assertEquals(expectedBody, OperationalRatingOrderLimitNotifier.buildXmlBody(CREATED_LIMIT, partnerInfo));
    }

    private static PartnerRatingLimitRange switchOffRange() {
        return mockRange(true, false, false);
    }

    private static PartnerRatingLimitRange ordersLimitRange() {
        return mockRange(false, true, false);
    }

    private static PartnerRatingLimitRange noLimitRange() {
        return mockRange(false, false, true);
    }

    private static PartnerRatingLimitRange mockRange(boolean shouldSwitchOff, boolean shouldLimit, boolean withoutLimit) {
        var mock = mock(PartnerRatingLimitRange.class);

        when(mock.shouldSwitchOffPartner()).thenReturn(shouldSwitchOff);
        when(mock.shouldCreateOrdersLimit()).thenReturn(shouldLimit);
        when(mock.withoutLimit()).thenReturn(withoutLimit);

        return mock;
    }
}
