package ru.yandex.market.delivery.transport_manager.service.trn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.converter.trn.TrnInformationConverter;
import ru.yandex.market.delivery.transport_manager.domain.dto.PartnerKey;
import ru.yandex.market.delivery.transport_manager.domain.dto.PartnerMarketKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitMeta;
import ru.yandex.market.delivery.transport_manager.service.LegalInfoService;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.logistic_point.LogisticPointSearchService;
import ru.yandex.market.delivery.transport_manager.service.movement.courier.MovementCourierService;
import ru.yandex.market.delivery.transport_manager.service.trn.dto.TrnInformation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;

public class TrnTemplaterServiceTest extends AbstractContextualTest {

    private static final String APP_TEXT = "Акт приема-передачи отправлений № %s от %s";

    @Autowired
    private TrnTemplaterService trnTemplaterService;

    @Autowired
    private MovementCourierService movementCourierService;

    @Autowired
    private LegalInfoService legalInfoService;

    @Autowired
    private LogisticPointSearchService logisticPointSearchService;

    @Autowired
    private TrnInformationConverter trnInformationConverter;

    @Autowired
    private TransportationService transportationService;

    @Autowired
    private TestableClock clock;

    @Test
    void testArrayNotNull() {
        doReturn(Optional.of(new MovementCourier()))
                .when(movementCourierService).getLatestCourier(any(), any());
        doReturn(legalInfos()).when(legalInfoService).find(any());
        doReturn(List.of()).when(logisticPointSearchService).getLogisticsPointsForUnits(anySet());
        doReturn(TrnInformation.builder().build())
                .when(trnInformationConverter).convert(any());
        var fakeInbound = new TransportationUnit();
        var fakeOutbound = new TransportationUnit();
        fakeInbound.setId(1L);
        fakeInbound.setMarketId(2L);
        fakeOutbound.setId(2L);
        fakeOutbound.setMarketId(1L);
        var fakeMovement = new Movement();
        fakeMovement.setMarketId(3L);
        var transportation = new Transportation();
        transportation.setId(1L);
        transportation.setMovement(fakeMovement);
        transportation.setOutboundUnit(fakeOutbound);
        transportation.setInboundUnit(fakeInbound);
        byte[] trnDocumentAsBytes = trnTemplaterService.getTrnDocumentAsBytes(transportation, new Register());
        Assertions.assertNotNull(trnDocumentAsBytes);
        Assertions.assertTrue(trnDocumentAsBytes.length > 0);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/trn/transportation_units.xml",
            "/repository/trn/transportations.xml",
            "/repository/trn/legal_info.xml",
            "/repository/trn/movement_courier.xml",
            "/repository/trn/address.xml",
            "/repository/trn/transportation_metadata.xml",
        }
    )
    void testDocumentIsFilled() throws IOException {
        LocalDateTime testDateTime = LocalDateTime.of(2022, 1, 1, 20, 0, 0);
        clock.setFixed(
            testDateTime.toInstant(ZoneOffset.UTC),
            ZoneId.ofOffset("UTC", ZoneOffset.UTC)
        );

        var transportation = transportationService.getById(1L);
        var register = createRegister();
        byte[] trnDocumentAsBytes = trnTemplaterService.getTrnDocumentAsBytes(transportation, register);

        File file = File.createTempFile("trn_test", "xlsx");

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(trnDocumentAsBytes);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet firstSheet = workbook.getSheetAt(0);

            assertNumericCellIsNotEmpty(2, 3, firstSheet);
            assertCellIsNotEmpty(2, 6, firstSheet);
            assertCellIsNotEmpty(5, 0, firstSheet);
            assertCellIsNotEmpty(6, 0, firstSheet);
            assertCellIsNotEmpty(10, 9, firstSheet);
            assertCellIsNotEmpty(12, 6, firstSheet);
            assertCellIsNotEmpty(14, 0, firstSheet);
            assertCellIsNotEmpty(17, 9, firstSheet);
            assertCellIsNotEmpty(19, 0, firstSheet);
            assertCellIsNotEmpty(22, 9, firstSheet);
            assertCellValueContains(24, 0, firstSheet, String.format(APP_TEXT, "TMU2", testDateTime.toLocalDate()));
            assertCellIsNotEmpty(36, 0, firstSheet);
            assertCellIsNotEmpty(37, 9, firstSheet);
            assertCellIsNotEmpty(39, 0, firstSheet);
            assertCellIsNotEmpty(39, 9, firstSheet);
            assertCellDoesNotThrowException(41, 0, firstSheet);
            assertCellIsNotEmpty(46, 0, firstSheet);
            assertCellIsNotEmpty(50, 0, firstSheet);
            assertCellIsNotEmpty(50, 6, firstSheet);
            assertNumericCellIsNotEmpty(52, 0, firstSheet);
            assertNumericCellIsNotEmpty(52, 6, firstSheet);
            assertCellIsNotEmpty(54, 0, firstSheet);
            assertCellDoesNotThrowException(56, 0, firstSheet);
            assertCellIsNotEmpty(68, 0, firstSheet);
            assertCellIsNotEmpty(68, 6, firstSheet);
        }
    }

    @Test
    @Disabled
    @DatabaseSetup(
        value = {
            "/repository/trn/transportation_units.xml",
            "/repository/trn/transportations.xml",
            "/repository/trn/legal_info.xml",
            "/repository/trn/movement_courier.xml",
            "/repository/trn/address.xml",
            "/repository/trn/transportation_metadata.xml",
        }
    )
    void testSwapLegalInfos() throws IOException {
        var transportation = transportationService.getById(1L);
        var register = createRegister();
        byte[] trnDocumentAsBytes = trnTemplaterService.getTrnDocumentAsBytes(transportation, register);

        File file = File.createTempFile("trn_test", "xlsx");

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(trnDocumentAsBytes);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet firstSheet = workbook.getSheetAt(0);

            assertCellValueContains(5, 0, firstSheet, "ЯНДЕКС");
            assertCellValueContains(12, 0, firstSheet, "Маркет.Операции");
        }
    }

    @Test
    @Disabled
    @DatabaseSetup(
        value = {
            "/repository/trn/transportation_units.xml",
            "/repository/trn/transportations.xml",
            "/repository/trn/movement_courier.xml",
            "/repository/trn/address.xml",
            "/repository/trn/transportation_metadata.xml",
        }
    )
    void testEmptyLegalInfos() throws IOException {
        var transportation = transportationService.getById(1L);
        var register = createRegister();
        byte[] trnDocumentAsBytes = trnTemplaterService.getTrnDocumentAsBytes(transportation, register);

        File file = File.createTempFile("trn_test", "xlsx");

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(trnDocumentAsBytes);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet firstSheet = workbook.getSheetAt(0);

            assertCellValueContains(5, 0, firstSheet, "");
            assertCellValueContains(12, 0, firstSheet, "");
            assertCellValueContains(10, 9, firstSheet, "нет");
        }
    }

    private Register createRegister() {
        var items = List.of(createItem(), createItem());

        Register register = new Register();
        register.setItems(items);
        return register;
    }

    private RegisterUnit createItem() {
        Korobyte korobyte = new Korobyte();
        korobyte.setHeight(2000);
        korobyte.setLength(3000);
        korobyte.setWidth(500);
        korobyte.setWeightGross(BigDecimal.valueOf(5));

        RegisterUnit registerUnit = new RegisterUnit();
        registerUnit.setKorobyte(korobyte);
        registerUnit.setUnitMeta(createUnitMeta());
        return registerUnit;
    }

    private UnitMeta createUnitMeta() {
        UnitMeta unitMeta = new UnitMeta();
        unitMeta.setPrice(BigDecimal.valueOf(100L));
        return unitMeta;
    }

    private void assertCellValueContains(int row, int cell, Sheet sheet, String expected) {
        Assertions.assertTrue(sheet.getRow(row).getCell(cell).getStringCellValue().contains(expected));
    }

    private void assertCellIsNotEmpty(int row, int cell, Sheet sheet) {
        Assertions.assertFalse(sheet.getRow(row).getCell(cell).getStringCellValue().isEmpty());
    }

    private void assertNumericCellIsNotEmpty(int row, int cell, Sheet sheet) {
        Assertions.assertTrue(sheet.getRow(row).getCell(cell).getNumericCellValue() != 0);
    }

    private void assertCellDoesNotThrowException(int row, int cell, Sheet sheet) {
        Assertions.assertDoesNotThrow(() -> sheet.getRow(row).getCell(cell).getNumericCellValue());
    }

    private HashMap<PartnerMarketKey, TransportationLegalInfo> legalInfos() {
        var map = new HashMap<PartnerMarketKey, TransportationLegalInfo>();
        map.put(PartnerMarketKey.of(new PartnerKey(1L, null), 1L), new TransportationLegalInfo().setLegalName("Some"));
        map.put(PartnerMarketKey.of(new PartnerKey(1L, null), 2L), new TransportationLegalInfo().setLegalName("Some"));
        map.put(PartnerMarketKey.of(new PartnerKey(1L, null), 3L), new TransportationLegalInfo().setLegalName("Some"));
        return map;
    }
}
