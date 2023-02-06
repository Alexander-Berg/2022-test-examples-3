package ru.yandex.market.wms.shared.libs.printer.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.label.printer.dao.ParcelLabelDataDao;
import ru.yandex.market.wms.shared.libs.label.printer.domain.dto.ParcelLabelPrinterData;
import ru.yandex.market.wms.shared.libs.printer.config.PrinterTestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(PrinterTestConfig.class)
@DatabaseSetup(value = "/db/dao/parcel-label-data/common.xml", connection = "wmwhseConnection")
class ParcelLabelDataDaoTest extends IntegrationTest {

    private static final ParcelLabelPrinterData P123_STANDARD = ParcelLabelPrinterData.builder()
            .orderKey("ORD0666")
            .externOrderKey("EXT0666")
            .storerName("STORER COMPANY")
            .address1("C_ADDRESS1")
            .address2("C_ADDRESS2")
            .address3("C_ADDRESS3")
            .city("C_CITY")
            .zip("C_ZIP")
            .state("C_STATE")
            .scheduledshipdate("06.01.2020")
            .carrierName("СД-МП")
            .caseId("P123")
            .boxNumber("1")
            .grossWgt("1.010")
            .customerName("C_COMPANY")
            .type(OrderType.STANDARD)
            .storerKey("987")
            .build();

    private static final ParcelLabelPrinterData P124_STANDARD = P123_STANDARD.toBuilder()
            .caseId("P124")
            .boxNumber("2")
            .grossWgt("3.350")
            .build();

    private static final ParcelLabelPrinterData P123_WITHDRAWAL = toWithdrawal(P123_STANDARD);
    private static final ParcelLabelPrinterData P124_WITHDRAWAL = toWithdrawal(P124_STANDARD);

    @Autowired
    private ParcelLabelDataDao parcelLabelDataDao;

    @Test
    @DatabaseSetup(value = "/db/dao/parcel-label-data/db-default.xml", connection = "wmwhseConnection")
    void findParcelIdByUitStandard() {
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456789")).contains(P123_STANDARD.getCaseId());
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456790")).contains(P124_STANDARD.getCaseId());
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456791")).contains(P124_STANDARD.getCaseId());
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456792")).contains(P124_STANDARD.getCaseId());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/parcel-label-data/db-withdrawal.xml", connection = "wmwhseConnection")
    void findParcelIdByUitWithdrawal() {
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456789")).contains(P123_WITHDRAWAL.getCaseId());
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456790")).contains(P124_WITHDRAWAL.getCaseId());
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456791")).contains(P124_WITHDRAWAL.getCaseId());
        assertThat(parcelLabelDataDao.findParcelIdByUit("123456792")).contains(P124_WITHDRAWAL.getCaseId());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/parcel-label-data/db-default.xml", connection = "wmwhseConnection")
    void findByParcelIdStandard() {
        assertThat(parcelLabelDataDao.findByParcelId(P123_STANDARD.getCaseId())).contains(P123_STANDARD);
        assertThat(parcelLabelDataDao.findByParcelId(P124_STANDARD.getCaseId())).contains(P124_STANDARD);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/parcel-label-data/db-withdrawal.xml", connection = "wmwhseConnection")
    void findByIdWithdrawal() {
        assertThat(parcelLabelDataDao.findById(P123_WITHDRAWAL.getCaseId())).contains(P123_WITHDRAWAL);
        assertThat(parcelLabelDataDao.findById(P124_WITHDRAWAL.getCaseId())).contains(P124_WITHDRAWAL);
    }

    @Test
    public void stringNormalizationTest() {

        String test1 = "$[gyn]s\\d^f.$a";
        assertEquals("$[gyn]sd^f.$a", ParcelLabelDataDao.normalize(test1));

        String test2 = " \\a";
        assertEquals(" a", ParcelLabelDataDao.normalize(test2));

        String test3 = "\\\\";
        assertEquals("", ParcelLabelDataDao.normalize(test3));

        assertNull(ParcelLabelDataDao.normalize(null), "no exception should be thrown");
    }

    private static ParcelLabelPrinterData toWithdrawal(ParcelLabelPrinterData data) {
        return data.toBuilder()
                .type(OrderType.OUTBOUND_FIT)
                .boxNumber(null)
                .build();
    }
}
