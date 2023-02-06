package ru.yandex.market.wms.packing.websocket;

import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.StorerType;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.dropping.client.DroppingClient;
import ru.yandex.market.wms.dropping.core.model.DropInfoResponse;
import ru.yandex.market.wms.dropping.core.model.ParcelDto;
import ru.yandex.market.wms.dropping.core.model.StorerDto;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.pojo.CarrierCompany;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.utils.PackingFlow;
import ru.yandex.market.wms.packing.utils.PackingTaskDataset;
import ru.yandex.market.wms.packing.utils.Parcel;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static ru.yandex.market.wms.packing.BaseDbObjects.CARTON_YMA;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"})
public class PackingWithDroppingTest extends PackingIntegrationTest {

    private static final String USER = "TEST";
    private static final String YMA = CARTON_YMA.getType();
    private static final CarrierCompany CARRIER_1 = new CarrierCompany("CARRIER-MP1", "СД-МП");
    private static final CarrierCompany CARRIER_2 = new CarrierCompany("CARRIER-MP2", "СД-МП2");
    private static final String DRP1 = "DRP1";
    private static final String DRP2 = "DRP2";
    private static final String DRP3 = "DRP3";
    private static final LocalDate YESTERDAY = LocalDate.parse("2020-03-31");
    private static final LocalDate TODAY = LocalDate.parse("2020-04-01");
    private static final LocalDate TOMORROW = LocalDate.parse("2020-04-02");

    @MockBean
    @Autowired
    private DroppingClient droppingClient;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(droppingClient);
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/dropping/normal/setup.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void normalFlow() throws Exception {
        PackingTable table = LocationsRov.TABLE_1;
        PackingFlow flow = createPackingFlow().connect(USER, LocationsRov.TABLE_1);

        // первый заказ, после него закешируется дефолтная дропка
        // дата у него вчерашняя, но считается как сегодняшняя
        String parcelId = "P000000501";
        String drp = DRP1;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_1), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto(parcelId, YESTERDAY)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0777").uits(List.of("UID0001"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping().scannedDropId(drp)
                .scanDropInfo(CloseParcelResponse.ScanDropInfo.builder()
                        .carrier(CARRIER_1).shipDate(TODAY).isDefault(true)
                        .build())));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        // второй заказ, с другой СД - надо сканировать дропку
        parcelId = "P000000502";
        drp = DRP2;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_2), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto(parcelId, TODAY)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0778").uits(List.of("UID0002"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping().scannedDropId(drp)
                .scanDropInfo(CloseParcelResponse.ScanDropInfo.builder()
                        .carrier(CARRIER_2).shipDate(TODAY).isDefault(false)
                        .build())));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        // третий заказ, совпадает по СД и ПДО (сегодня), сканировать дропку не надо, само дропнется на дефолтную
        parcelId = "P000000503";
        drp = DRP1;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_1), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto("P000000501", YESTERDAY), new ParcelDto(parcelId, TODAY)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0780").uits(List.of("UID0004"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping()));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        // четветый заказ, с будущей ПДО - надо сканировать дропку
        parcelId = "P000000504";
        drp = DRP3;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_1), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto(parcelId, TOMORROW)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0779").uits(List.of("UID0003"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping().scannedDropId(drp)
                .scanDropInfo(CloseParcelResponse.ScanDropInfo.builder()
                        .carrier(CARRIER_1).shipDate(TOMORROW).isDefault(false)
                        .build())));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        Mockito.verifyNoMoreInteractions(droppingClient);
        flow.disconnect();
    }

    /**
     * Три заказа с одной СД и ПДО
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/dropping/default-drop-closing/setup.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flowWithDefaultDropClosing() throws Exception {
        PackingTable table = LocationsRov.TABLE_1;
        PackingFlow flow = createPackingFlow().connect(USER, LocationsRov.TABLE_1);

        // первый заказ, после него закешируется дефолтная дропка
        String parcelId = "P000000501";
        String drp = DRP1;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_1), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto(parcelId, TODAY)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0777").uits(List.of("UID0001"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping().scannedDropId(drp)
                .scanDropInfo(CloseParcelResponse.ScanDropInfo.builder()
                        .carrier(CARRIER_1).shipDate(TODAY).isDefault(true)
                        .build())));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        // второй заказ, сканировать дропку не надо, само дропнется на дефолтную
        parcelId = "P000000502";
        drp = DRP1;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_1), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto("P000000501", TODAY), new ParcelDto(parcelId, TODAY)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0778").uits(List.of("UID0002"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping()));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        // закрываем дефолтную дропку
        flow.closeDrop(DRP1);

        // третий заказ, дефолтной дропки нет -> надо сканировать дропку
        parcelId = "P000000503";
        drp = DRP2;
        Mockito.when(droppingClient.putParcelOnDrop(parcelId, drp, table.getLoc()))
                .thenReturn(new DropInfoResponse(toStorerDto(CARRIER_1), drp, DropInfoResponse.Status.IN_PROGRESS,
                        List.of(new ParcelDto(parcelId, TODAY)), false));

        flow.packSortable(PackingTaskDataset.of(Parcel.builder().orderKey("ORD0779").uits(List.of("UID0003"))
                .parcelId(parcelId).parcelNumber(1).carton(YMA).isLast().withDropping().scannedDropId(drp)
                .scanDropInfo(CloseParcelResponse.ScanDropInfo.builder()
                        .carrier(CARRIER_1).shipDate(TODAY).isDefault(true)
                        .build())));

        Mockito.verify(droppingClient).putParcelOnDrop(parcelId, drp, table.getLoc());

        Mockito.verifyNoMoreInteractions(droppingClient);
        flow.disconnect();
    }

    private static StorerDto toStorerDto(CarrierCompany carrier) {
        return new StorerDto(carrier.getStorerKey(), StorerType.CARRIER, carrier.getCompany());
    }
}
