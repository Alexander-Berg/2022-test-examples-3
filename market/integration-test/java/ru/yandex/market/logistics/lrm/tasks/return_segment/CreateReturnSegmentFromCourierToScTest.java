package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.entity.embedded.ShipmentFields.Courier;
import ru.yandex.market.logistics.lrm.repository.ReturnSegmentRepository;
import ru.yandex.market.logistics.lrm.service.ReturnSegmentService;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

@DisplayName("Обработка создания начального сегмента СЦ после получения события от курьерки")
@DatabaseSetup("/database/tasks/return-segment/create-courier-to-sc/before/common.xml")
@ParametersAreNonnullByDefault
class CreateReturnSegmentFromCourierToScTest extends AbstractIntegrationTest {

    private static final long PARTNER_ID = 100;
    private static final long WAREHOUSE_ID = 200;
    private static final long COURIER_ID = 123;
    private static final long COURIER_UID = 234;
    private static final String WAREHOUSE_EXTERNAL_ID = "300";
    private static final String BOX_EXTERNAL_ID = "box-external-id";

    @Autowired
    private ReturnSegmentRepository returnSegmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ReturnSegmentService returnSegmentService;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-09-06T11:12:13.00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успешное создание следующего сегмента для СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-courier-to-sc/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        execute(null, defaultCourier());
    }

    @Test
    @DisplayName("Успешное создание следующего сегмента для СЦ, курьер не передан")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-courier-to-sc/after/without_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successWithoutCourier() {
        execute(null, null);
    }

    @Test
    @DisplayName("Несколько идентификаторов партнёров СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-courier-to-sc/after/multiple_sorting_center_ids.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sortingCenterPartnerIdsOverride() {
        execute(List.of(110L, 120L), defaultCourier());
    }

    private void execute(@Nullable List<Long> sortingCenterPartnerIds, @Nullable Courier courierDto) {
        transactionTemplate.execute(status -> {
            returnSegmentService.createScSegment(
                returnSegmentRepository.findById(10L).orElseThrow(),
                logisticsPointResponse(),
                partnerResponse(),
                sortingCenterPartnerIds,
                WAREHOUSE_ID,
                courierDto
            );
            return null;
        });
    }

    @Nonnull
    private LogisticsPointResponse logisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(WAREHOUSE_ID)
            .partnerId(PARTNER_ID)
            .externalId(WAREHOUSE_EXTERNAL_ID)
            .name("склад сц")
            .build();
    }

    @Nonnull
    private PartnerResponse partnerResponse() {
        return PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .name("partner name")
            .build();
    }

    @Nonnull
    private static Courier defaultCourier() {
        return new Courier()
            .setId(COURIER_ID)
            .setUid(COURIER_UID)
            .setName("courier")
            .setPhoneNumber("+7-000-000-00-00")
            .setCarNumber("car");
    }

}
