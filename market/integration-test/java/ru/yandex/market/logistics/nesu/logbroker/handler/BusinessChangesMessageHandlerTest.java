package ru.yandex.market.logistics.nesu.logbroker.handler;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.producer.PushDbsShopLicenseToLmsProducer;
import ru.yandex.market.logistics.nesu.model.entity.type.ShopLicenseType;
import ru.yandex.market.logistics.nesu.service.shop.ShopLicenseService;
import ru.yandex.market.logistics.nesu.service.shop.ShopService;
import ru.yandex.market.partner.event.PartnerInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тесты логики обработчика сообщений из топика mbi/.../business_changes")
@DatabaseSetup("/jobs/executors/yt_update_dbs_licenses/before/update_license_setup.xml")
class BusinessChangesMessageHandlerTest extends AbstractContextualTest {

    private static final long EPOCH_SECOND_TIMESTAMP = Instant.parse("2022-01-27T21:43:00Z").getEpochSecond();

    @Autowired
    private ShopLicenseService shopLicenseService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private PushDbsShopLicenseToLmsProducer pushDbsShopLicenseToLmsProducer;

    @BeforeEach
    void setUp() {
        doNothing()
            .when(pushDbsShopLicenseToLmsProducer)
            .produceTask(
                anyLong(),
                any(ShopLicenseType.class)
            );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(pushDbsShopLicenseToLmsProducer);
    }

    @Test
    @DisplayName("Есть события магазинов DBS-партнеров")
    @ExpectedDatabase(
        value = "/logbroker/handler/after/licenses_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void hasDbsEvents() {
        new BusinessChangesMessageHandler(shopLicenseService, shopService, pushDbsShopLicenseToLmsProducer).process(
            List.of(
                PartnerInfo.PartnerInfoEvent.newBuilder()
                    .setId(1L)
                    .setIsDropshipBySeller(true)
                    .setSellsMedicine(true)
                    .setIsMedicineCourier(true)
                    .setTimestamp(EPOCH_SECOND_TIMESTAMP)
                    .build(),
                PartnerInfo.PartnerInfoEvent.newBuilder()
                    .setId(2L)
                    .setIsDropshipBySeller(true)
                    .setSellsMedicine(true)
                    .setIsMedicineCourier(false)
                    .setTimestamp(EPOCH_SECOND_TIMESTAMP)
                    .build(),
                PartnerInfo.PartnerInfoEvent.newBuilder()
                    .setId(4L)
                    .setIsDropshipBySeller(true)
                    .setSellsMedicine(true)
                    .setIsMedicineCourier(false)
                    .setTimestamp(EPOCH_SECOND_TIMESTAMP)
                    .build(),
                PartnerInfo.PartnerInfoEvent.newBuilder()
                    .setId(5L)
                    .setIsDropshipBySeller(false)
                    .setSellsMedicine(false)
                    .setIsMedicineCourier(true)
                    .setTimestamp(EPOCH_SECOND_TIMESTAMP)
                    .build()
        ));

        verify(pushDbsShopLicenseToLmsProducer).produceTask(1L, ShopLicenseType.CAN_SELL_MEDICINE);
        verify(pushDbsShopLicenseToLmsProducer).produceTask(2L, ShopLicenseType.CAN_DELIVER_MEDICINE);
    }

    @Test
    @DisplayName("Нет событий магазинов DBS-партнеров")
    @ExpectedDatabase(
        value = "/jobs/executors/yt_update_dbs_licenses/before/update_license_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noDbsEvents() {
        new BusinessChangesMessageHandler(shopLicenseService, shopService, pushDbsShopLicenseToLmsProducer).process(
            List.of(
                PartnerInfo.PartnerInfoEvent.newBuilder()
                    .setId(1L)
                    .setIsDropshipBySeller(false)
                    .setSellsMedicine(false)
                    .setIsMedicineCourier(false)
                    .setTimestamp(EPOCH_SECOND_TIMESTAMP)

                    .build(),
                PartnerInfo.PartnerInfoEvent.newBuilder()
                    .setId(2L)
                    .setIsDropshipBySeller(false)
                    .setSellsMedicine(true)
                    .setIsMedicineCourier(true)
                    .setTimestamp(EPOCH_SECOND_TIMESTAMP)
                    .build()
        ));
    }
}
