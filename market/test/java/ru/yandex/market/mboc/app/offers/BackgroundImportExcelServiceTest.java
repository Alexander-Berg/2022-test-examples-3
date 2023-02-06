package ru.yandex.market.mboc.app.offers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundAction;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter.MasterDataConvertResult;
import ru.yandex.market.mboc.common.offers.ExcelS3ServiceMock;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.converter.ExcelFileToOffersConverter;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.ThrowingConsumer;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.web.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author yuramalinov
 * @created 15.05.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BackgroundImportExcelServiceTest extends AbstractBackgroundImportServiceTest {

    private static final String FILE_NAME = "some file.xlsx";

    @Test
    public void testException() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/CorrectSample.xls");

        doAnswer(i -> {
            throw new IllegalStateException("Fine exception for test purposes");
        }).when(enrichmentService).enrichOffers(anyList(), any());

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).contains("IllegalStateException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testError() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/BrokenSample.xls");

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Errors in import");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMasterDataParsingError() throws IOException, InterruptedException {

        ImportedOffer importedOffer = generateOffer();
        importedOffer.setMasterData(ExcelHeaders.NDS, "incorrect nds");
        importedOffer.setShopSkuId(TEST_SSKU);

        importExcelService = importExcelServiceAnsweringOffers(importedOffer);

        byte[] bytes = readResource("excel/CorrectSample.xls"); // чтобы парсинг не упал

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Errors in import");

        Result result = (Result) action.getResult();
        assertSoftly(softly -> {
            softly.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
            softly.assertThat(result.getMessage()).contains("НДС 'incorrect nds' должен иметь одно из значений");
            softly.assertThat(result.getMessage()).contains("Ошибка на строке 2");
        });

        verify(masterDataHelperService, Mockito.never()).saveSskuMasterDataAndDocuments(Mockito.any());
    }

    @Test
    public void testMasterDataParsingWarnings() throws IOException, InterruptedException {

        ImportedOffer importedOffer = generateOffer();
        importedOffer.getMasterData().remove(ExcelHeaders.NDS);

        ErrorInfo xErrorInfo = new ErrorInfo("code", "message X", ErrorInfo.Level.WARNING, Collections.emptyMap());
        ErrorInfo yErrorInfo = new ErrorInfo("code", "message Y", ErrorInfo.Level.WARNING, Collections.emptyMap());
        List<MasterDataConvertResult> masterDataConvertResultList = Collections.singletonList(
            new MasterDataConvertResult(14, generateMasterData(importedOffer),
                Arrays.asList(
                    new ErrorAtLine(14, xErrorInfo),
                    new ErrorAtLine(42, yErrorInfo)
                ))
        );

        importedOfferToMasterDataConverter = mock(ImportedOfferToMasterDataConverter.class);
        when(importedOfferToMasterDataConverter.convert(anyList())).thenReturn(masterDataConvertResultList);

        importExcelService = importExcelServiceAnsweringOffers(importedOffer);

        byte[] bytes = readResource("excel/CorrectSample.xls"); // чтобы парсинг не упал

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Done with warnings");

        Result result = (Result) action.getResult();
        assertSoftly(softly -> {
            softly.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.WARNING);
            softly.assertThat(result.getMessage()).contains("Файл импортирован, но есть замечания");
            softly.assertThat(result.getMessage()).contains("message X");
            softly.assertThat(result.getMessage()).contains("message Y");
            softly.assertThat(result.getMessage()).contains("Ошибка на строке 15");
            softly.assertThat(result.getMessage()).contains("Ошибка на строке 43");
            softly.assertThat(offerRepository.findAll()).hasSize(1);
        });

        verify(masterDataHelperService, times(1)).saveSskuMasterDataAndDocuments(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCorrect() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/CorrectSample.xls");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch atEnrichment = new CountDownLatch(1);
        CountDownLatch passEnrichment = new CountDownLatch(1);
        backgroundActionService.startAction(BackgroundActionService.Context.builder().build(),
            ThrowingConsumer.rethrow(a -> start.await()));

        doAnswer(i -> {
            atEnrichment.countDown();
            passEnrichment.await();
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        try {
            int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
                actionHandle -> importExcelService
                    .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                        new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));

            // Ещё не начали:
            BackgroundAction action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).isNull();

            start.countDown(); // Запустили
            atEnrichment.await(); // и ждём, пока дойдёт до enrichment

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).contains("Прочитаны строки, количество: 1, ошибки: 0");

            passEnrichment.countDown();

            backgroundActionService.stop(); // Теперь ждём конца

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isTrue();
            assertThat(action.getState()).isEqualTo("Done");

            Result result = (Result) action.getResult();
            assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);

            ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
            verify(masterDataHelperService).saveSskuMasterDataAndDocuments(argumentCaptor.capture());
            Collection<MasterData> masterDataArgument = argumentCaptor.getValue();
            assertThat(masterDataArgument).extracting(MasterData::getShopSku).containsExactlyInAnyOrder(SSKU_FROM_FILE);
            assertThat(masterDataArgument).extracting(MasterData::getSupplierId).containsExactlyInAnyOrder(SUPPLIER_ID);
            assertThat(masterDataArgument).extracting(MasterData::getVat).containsExactlyInAnyOrder(VatRate.VAT_10);
            assertThat(masterDataArgument).flatExtracting(MasterData::getManufacturerCountries)
                .containsExactlyInAnyOrder(COUNTRY_FROM_CORRECT_FILE);
        } finally {
            // В случае ошибки может и не отпустить.
            start.countDown();
            passEnrichment.countDown();
        }
    }

    @Test
    public void testAuthenticationIsSetForBackgorundAction() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/CorrectSample.xls");

        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, "test-user",
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, "test-user",
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop(); // Ждем конца обработки

        assertThat(offerRepository.getLastAuditStaffLogin(1)).contains("test-user");
    }

    @Test
    public void testDifferentTimeUnits() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/DifferentTimeUnits.xls");

        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        when(mboTimeUnitAliasesService.getTimeUnitAliases(anyLong()))
            .thenReturn(ImmutableMap.of("нед", TimeInUnits.TimeUnit.WEEK, "г", TimeInUnits.TimeUnit.YEAR));

        backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, "test-user",
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, "test-user",
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop(); // Ждем конца обработки

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(masterDataHelperService).saveSskuMasterDataAndDocuments(argumentCaptor.capture());
        List<MasterData> masterDataArgument = argumentCaptor.getValue();
        assertThat(masterDataArgument).extracting(MasterData::getShelfLife).containsExactly(
            new TimeInUnits(5, TimeInUnits.TimeUnit.WEEK),
            new TimeInUnits(2, TimeInUnits.TimeUnit.MONTH));
        assertThat(masterDataArgument).extracting(MasterData::getLifeTime).containsExactly(
            new TimeInUnits(2, TimeInUnits.TimeUnit.YEAR),
            new TimeInUnits(1, TimeInUnits.TimeUnit.YEAR));
        assertThat(masterDataArgument).extracting(MasterData::getGuaranteePeriod).containsExactly(
            new TimeInUnits(30, TimeInUnits.TimeUnit.DAY),
            new TimeInUnits(50, TimeInUnits.TimeUnit.DAY));
    }

    @Test
    public void testMissingTimeUnits() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/MissingTimeUnits.xls");

        doAnswer(i -> {
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        when(mboTimeUnitAliasesService.getTimeUnitAliases(anyLong()))
            .thenReturn(ImmutableMap.of("нед", TimeInUnits.TimeUnit.WEEK, "г", TimeInUnits.TimeUnit.YEAR));

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, "test-user",
            actionHandle -> importExcelService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, "test-user",
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop(); // Ждем конца обработки

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Errors in import");

        Result result = (Result) action.getResult();
        assertSoftly(softly -> {
            softly.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
            softly.assertThat(result.getMessage()).contains(
                "Не получилось распарсить значение в столбце 'Срок службы': " +
                    "Неизвестная единица [years] в строке [2 years]");
            softly.assertThat(result.getMessage()).contains("Ошибка на строке 2");
        });

        verify(masterDataHelperService, Mockito.never()).saveSskuMasterDataAndDocuments(Mockito.any());
    }

    @Test
    public void testBusinessIdImport() throws IOException, InterruptedException {
        supplierRepository.deleteAll();
        Supplier bizSupplier = OfferTestUtils.businessSupplier();
        supplierRepository.insert(bizSupplier);

        Supplier supplier3p = new Supplier(SUPPLIER_ID, "biz child")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setBusinessId(OfferTestUtils.BIZ_ID_SUPPLIER);
        supplierRepository.insert(supplier3p);

        testCorrect();

        List<Offer> offers = offerRepository.findAll();
        assertThat(offers).extracting(Offer::getBusinessId).containsOnly(OfferTestUtils.BIZ_ID_SUPPLIER);
    }

    @SuppressWarnings("unchecked")
    private ImportExcelService importExcelServiceAnsweringOffers(ImportedOffer... offers) {
        ExcelFileToOffersConverter<ImportedOffer> importedExcelFileConverter = mock(
            ExcelFileToOffersConverter.class
        );
        when(importedExcelFileConverter.parse(anyInt(), any(), any()))
            .thenAnswer(c -> {
                OffersParseResult.Builder<Object> resultBuilder = OffersParseResult.newBuilder();
                IntStream.range(0, offers.length).forEach(i -> resultBuilder.addOffer(i + 1, offers[i]));
                return resultBuilder.build();
            });

        doAnswer(i -> {
            List<Offer> offrs = i.getArgument(0);
            offrs.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());


        var keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue("is1pDatacampProcessingEnabled", false);

        return new ImportExcelService(
            importedExcelFileConverter,
            new ExcelS3ServiceMock(),
            new ImportOffersProcessService(
                offerRepository, masterDataRepositoryMock, masterDataHelperService,
                enrichmentService, TransactionHelper.MOCK,
                importedOfferToMasterDataConverter,
                supplierRepository,
                applySettingsService,
                Mockito.mock(OffersProcessingStatusService.class),
                transformImportOffersService,
                Mockito.mock(GlobalVendorsCachingService.class),
                false,
                keyValueService)
        );
    }
}
