package ru.yandex.market.core.supplier.state;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.tariff.db.dao.CalculatorLogDao;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.supplier.state.PartnerFulfillmentLinkChangeEvent.ChangeType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Проверям {@link PartnerFulfillmentLinkUpdateListener}.
 */
@DbUnitDataSet(before = "PartnerFulfillmentLinkUpdateListenerTest.before.csv")
class PartnerFulfillmentLinkUpdateListenerTest extends FunctionalTest {

    @Autowired
    private CalculatorLogDao calculatorLogDao;

    @Autowired
    private PartnerTypeAwareService awareService;

    @Autowired
    private DataCampClient dataCampShopClient;

    @Autowired
    private PartnerFulfillmentLinkUpdateNotificationSender partnerFulfillmentLinkUpdateNotificationSender;

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Autowired
    private BusinessService businessService;

    private static long OUR_WAREHOUSE_ID = 145L;
    private static long NOT_OUR_WAREHOUSE_ID = 199L;

    private PartnerFulfillmentLinkUpdateListener partnerFulfillmentLinkUpdateListener;

    private static Stream<Arguments> argsForWarehouseAdd() {
        return Stream.of(
                Arguments.of(1010, 0),
                Arguments.of(1011, 1),
                Arguments.of(1012, 0),
                Arguments.of(1013, 1)
        );
    }

    @BeforeEach
    void init() {
        partnerFulfillmentLinkUpdateListener =
                new PartnerFulfillmentLinkUpdateListener(calculatorLogDao, awareService,
                        deliveryInfoService, businessService, partnerFulfillmentLinkUpdateNotificationSender);
    }

    @DisplayName("ФФ склад, который у нас есть, не создается заново")
    @Test
    void ourWarehouseNotAddedTest() {
        int partnerId = 1011;

        PartnerFulfillmentLinkChangeEvent event =
                new PartnerFulfillmentLinkChangeEvent(partnerId, Long.valueOf(10), OUR_WAREHOUSE_ID, ChangeType.UPDATE);

        partnerFulfillmentLinkUpdateListener.handleLinkChangedEvent(event);
        verify(dataCampShopClient, times(0))
                .addWarehouse(eq(partnerId), eq(10L), eq(OUR_WAREHOUSE_ID), eq(1014L));
        ;
    }


    @DisplayName("Добавление привязки к новому складу")
    @ParameterizedTest
    @MethodSource("argsForWarehouseAdd")
    void addWarehouseTest(long partnerId, int wantedNumber) {
        PartnerFulfillmentLinkChangeEvent event =
                new PartnerFulfillmentLinkChangeEvent(partnerId, Long.valueOf(10), NOT_OUR_WAREHOUSE_ID,
                        ChangeType.UPDATE);

        partnerFulfillmentLinkUpdateListener.handleLinkChangedEvent(event);
    }

    @DisplayName("Добавление привязки к новому складу с пустым фидом, не должно пройти")
    @Test
    void addWarehouseNullFeedTest() {
        PartnerFulfillmentLinkChangeEvent event =
                new PartnerFulfillmentLinkChangeEvent(1013, null, NOT_OUR_WAREHOUSE_ID, ChangeType.UPDATE);

        partnerFulfillmentLinkUpdateListener.handleLinkChangedEvent(event);
    }
}
