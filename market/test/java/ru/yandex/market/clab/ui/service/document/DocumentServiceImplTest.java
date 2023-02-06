package ru.yandex.market.clab.ui.service.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import liquibase.util.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.clab.common.config.component.ConverterConfig;
import ru.yandex.market.clab.common.converter.FulfilmentApiConverter;
import ru.yandex.market.clab.common.service.ProcessingException;
import ru.yandex.market.clab.common.service.billing.BillingActionFilter;
import ru.yandex.market.clab.common.service.billing.PaidActionStats;
import ru.yandex.market.clab.common.service.movement.MovementService;
import ru.yandex.market.clab.common.service.requested.movement.RequestedMovementService;
import ru.yandex.market.clab.common.service.user.UserInfo;
import ru.yandex.market.clab.common.service.warehouse.WarehouseService;
import ru.yandex.market.clab.db.jooq.generated.enums.DocumentFormat;
import ru.yandex.market.clab.db.jooq.generated.enums.DocumentType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Document;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Warehouse;
import ru.yandex.market.clab.ui.data.billing.PaidActionStatsUi;
import ru.yandex.market.clab.ui.data.billing.PaidActionUserStatsUi;
import ru.yandex.market.clab.ui.service.billing.BillingUiService;
import ru.yandex.market.logistic.api.model.fulfillment.Car;
import ru.yandex.market.logistic.api.model.fulfillment.Courier;
import ru.yandex.market.logistic.api.model.fulfillment.LegalEntity;
import ru.yandex.market.logistic.api.model.fulfillment.Outbound;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 11/21/2018
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ConverterConfig.class})
public class DocumentServiceImplTest {

    private AtomicLong idGenerator = new AtomicLong();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MovementService movementService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private RequestedMovementService requestedMovementService;

    @Mock
    private BillingUiService billingUiService;

    private DocumentRepository documentRepository = new DocumentRepositoryStub();

    @Autowired
    private FulfilmentApiConverter fulfilmentApiConverter;

    private DocumentService documentService;

    @Before
    public void setUp() {
        documentService = new DocumentServiceImpl(
            documentRepository, movementService, fulfilmentApiConverter,
            warehouseService, requestedMovementService, billingUiService);
    }

    @Test(expected = ProcessingException.class)
    public void testTorg13IncomingMovement() {
        when(movementService.getMovement(anyLong())).thenReturn(
            createMovement(MovementState.SENDING, MovementDirection.INCOMING));

        documentService.getTorg13(1L);
    }

    @Test(expected = ProcessingException.class)
    public void testTorg13NotSendingMovement() {
        when(movementService.getMovement(anyLong())).thenReturn(
            createMovement(MovementState.PREPARING_TO_OUT, MovementDirection.OUTGOING));

        documentService.getTorg13(1L);
    }

    @Test(expected = ProcessingException.class)
    public void testTorg13NoGoods() {
        when(movementService.getMovement(anyLong())).thenReturn(
            createMovement(MovementState.SENDING, MovementDirection.OUTGOING));
        when(movementService.getGoodsNoData(anyLong())).thenReturn(Collections.emptyList());

        documentService.getTorg13(1L);
    }

    @Test(expected = ProcessingException.class)
    public void testWaybillIncomingMovement() {
        when(movementService.getMovement(anyLong())).thenReturn(
            createMovement(MovementState.SENDING, MovementDirection.INCOMING));

        documentService.getWaybill(1L);
    }

    @Test(expected = ProcessingException.class)
    public void testWaybillNotSendingMovement() {
        when(movementService.getMovement(anyLong())).thenReturn(
            createMovement(MovementState.PREPARING_TO_OUT, MovementDirection.OUTGOING));

        documentService.getWaybill(1L);
    }

    @Test(expected = ProcessingException.class)
    public void testWaybillNoGoods() {
        when(movementService.getMovement(anyLong())).thenReturn(
            createMovement(MovementState.SENDING, MovementDirection.OUTGOING));
        when(movementService.getGoodsNoData(anyLong())).thenReturn(Collections.emptyList());

        documentService.getWaybill(1L);
    }

    @Test
    @Ignore("https://st.yandex-team.ru/MBO-23335#5e11f0eaf3f60814c5f2d77b")
    public void testTorg13AndWaybillGeneration() throws IOException {
        Movement movement = createMovement(MovementState.SENDING, MovementDirection.OUTGOING);
        when(movementService.getMovement(anyLong())).thenReturn(movement);
        when(movementService.getGoodsNoData(anyLong())).thenReturn(Arrays.asList(
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT),
            createGood(GoodState.OUT)
        ));
        when(warehouseService.getOwnWarehouse()).thenReturn(createOwnWarehouse());
        when(warehouseService.getWarehouseById(anyLong())).thenReturn(createWarehouse());

        Document torg13 = documentService.getTorg13(1L);
        Document waybill = documentService.getWaybill(1L);

        assertThat(torg13.getName()).isEqualTo("ЛК0000001");
        assertThat(torg13.getFileName()).isEqualTo("ЛК0000001.pdf");
        assertThat(torg13.getCompatililityFileName()).isEqualTo("CL0000001.pdf");
        assertThat(torg13.getEntityId()).isEqualTo(1L);
        assertThat(torg13.getFormat()).isEqualTo(DocumentFormat.PDF);
        assertThat(torg13.getType()).isEqualTo(DocumentType.TORG13);

        assertThat(waybill.getName()).isEqualTo("ЛКТН0000001");
        assertThat(waybill.getFileName()).isEqualTo("ЛКТН0000001.pdf");
        assertThat(waybill.getCompatililityFileName()).isEqualTo("CLWB0000001.pdf");
        assertThat(waybill.getEntityId()).isEqualTo(1L);
        assertThat(waybill.getFormat()).isEqualTo(DocumentFormat.PDF);
        assertThat(waybill.getType()).isEqualTo(DocumentType.WAYBILL);

        Document newTorg13 = documentService.getTorg13(1L);
        Document newWaybill = documentService.getWaybill(1L);
        assertThat(newTorg13).isEqualTo(torg13);
        assertThat(newWaybill).isEqualTo(waybill);
    }

    @Test
    @Ignore("https://st.yandex-team.ru/MBO-23335#5e11f0eaf3f60814c5f2d77b")
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testBillingReportGeneration() throws IOException {
        UserInfo anmalysh = createUser("anmalysh", "Александр", "Малышев");
        UserInfo shumakovnd = createUser("shumakov-nd", "Никита", "Шумаков");

        PaidActionUserStatsUi anmalyshStats = createUserStats(
            anmalysh,
            createStatsUi(40),
            createStatsUi(50)
        );

        PaidActionUserStatsUi shumakovndStats = createUserStats(
            shumakovnd,
            createStatsUi(0),
            createStatsUi(0)
        );

        when(billingUiService.getUsersStats(any())).thenReturn(Arrays.asList(anmalyshStats, shumakovndStats));

        Document document = documentService.getBillingReport(new BillingActionFilter());

        assertThat(document).isNotNull();
        assertThat(document.getFormat()).isEqualTo(DocumentFormat.XLSX);
        assertThat(document.getFileName()).isEqualTo("БиллингЗаВсеВремя.xlsx");
        assertThat(document.getCompatililityFileName()).isEqualTo("BillingAllTime.xlsx");
        assertBillingReport(document.getData());

        document = documentService.getBillingReport(new BillingActionFilter()
            .setStartDate(LocalDate.of(2019, 6, 1).atStartOfDay()));

        assertThat(document).isNotNull();
        assertThat(document.getFormat()).isEqualTo(DocumentFormat.XLSX);
        assertThat(document.getFileName()).isEqualTo("БиллингС01.06.2019.xlsx");
        assertThat(document.getCompatililityFileName()).isEqualTo("BillingFrom01.06.2019.xlsx");
        assertBillingReport(document.getData());

        document = documentService.getBillingReport(new BillingActionFilter()
            .setEndDate(LocalDate.of(2019, 6, 1).atStartOfDay()));

        assertThat(document).isNotNull();
        assertThat(document.getFormat()).isEqualTo(DocumentFormat.XLSX);
        assertThat(document.getFileName()).isEqualTo("БиллингДо01.06.2019.xlsx");
        assertThat(document.getCompatililityFileName()).isEqualTo("BillingTo01.06.2019.xlsx");
        assertBillingReport(document.getData());

        document = documentService.getBillingReport(new BillingActionFilter()
            .setStartDate(LocalDate.of(2019, 5, 1).atStartOfDay())
            .setEndDate(LocalDate.of(2019, 6, 1).atStartOfDay()));

        assertThat(document).isNotNull();
        assertThat(document.getFormat()).isEqualTo(DocumentFormat.XLSX);
        assertThat(document.getFileName()).isEqualTo("Биллинг01.05.2019-01.06.2019.xlsx");
        assertThat(document.getCompatililityFileName()).isEqualTo("Billing01.05.2019-01.06.2019.xlsx");
        assertBillingReport(document.getData());
    }

    private Good createGood(GoodState goodState) {
        return new Good()
            .setState(goodState)
            .setMskuId(idGenerator.incrementAndGet())
            .setSupplierId(idGenerator.incrementAndGet())
            .setSupplierSkuId("ssku" + idGenerator.incrementAndGet())
            .setMskuId(idGenerator.incrementAndGet())
            .setMskuTitle("msku" + idGenerator.incrementAndGet())
            .setPrice(BigDecimal.valueOf(idGenerator.incrementAndGet()))
            .setWeightNetto(BigDecimal.valueOf(idGenerator.incrementAndGet()))
            .setWeightBrutto(BigDecimal.valueOf(idGenerator.incrementAndGet()));
    }

    private Movement createMovement(MovementState movementState,
                                    MovementDirection movementDirection) {
        Car car = new Car("о001оо100", "Porsche Cayenne Turbo S");
        Person person = new Person("Иван", "Иванов", "Иванович");
        Phone phone = new Phone("88005553535", "1937");
        LegalEntity legalEntity = new LegalEntity.LegalEntityBuilder().setLegalName("ООО \"Рога и Копыта\"").build();
        Courier courier = new Courier(Collections.singletonList(person), car, phone, legalEntity);
        Outbound outbound = new Outbound.OutboundBuilder(
            new ResourceId("yandexId", "contentLabId"),
            StockType.FIT,
            Collections.emptyList(),
            courier,
            legalEntity,
            null
        ).build();
        return new Movement()
            .setId(1L)
            .setOutbound(fulfilmentApiConverter.serializeOutboundData(outbound))
            .setState(movementState)
            .setDirection(movementDirection)
            .setWarehouseId(1L);
    }

    private Warehouse createOwnWarehouse() {
        return new Warehouse()
            .setId(idGenerator.incrementAndGet())
            .setAddress("Россия, 127018, г. Москва, ул. Полковая, д.3")
            .setName("Суперсклад")
            .setOrgName("ООО \"Без лоха и жизнь плоха\"")
            .setUnitName("Коллекторское бюро ООО \"Без лоха и жизнь плоха\"");
    }

    private Warehouse createWarehouse() {
        return new Warehouse()
            .setId(idGenerator.incrementAndGet())
            .setAddress("Россия, 100500, г. Москва, ул. Задрищенская, д. 10, кв. 12")
            .setName("Лаборатория Контента")
            .setOrgName("ООО \"ЯНДЕКС.МАРКЕТ\"")
            .setUnitName("Лаборатория Контента ООО \"ЯНДЕКС.МАРКЕТ\"");
    }

    private UserInfo createUser(String login, String firstName, String lastName) {
        UserInfo result = new UserInfo();
        result.setLogin(login);
        result.setFirstName(firstName);
        result.setLastName(lastName);
        return result;
    }

    private PaidActionUserStatsUi createUserStats(UserInfo user, PaidActionStatsUi... statsUi) {
        PaidActionUserStatsUi userStatsUi = new PaidActionUserStatsUi(user.getLogin(), Arrays.asList(statsUi));
        userStatsUi.setUserInfo(user);
        return userStatsUi;
    }

    private PaidActionStatsUi createStatsUi(long priceSum) {
        return new PaidActionStatsUi(
            new PaidActionStats(PaidAction.GOOD_ACCEPT, 1, priceSum), "something");
    }

    private void assertBillingReport(byte[] data) throws IOException {
        InputStream tsvStream = getClass().getResourceAsStream("/document/billing-report.tsv");
        List<List<String>> expectedData = IOUtils.readLines(tsvStream, StandardCharsets.UTF_8).stream()
            .map(line -> Arrays.asList(line.split("\t", -1)))
            .collect(Collectors.toList());

        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data));
        assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getSheetName()).isEqualTo("Список сотрудников");
        for (int row = 0; row < expectedData.size(); row++) {
            List<String> expectedRow = expectedData.get(row);
            for (int col = 0; col < expectedRow.size(); col++) {
                assertCellValue(sheet.getRow(row).getCell(col), expectedRow.get(col));
            }
        }
    }

    private void assertCellValue(Cell cell, String value) {
        if (StringUtils.isEmpty(value) && cell == null) {
            return;
        }
        switch (cell.getCellTypeEnum()) {
            case STRING:
                assertThat(cell.getStringCellValue()).isEqualTo(value);
                break;
            case NUMERIC:
                assertThat(cell.getNumericCellValue()).isEqualTo(Double.valueOf(value));
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
