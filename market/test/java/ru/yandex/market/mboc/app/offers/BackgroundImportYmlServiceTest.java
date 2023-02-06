package ru.yandex.market.mboc.app.offers;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

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
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.converter.YmlFileToImportedOfferConverter;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
public class BackgroundImportYmlServiceTest extends AbstractBackgroundImportServiceTest {

    private static final String FILE_NAME = "some file.yml";

    @Test
    public void testException() throws IOException, InterruptedException {
        byte[] bytes = readResource("yml/valid-all-offers.yml");

        Mockito.doAnswer(i -> {
            throw new IllegalStateException("Fine exception for test purposes");
        }).when(enrichmentService).enrichOffers(anyList(), any());

        int actionId = backgroundImportService.startImportFile(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importFileService
                .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).contains("IllegalStateException");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testError() throws IOException, InterruptedException {
        byte[] bytes = readResource("yml/invalid-all-offers.yml");


        int actionId = backgroundImportService.startImportFile(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importFileService
                .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                    new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Errors in import");

        Result result = (Result) action.getResult();
        assertSoftly(softly -> {
            assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
            softly.assertThat(result.getMessage())
                .contains("Url is not defined")
                .contains("содержит недопустимые символы")
                .contains("Ошибка на строке 2")
                .contains("Ошибка на строке 3")
                .contains("Ошибка на строке 4")
                .contains("Ошибка на строке 5")
                .contains("Ошибка на строке 6")
                .contains("Ошибка на строке 7")
                .contains("Ошибка на строке 8")
                .contains("Invalid url");
            softly.assertThat(offerRepository.findAll()).hasSize(0);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMasterDataParsingError() throws IOException, InterruptedException {

        ImportedOffer importedOffer = generateOffer();
        importedOffer.setMasterData(ExcelHeaders.NDS, "incorrect nds");
        importedOffer.setShopSkuId(TEST_SSKU);

        importFileService = importYmlServiceAnsweringOffers(importedOffer);

        byte[] bytes = readResource("yml/valid-all-offers.yml"); // чтобы парсинг не упал

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importFileService
                .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
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

        Mockito.verify(masterDataHelperService, Mockito.never()).saveSskuMasterDataAndDocuments(Mockito.any());
    }

    @Test
    public void testMasterDataParsingWarnings() throws IOException, InterruptedException {

        ImportedOffer importedOffer = generateOffer();
        importedOffer.getMasterData().remove(ExcelHeaders.NDS);

        ErrorInfo xErrorInfo = new ErrorInfo("code", "message X",
            ErrorInfo.Level.WARNING, Collections.emptyMap());
        ErrorInfo yErrorInfo = new ErrorInfo("code", "message Y",
            ErrorInfo.Level.WARNING, Collections.emptyMap());
        List<ImportedOfferToMasterDataConverter.MasterDataConvertResult> masterDataConvertResultList =
            Collections.singletonList(
                new ImportedOfferToMasterDataConverter.MasterDataConvertResult(14, generateMasterData(importedOffer),
                    Arrays.asList(
                        new ErrorAtLine(14, xErrorInfo),
                        new ErrorAtLine(42, yErrorInfo)
                    ))
            );

        importedOfferToMasterDataConverter = mock(ImportedOfferToMasterDataConverter.class);
        when(importedOfferToMasterDataConverter.convert(anyList())).thenReturn(masterDataConvertResultList);

        importFileService = importYmlServiceAnsweringOffers(importedOffer);


        byte[] bytes = readResource("yml/valid-all-offers.yml"); // чтобы парсинг не упал

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importFileService
                .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
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
    private ImportFileService importYmlServiceAnsweringOffers(ImportedOffer... offers) {
        YmlFileToImportedOfferConverter ymlFileToImportedOfferConverter = mock(
            YmlFileToImportedOfferConverter.class
        );
        when(ymlFileToImportedOfferConverter.parse(anyInt(), any(), any()))
            .thenAnswer(c -> {
                OffersParseResult.Builder<Object> resultBuilder = OffersParseResult.newBuilder();
                IntStream.range(0, offers.length).forEach(i -> resultBuilder.addOffer(i + 1, offers[i]));
                return resultBuilder.build();
            });

        Mockito.doAnswer(i -> {
            List<Offer> offrs = i.getArgument(0);
            offrs.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        var keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue("is1pDatacampProcessingEnabled", false);

        ImportOffersProcessService processService = new ImportOffersProcessService(
            offerRepository,
            masterDataRepositoryMock,
            masterDataHelperService,
            enrichmentService, TransactionHelper.MOCK,
            importedOfferToMasterDataConverter,
            supplierRepository,
            applySettingsService,
            Mockito.mock(OffersProcessingStatusService.class),
            transformImportOffersService,
            Mockito.mock(GlobalVendorsCachingService.class),
            false,
            keyValueService);


        return new ImportFileService(
            importExcelService,
            new ImportYmlService(ymlFileToImportedOfferConverter, processService)
        );
    }

    @Test
    public void testCorrect() throws IOException, InterruptedException {
        byte[] bytes = readResource("yml/valid-all-offers.yml");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch atEnrichment = new CountDownLatch(1);
        CountDownLatch passEnrichment = new CountDownLatch(1);
        backgroundActionService.startAction(BackgroundActionService.Context.builder().build(),
            ThrowingConsumer.rethrow(a -> start.await()));

        Mockito.doAnswer(i -> {
            atEnrichment.countDown();
            passEnrichment.await();
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        try {
            int actionId = backgroundImportService.startImportFile(FILE_NAME, DESCRIPTION, LOGIN,
                actionHandle -> importFileService
                    .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                        new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));

            // Ещё не начали:
            BackgroundAction action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).isNull();

            start.countDown(); // Запустили
            atEnrichment.await(); // и ждём, пока дойдёт до enrichment

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).contains("Прочитаны строки, количество: 4, ошибки: 0");

            passEnrichment.countDown();

            backgroundActionService.stop(); // Теперь ждём конца

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isTrue();
            assertThat(action.getState()).isEqualTo("Done");

            Result result = (Result) action.getResult();
            assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);

            ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.verify(masterDataHelperService).saveSskuMasterDataAndDocuments(argumentCaptor.capture());
            Collection<MasterData> masterDataArgument = argumentCaptor.getValue();
            assertThat(masterDataArgument).extracting(MasterData::getShopSku)
                .containsExactlyInAnyOrder("386007", "386011", "386012", "book1");
            assertThat(masterDataArgument).extracting(MasterData::getShelfLife)
                .containsExactlyInAnyOrder(
                    new TimeInUnits(1, TimeInUnits.TimeUnit.YEAR),
                    new TimeInUnits(6, TimeInUnits.TimeUnit.MONTH),
                    new TimeInUnits(370, TimeInUnits.TimeUnit.DAY),
                    new TimeInUnits(370, TimeInUnits.TimeUnit.DAY)
                );
            assertThat(masterDataArgument).extracting(MasterData::getSupplierId).containsOnly(SUPPLIER_ID);
            assertThat(masterDataArgument).extracting(MasterData::getVat).containsOnly(VatRate.VAT_20);
            assertThat(masterDataArgument).extracting(MasterData::getBoxCount).contains(2);
            assertThat(masterDataArgument).extracting(MasterData::getMinShipment).contains(11);
            assertThat(masterDataArgument).extracting(MasterData::getSupplySchedule)
                .contains(List.of(new SupplyEvent(DayOfWeek.MONDAY), new SupplyEvent(DayOfWeek.FRIDAY)));
            assertThat(masterDataArgument).flatExtracting(MasterData::getManufacturerCountries)
                .containsOnly(COUNTRY_FROM_CORRECT_FILE);
        } finally {
            // В случае ошибки может и не отпустить.
            start.countDown();
            passEnrichment.countDown();
        }
    }

    @Test
    public void testDoubles() throws IOException, InterruptedException {
        byte[] bytes = readResource("yml/double-offer.yml");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch atEnrichment = new CountDownLatch(1);
        CountDownLatch passEnrichment = new CountDownLatch(1);
        backgroundActionService.startAction(BackgroundActionService.Context.builder().build(),
            ThrowingConsumer.rethrow(a -> start.await()));

        Mockito.doAnswer(i -> {
            atEnrichment.countDown();
            passEnrichment.await();
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        try {
            int actionId = backgroundImportService.startImportFile(FILE_NAME, DESCRIPTION, LOGIN,
                actionHandle -> importFileService
                    .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                        new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));

            // Ещё не начали:
            BackgroundAction action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).isNull();

            start.countDown(); // Запустили
            atEnrichment.await(); // и ждём, пока дойдёт до enrichment

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).contains("Прочитаны строки, количество: 2, ошибки: 1");

            passEnrichment.countDown();

            backgroundActionService.stop(); // Теперь ждём конца

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isTrue();
            assertThat(action.getState()).isEqualTo("Errors in import");

            Result result = (Result) action.getResult();
            assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        } finally {
            // В случае ошибки может и не отпустить.
            start.countDown();
            passEnrichment.countDown();
        }
    }

    @Test
    public void testWrongShopSku() throws IOException, InterruptedException {
        byte[] bytes = readResource("yml/wrong-offer.yml");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch atEnrichment = new CountDownLatch(1);
        CountDownLatch passEnrichment = new CountDownLatch(1);
        backgroundActionService.startAction(BackgroundActionService.Context.builder().build(),
            ThrowingConsumer.rethrow(a -> start.await()));

        Mockito.doAnswer(i -> {
            atEnrichment.countDown();
            passEnrichment.await();
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        try {
            int actionId = backgroundImportService.startImportFile(FILE_NAME, DESCRIPTION, LOGIN,
                actionHandle -> importFileService
                    .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                        new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));

            // Ещё не начали:
            BackgroundAction action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).isNull();

            start.countDown(); // Запустили
            atEnrichment.await(); // и ждём, пока дойдёт до enrichment

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).contains("Прочитаны строки, количество: 1, ошибки: 1");

            passEnrichment.countDown();

            backgroundActionService.stop(); // Теперь ждём конца

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isTrue();
            assertThat(action.getState()).isEqualTo("Errors in import");

            Result result = (Result) action.getResult();
            assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        } finally {
            // В случае ошибки может и не отпустить.
            start.countDown();
            passEnrichment.countDown();
        }
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

    @Test
    public void testMissedParamValue() throws IOException, InterruptedException {
        byte[] bytes = readResource("yml/missed-param-offer.yml");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch atEnrichment = new CountDownLatch(1);
        CountDownLatch passEnrichment = new CountDownLatch(1);
        backgroundActionService.startAction(BackgroundActionService.Context.builder().build(),
            ThrowingConsumer.rethrow(a -> start.await()));

        Mockito.doAnswer(i -> {
            atEnrichment.countDown();
            passEnrichment.await();
            List<Offer> offers = i.getArgument(0);
            offers.forEach(o -> o.setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED));
            return null;
        }).when(enrichmentService).enrichOffers(anyList(), any());

        try {
            int actionId = backgroundImportService.startImportFile(FILE_NAME, DESCRIPTION, LOGIN,
                actionHandle -> importFileService
                    .importFile(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN,
                        new ImportFileService.ImportSettings(ImportFileService.SavePolicy.ALL_OR_NOTHING)));

            // Ещё не начали:
            BackgroundAction action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).isNull();

            start.countDown(); // Запустили
            atEnrichment.await(); // и ждём, пока дойдёт до enrichment

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).contains("Прочитаны строки, количество: 1, ошибки: 1");

            passEnrichment.countDown();

            backgroundActionService.stop(); // Теперь ждём конца

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isTrue();
            assertThat(action.getState()).isEqualTo("Errors in import");

            Result result = (Result) action.getResult();
            assertSoftly(softly -> {
                softly.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
                softly.assertThat(result.getMessage()).contains("Offer param value is not defined");
                softly.assertThat(result.getMessage()).contains("Ошибка на строке 2");
            });
        } finally {
            // В случае ошибки может и не отпустить.
            start.countDown();
            passEnrichment.countDown();
        }
    }
}
