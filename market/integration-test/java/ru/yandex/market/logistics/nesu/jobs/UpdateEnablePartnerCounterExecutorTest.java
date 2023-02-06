package ru.yandex.market.logistics.nesu.jobs;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.ConnectedPartnerDataRequest;
import ru.yandex.market.logistics.management.entity.response.partner.ConnectedPartnerDataDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.api.converter.ShipmentTypeConverter;
import ru.yandex.market.logistics.nesu.jobs.executor.UpdateEnablePartnerCounterExecutor;
import ru.yandex.market.logistics.nesu.service.geo.LocationService;
import ru.yandex.market.logistics.nesu.service.lms.PartnerService;
import ru.yandex.market.logistics.nesu.service.logisticpointavailability.LogisticPointAvailabilityService;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DatabaseSetup("/repository/connected-partners/prepare.xml")
@DisplayName("Обновление счетчика количества магазинов, подключенных к точке сдачи")
public class UpdateEnablePartnerCounterExecutorTest extends AbstractContextualTest {

    private static final ConnectedPartnerDataRequest REQUEST = new ConnectedPartnerDataRequest(
        List.of(1L, 2L, 5L, 6L),
        List.of(PartnerType.DROPSHIP, PartnerType.SUPPLIER)
    );

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();
    @Autowired
    private LogisticPointAvailabilityService logisticPointAvailabilityService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private EnumConverter enumConverter;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ShipmentTypeConverter shipmentTypeConverter;

    private UpdateEnablePartnerCounterExecutor enablePartnerCounterExecutor;

    @BeforeEach
    public void setup() {
        enablePartnerCounterExecutor = new UpdateEnablePartnerCounterExecutor(
            logisticPointAvailabilityService,
            locationService,
            partnerService,
            enumConverter,
            transactionTemplate,
            shipmentTypeConverter
        );
    }

    @AfterEach
    public void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Обновление счетчика")
    @ExpectedDatabase(
        value = "/repository/connected-partners/result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void doJob() {
        when(lmsClient.getConnectedPartnerData(REQUEST)).thenReturn(dataDtos());

        enablePartnerCounterExecutor.doJob(null);
        verify(lmsClient).getConnectedPartnerData(REQUEST);
        verifyBackLog();
    }

    @Test
    @DisplayName("Обновление счетчика для забора и экспресса одновременно")
    @DatabaseSetup("/repository/connected-partners/withdraw_prepare.xml")
    @ExpectedDatabase(
        value = "/repository/connected-partners/withdraw_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void doJobForWithdraw() {
        ConnectedPartnerDataRequest request = new ConnectedPartnerDataRequest(
            List.of(1L, 5L, 5L),
            List.of(PartnerType.DROPSHIP, PartnerType.SUPPLIER)
        );
        when(lmsClient.getConnectedPartnerData(request)).thenReturn(withdrawDtos());

        enablePartnerCounterExecutor.doJob(null);
        verify(lmsClient).getConnectedPartnerData(request);
    }

    @Nonnull
    private List<ConnectedPartnerDataDto> dataDtos() {
        return List.of(
            ConnectedPartnerDataDto.builder()
                .locationId(3)
                .connectedShopsCount(2L)
                .logisticsPointId(1L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.IMPORT)
                .warehousePartnerType(PartnerType.SORTING_CENTER)
                .build(),
            ConnectedPartnerDataDto.builder()
                .locationId(1)
                .connectedShopsCount(3L)
                .logisticsPointId(1L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.IMPORT)
                .warehousePartnerType(PartnerType.SORTING_CENTER)
                .build(),
            ConnectedPartnerDataDto.builder()
                .locationId(10001)
                .connectedShopsCount(1L)
                .logisticsPointId(2L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.IMPORT)
                .warehousePartnerType(PartnerType.SORTING_CENTER)
                .build(),
            ConnectedPartnerDataDto.builder()
                .locationId(1)
                .connectedShopsCount(10L)
                .logisticsPointId(5L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.WITHDRAW)
                .warehousePartnerType(PartnerType.SORTING_CENTER)
                .build(),
            ConnectedPartnerDataDto.builder()
                .locationId(1)
                .connectedShopsCount(1L)
                .logisticsPointId(6L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.WITHDRAW)
                .warehousePartnerType(PartnerType.SORTING_CENTER)
                .build()
        );
    }

    @Nonnull
    private List<ConnectedPartnerDataDto> withdrawDtos() {
        return List.of(
            ConnectedPartnerDataDto.builder()
                .locationId(10001)
                .connectedShopsCount(5L)
                .logisticsPointId(1L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.WITHDRAW)
                .build(),
            ConnectedPartnerDataDto.builder()
                .locationId(10001)
                .connectedShopsCount(12L)
                .logisticsPointId(5L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.WITHDRAW)
                .build(),
            ConnectedPartnerDataDto.builder()
                .locationId(1)
                .connectedShopsCount(11L)
                .logisticsPointId(5L)
                .partnerType(PartnerType.DROPSHIP)
                .shipmentType(ShipmentType.WITHDRAW)
                .build()
        );
    }

    private void verifyBackLog() {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=WARN\t"
                    + "format=plain\t"
                    + "payload=Has been used more than 80% of logistics point partner capacity\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "tags=LOGISTIC_POINT_PARTNER_CAPACITY_EXPIRED\t"
                    + "extra_keys=logisticPointAvailabilityIds\textra_values=1;2;5"
            );
    }
}
