package ru.yandex.market.ff.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMultimap;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.reader.GridReaderProvider;
import ru.yandex.market.ff.grid.validation.Violation;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.ExternalRequestItemErrorInfo;
import ru.yandex.market.ff.model.entity.ShopRequestDocument;
import ru.yandex.market.ff.repository.ShopRequestDocumentRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.ff.client.enums.ExternalRequestItemErrorSource.MBO;
import static ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType.ACTUAL_SUPPLY_ITEMS_COUNT;
import static ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType.ACTUAL_SUPPLY_PALETTES_COUNT;
import static ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType.AVAILABLE_TO_SUPPLY_ITEMS_COUNT;
import static ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType.AVAILABLE_TO_SUPPLY_PALETTES_COUNT;
import static ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType.SUPPLY_DATE;
import static ru.yandex.market.ff.i18n.ErrorTypeToMessageMappingUtil.SUPPLY_QUOTA_EXCEEDED_DATE_FORMAT;

/**
 * Интеграционный тест для {@link ErrorDocumentGenerationService}.
 *
 * @author avetokhin 11/02/18.
 */
@ContextConfiguration(classes = ErrorDocumentGenerationServiceTest.Conf.class)
@ActiveProfiles("ErrorDocumentGenerationServiceTest")
@DatabaseSetup("classpath:service/error-doc-generation/before.xml")
class ErrorDocumentGenerationServiceTest extends IntegrationTest {

    private static final String SUPPLY_PATH = "supply/valid";

    private static final Map<String, EnrichmentResultContainer> BACKGROUND_ERRORS;
    private static final ImmutableMultimap<GridCell, Violation> UPLOAD_ERRORS;

    static {
        var serviceIds = new HashSet<Long>();
        serviceIds.add(null);
        BACKGROUND_ERRORS = generateBackgroundErrors(serviceIds);

        UPLOAD_ERRORS = ImmutableMultimap.<GridCell, Violation>builder()
                .putAll(new DefaultGridCell(0, 0, "test"),
                        new Violation("Some error"), new Violation("Another error"))
                .putAll(new DefaultGridCell(1, 3, "test"), new Violation("Epic error"))
                .build();
    }

    @Autowired
    private ErrorDocumentGenerationService service;

    @Autowired
    private ShopRequestDocumentRepository documentRepository;

    @Autowired
    private GridReaderProvider gridReaderProvider;

    private static Map<String, EnrichmentResultContainer> generateBackgroundErrors(@Nonnull Set<Long> serviceIds) {

        EnrichmentResultContainer errorsContainer = new EnrichmentResultContainer(1);

        serviceIds.forEach(serviceId -> {
            Arrays.stream(RequestItemErrorType.values())
                    .filter(err -> err != RequestItemErrorType.ITEM_HAS_ASSORTMENT_ERROR)
                    .forEach(errorType -> errorsContainer.addValidationError(errorType, serviceId, null, null));

            errorsContainer.addExternalError(
                    new ExternalRequestItemErrorInfo(
                            1L, 1L, serviceId, null,
                            MBO, "CustomErrorCode",
                            "CustomTemplate", "AttributesJson", false,
                            "Запрещены поставки msku Электрический духовой шкаф AEG B 4001 4A " +
                                    "#1606994 на склад Маршрут ФФ #145 c 1999-01-01")
            );
        });

        return Map.of("BBBBBBBBBBBBBBBBBB", errorsContainer);
    }


    @Test
    void generateCsvUpload() throws IOException {
        InputStream is = generateUpload(FileExtension.CSV);
        assertCsv(is, "service/error-doc-generation/upload_errors.csv");
    }

    @Test
    void generateXlsUpload() throws IOException {
        InputStream is = generateUpload(FileExtension.XLS);
        assertUploadExcel(is);
        // IOUtils.copy(is, new FileOutputStream("test.xls"));
    }

    @Test
    void generateXlsxUpload() throws IOException {
        InputStream is = generateUpload(FileExtension.XLSX);
        assertUploadExcel(is);
        // IOUtils.copy(is, new FileOutputStream("test.xlsx"));
    }

    /**
     * В результате валидациии возвращается:
     * - 2 идентичные ошибки от MBO для 2-х разных дат по 172 складу
     * - 3 идентичные внутренние ошибки валидации для 3-х разных дат по 147 складу
     * <p>
     * В отчете получим: по одной ошибке в колонках для 172 и 147
     */
    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void generateCsvBackgroundWithSingleErrorsForTwoWarehouses() throws IOException {
        // external errors for 172 warehouse
        var errorContainer1 = new EnrichmentResultContainer(1L);
        errorContainer1.addExternalError(new ExternalRequestItemErrorInfo(
                4L, 1L, 172L, LocalDate.of(2020, 7, 1), MBO,
                "mboc.msku.error.supply-forbidden.category.warehouse",
                "Запрещены поставки msku в категории #{{categoryId}} на склад #{{warehouseId}}",
                "{\"warehouseId\":172, \"categoryId\":15694869}", false, null)
        );
        errorContainer1.addExternalError(new ExternalRequestItemErrorInfo(
                4L, 1L, 172L, LocalDate.of(2020, 7, 2), MBO,
                "mboc.msku.error.supply-forbidden.category.warehouse",
                "Запрещены поставки msku в категории #{{categoryId}} на склад #{{warehouseId}}",
                "{\"warehouseId\":172, \"categoryId\":15694869}", false, null)
        );

        // internal errors for 147 warehouse
        var errorContainer2 = new EnrichmentResultContainer(2L);
        errorContainer2.addValidationError(
                RequestItemErrorType.SUPPLY_QUOTA_EXCEEDED, 147L, LocalDate.of(2001, 1, 11), null);
        errorContainer2.addValidationError(
                RequestItemErrorType.SUPPLY_QUOTA_EXCEEDED, 147L, LocalDate.of(2001, 1, 12), null);
        errorContainer2.addValidationError(
                RequestItemErrorType.SUPPLY_QUOTA_EXCEEDED, 147L, null, null);

        var errors = Map.of(
                "AAAAAAAAAAAAAAAAAA", errorContainer1,
                "BBBBBBBBBBBBBBBBBB", errorContainer2
        );

        ShopRequestDocument document = documentRepository.getById(6);
        InputStream is = service.generateDocumentFile(document, errors, false);

        assertCsv(is, "service/error-doc-generation/multi-warehouse-with-single-error-by-service-id.csv");
        // IOUtils.copy(is, new FileOutputStream("test.csv"));
    }


    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void fileWithQuotaExceededWithDescriptionErrors() throws IOException {

        // internal errors for 147 warehouse
        var errorContainer2 = new EnrichmentResultContainer(2L);

        final Map<RequestItemErrorAttributeType, String> attributes =
                new HashMap<>();
        attributes.put(ACTUAL_SUPPLY_ITEMS_COUNT, "200");
        attributes.put(ACTUAL_SUPPLY_PALETTES_COUNT, "10");
        attributes.put(SUPPLY_DATE,
                LocalDate.of(2022, 5, 19).format(SUPPLY_QUOTA_EXCEEDED_DATE_FORMAT));
        attributes.put(AVAILABLE_TO_SUPPLY_ITEMS_COUNT, "100");
        attributes.put(AVAILABLE_TO_SUPPLY_PALETTES_COUNT, "5");

        errorContainer2.addValidationError(
                RequestItemErrorType.SUPPLY_QUOTA_EXCEEDED, 147L, LocalDate.of(2001, 1, 11), attributes);

        var errors = Map.of(
                "BBBBBBBBBBBBBBBBBB", errorContainer2
        );

        ShopRequestDocument document = documentRepository.getById(6);
        InputStream is = service.generateDocumentFile(document, errors, false);

        assertCsv(is, "service/error-doc-generation/quota-exceeded-with-description.csv");
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void generateCsvBackgroundWithErrorsForOneSpecifiedWarehouseAndNotSpecifiedWarehouse() throws IOException {
        var errorContainer1 = new EnrichmentResultContainer(1L);
        errorContainer1.addValidationError(
                RequestItemErrorType.CALENDARING_IS_NOT_APPLICABLE_FOR_REQUEST_BUT_REQUIRED, 147L,
                LocalDate.of(2001, 1, 11), null);

        var errorContainer2 = new EnrichmentResultContainer(2L);
        errorContainer2.addValidationError(
                RequestItemErrorType.SUPPLY_QUOTA_EXCEEDED, null, LocalDate.of(2001, 1, 11), null);

        var errors = Map.of(
                "AAAAAAAAAAAAAAAAAA", errorContainer1,
                "BBBBBBBBBBBBBBBBBB", errorContainer2
        );

        ShopRequestDocument document = documentRepository.getById(6);
        InputStream is = service.generateDocumentFile(document, errors, false);

        assertCsv(is, "service/error-doc-generation/one-specified-warehouse-and-not-specified-error.csv");
        // IOUtils.copy(is, new FileOutputStream("test.csv"));
    }

    private InputStream generateUpload(final FileExtension extension) {
        final Grid grid = gridReaderProvider.provide(extension).read(supplyFile(extension));
        return service.generateDocumentFile(grid, extension, UPLOAD_ERRORS, RequestType.SUPPLY);
    }

    private InputStream generateBackground(final long documentId) {
        final ShopRequestDocument document = documentRepository.getById(documentId);
        return service.generateDocumentFile(document, BACKGROUND_ERRORS, false);
    }

    private void assertCsv(final InputStream is, final String resourceName) throws IOException {
        String result = IOUtils.toString(is, StandardCharsets.UTF_8);
        InputStream expectedIs =
                ClassLoader.getSystemResourceAsStream(resourceName);
        assertNotNull(expectedIs);
        String expected = IOUtils.toString(expectedIs, StandardCharsets.UTF_8);
        assertThat(result, equalTo(expected));
    }

    private void assertUploadExcel(InputStream is) throws IOException {
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        assertHeaderRow(sheet.getRow(0));
        assertFirstRow(sheet.getRow(1), "Ваш SKU: Some error; Ваш SKU: Another error");
        assertSecondRow(sheet.getRow(2), "Количество товаров в поставке: Epic error");
    }

    private void assertSecondRow(final Row row, final String error) {
        assertThat(row.getCell(0).getStringCellValue(), equalTo("BBBBBBBBBBBBBBBBBB"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Товар2"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("123456679011[~{}"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("2"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("20"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("2"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("коммент"));
        if (error == null) {
            assertThat(row.getCell(8), nullValue());
        } else {
            assertThat(row.getCell(8).getStringCellValue(), equalTo(error));
        }
    }

    private void assertFirstRow(final Row row, final String error) {
        assertThat(row.getCell(0).getStringCellValue(), equalTo("AAAAAAAAAAAAAAAAAA"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Товар1"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("123456679012,3846578491843"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("1"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("50.5"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("VAT_10"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("12"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo(""));
        if (error == null) {
            assertThat(row.getCell(8), nullValue());
        } else {
            assertThat(row.getCell(8).getStringCellValue(), equalTo(error));
        }
    }

    private void assertHeaderRow(final Row row) {
        assertThat(row.getCell(0).getStringCellValue(), equalTo("Ваш SKU"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Название товара"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Штрихкоды"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("Количество товаров в поставке"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Объявленная ценность одного товара, руб."));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("НДС"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Сколько мест занимает один товар"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Комментарий для склада"));
        assertThat(row.getCell(8).getStringCellValue(), equalTo("Ошибки валидации (заполняется Маркетом)"));
    }

    @Configuration
    @Profile("ErrorDocumentGenerationServiceTest")
    static class Conf {

        @Bean
        @Primary
        MdsS3Service mdsS3Service() {
            final MdsS3Service mdsS3Service = mock(MdsS3Service.class);

            doAnswer(invocation -> {
                final FileExtension extension = invocation.getArgument(3);
                final InputStream is = supplyFile(extension);
                final OutputStream os = invocation.getArgument(4);
                IOUtils.copy(is, os);
                return null;
            }).when(mdsS3Service).downloadRequestDocument(anyLong(), anyLong(), any(), any(), any());

            return mdsS3Service;
        }
    }

    private static InputStream supplyFile(final FileExtension extension) {
        return ClassLoader.getSystemResourceAsStream(String.format("%s.%s", SUPPLY_PATH,
                extension.name().toLowerCase()));
    }

}
