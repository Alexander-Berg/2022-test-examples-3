package ru.yandex.market.wms.shared.libs.printer.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.label.printer.domain.dto.ParcelLabelPrinterData;
import ru.yandex.market.wms.shared.libs.label.printer.service.ParcelLabelDataService;
import ru.yandex.market.wms.shared.libs.printer.config.PrinterTestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.shared.libs.label.printer.service.ParcelLabelDataService.LAST_BOX;
import static ru.yandex.market.wms.shared.libs.label.printer.service.ParcelLabelDataService.LAST_BOX_OF_FEW;
import static ru.yandex.market.wms.shared.libs.label.printer.service.ParcelLabelDataService.NOT_LAST_BOX;

@Import({
        PrinterTestConfig.class,
})
class ParcelLabelDataServiceTest extends IntegrationTest {

    @Autowired
    private ParcelLabelDataService parcelLabelDataService;

    @Test
    @DatabaseSetup(value = "/db/service/parcel-label/db-single-parcel.xml", connection = "wmwhseConnection")
    void singleParcel() {
        var p1 = getAllPackedData("ORD1", "P1", 1, 1, "2.600", LAST_BOX);

        assertThat(parcelLabelDataService.buildPrintDataByParcelId(p1.getCaseId())).contains(p1);
        assertThat(parcelLabelDataService.buildPrintDataById(p1.getCaseId())).contains(p1);

        List.of("UID0001", "UID0002", "UID0003").forEach(uit ->
                assertThat(parcelLabelDataService.buildPrintDataByUit(uit)).contains(p1)
        );
    }

    @Test
    @DatabaseSetup(value = "/db/service/parcel-label/db-packed-dropped.xml", connection = "wmwhseConnection")
    void parcelsPackedAndDropped() {
        var p1 = getAllPackedData("ORD1", "P1", 1, 2, "10.000", NOT_LAST_BOX); // dropped
        var p2 = getAllPackedData("ORD1", "P2", 2, 2, "2.000", String.format(LAST_BOX_OF_FEW, 2)); // packed

        // by parcel id
        assertThat(parcelLabelDataService.buildPrintDataByParcelId(p1.getCaseId())).contains(p1);
        assertThat(parcelLabelDataService.buildPrintDataByParcelId(p2.getCaseId())).contains(p2);

        // by id
        assertThat(parcelLabelDataService.buildPrintDataById(p1.getCaseId())).isEmpty();
        assertThat(parcelLabelDataService.buildPrintDataById(p2.getCaseId())).contains(p2);

        // by uit
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0001")).contains(p1);
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0002")).contains(p2);
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0003")).contains(p2);
    }


    /**
     * Клинетский заказ разбился на 2 заказа, один из которых полностью упакован, второй не упакован
     */
    @Test
    @DatabaseSetup(value = "/db/service/parcel-label/db-split-partially-packed.xml", connection = "wmwhseConnection")
    void orderSplitPartiallyPacked() {
        var p1 = getPartiallyPackedData("ORD1A", "P1", 1, "10.000");
        var p2 = getPartiallyPackedData("ORD1A", "P2", 2, "10.000");

        // by parcel id and id
        List.of(p1, p2).forEach(data -> {
            assertThat(parcelLabelDataService.buildPrintDataByParcelId(data.getCaseId())).contains(data);
            assertThat(parcelLabelDataService.buildPrintDataById(data.getCaseId())).contains(data);
        });

        // by uit
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0001")).contains(p1);
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0002")).contains(p2);
    }

    /**
     * Клинетский заказ разбился на 2 заказа, оба полностью упакованы
     */
    @Test
    @DatabaseSetup(value = "/db/service/parcel-label/db-split-all-packed.xml", connection = "wmwhseConnection")
    void orderSplitAllPacked() {
        var p1 = getAllPackedData("ORD1A", "P1", 1, 3, "10.000", NOT_LAST_BOX);
        var p2 = getAllPackedData("ORD1A", "P2", 2, 3, "10.000", NOT_LAST_BOX);
        var p3 = getAllPackedData("ORD1B", "P3", 3, 3, "1.000", String.format(LAST_BOX_OF_FEW, 3));

        // by parcel id and id
        List.of(p1, p2, p3).forEach(data -> {
            assertThat(parcelLabelDataService.buildPrintDataByParcelId(data.getCaseId())).contains(data);
            assertThat(parcelLabelDataService.buildPrintDataById(data.getCaseId())).contains(data);
        });

        // by uit
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0001")).contains(p1);
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0002")).contains(p2);
        assertThat(parcelLabelDataService.buildPrintDataByUit("UID0003")).contains(p3);
    }

    ParcelLabelPrinterData getPartiallyPackedData(String orderKey, String parcelId, int parcelNumber, String weight) {
        return ParcelLabelPrinterData.builder()
                .externOrderKey("EXTORD1")
                .type(OrderType.STANDARD)
                .carrierName("СД 1")
                .storerName("Поставщик 1")
                .storerKey("SUPPLIER1")
                .state("Мос область")
                .city("Балашиха")
                .zip("11112222")
                .address1("Адрес 1")
                .address2("Адрес 2")
                .address3("Адрес 3")
                .scheduledshipdate("01.04.2020")
                .customerName("Юзер")
                .total("+")
                .lastBoxMessage(NOT_LAST_BOX)
                .caseId(parcelId)
                .boxNumber(Integer.toString(parcelNumber))
                .grossWgt(weight)
                .orderKey(orderKey)
                .build();
    }

    ParcelLabelPrinterData getAllPackedData(String orderKey, String parcelId,
                                            int parcelNumber, int total, String weight, String lastBoxMessage) {
        return getPartiallyPackedData(orderKey, parcelId, parcelNumber, weight)
                .toBuilder()
                .total(String.valueOf(total))
                .lastBoxMessage(lastBoxMessage)
                .build();
    }
}
