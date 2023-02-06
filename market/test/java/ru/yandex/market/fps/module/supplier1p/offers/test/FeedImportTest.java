package ru.yandex.market.fps.module.supplier1p.offers.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.fps.module.supplier1p.offers.Feed;
import ru.yandex.market.fps.module.supplier1p.offers.XlsxByMarketTemplateWritingService;
import ru.yandex.market.fps.module.supplier1p.offers.impl.ValidationData;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.logic.def.PreviewableFileUploadResult;
import ru.yandex.market.jmf.logic.wf.WfService;
import ru.yandex.market.jmf.mds.UploadResult;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.ItemMessage;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.ParsedOffer;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.ProcessingStatus;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.RequestMode;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.Response;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.Result;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.ResultRequest;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload.Status;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer.Offer;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import ru.yandex.market.mdm.http.MdmCommon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Transactional
@SpringJUnitConfig(InternalModuleSupplier1pOffersTestConfiguration.class)
public class FeedImportTest extends AbstractFeedTest {
    public static final int MBOC_VERIFY_REQUEST_ID = 123;
    public static final int MBOC_COMMIT_REQUEST_ID = 321;
    public static final PreviewableFileUploadResult FIRST_XLSX_UPLOAD_RESULT = new PreviewableFileUploadResult(
            new UploadResult("url1", ResourceLocation.create("bucket", "key"), 123L),
            "type"
    );
    public static final PreviewableFileUploadResult SECOND_XLSX_UPLOAD_RESULT = new PreviewableFileUploadResult(
            new UploadResult("url2", ResourceLocation.create("bucket", "key"), 123L),
            "type"
    );
    private final ResourceLoader resourceLoader;
    private final WfService wfService;

    @Autowired
    public FeedImportTest(
            BcpService bcpService,
            AttachmentsService attachmentsService,
            MboMappingsService mboMappingsService,
            XlsxByMarketTemplateWritingService xlsxByMarketTemplateWritingService,
            ResourceLoader resourceLoader1,
            WfService wfService,
            SupplierTestUtils supplierTestUtils) {
        super(supplierTestUtils, bcpService, attachmentsService, mboMappingsService,
                xlsxByMarketTemplateWritingService);
        this.resourceLoader = resourceLoader1;
        this.wfService = wfService;
    }

    @NotNull
    private static List<ValidationData> getExpectedValidationData(boolean hasOks, boolean hasWarnings,
                                                                  boolean hasErrors) {
        return Stream.of(
                        !hasOks ? null : new ValidationData(ParsedOffer.newBuilder()
                                .setExcelDataLineIndex(1)
                                .setOffer(
                                        Offer.newBuilder()
                                                .setShopSkuId("test1.sku")
                                                .setTitle("test1")
                                                .setShopPrice("20")
                                                .setShopCategoryName("test category")
                                                .setIsRealizing(true)
                                                .setDescription("description")

                                )
                                .setMasterDataInfo(
                                        MasterDataInfo.newBuilder()
                                                .setProviderProductMasterData(
                                                        MasterDataProto.ProviderProductMasterData.newBuilder()
                                                                .setWeightDimensionsInfo(
                                                                        MdmCommon.WeightDimensionsInfo.newBuilder()
                                                                                .setBoxHeightUm(100000)
                                                                                .setBoxWidthUm(100000)
                                                                                .setBoxLengthUm(100000)
                                                                                .setWeightGrossMg(10000)
                                                                )
                                                )
                                )
                                .build(),
                                List.of()),
                        !hasWarnings ? null : new ValidationData(ParsedOffer.newBuilder()
                                .setExcelDataLineIndex(2)
                                .setOffer(
                                        Offer.newBuilder()
                                                .setShopSkuId("test2.sku")
                                                .setTitle("test2")
                                                .setShopPrice("20")
                                                .setShopCategoryName("test category")
                                                .setIsRealizing(true)
                                                .setDescription("description")

                                )
                                .setMasterDataInfo(
                                        MasterDataInfo.newBuilder()
                                                .setProviderProductMasterData(
                                                        MasterDataProto.ProviderProductMasterData.newBuilder()
                                                                .setWeightDimensionsInfo(
                                                                        MdmCommon.WeightDimensionsInfo.newBuilder()
                                                                                .setBoxHeightUm(100000)
                                                                                .setBoxWidthUm(100000)
                                                                                .setBoxLengthUm(100000)
                                                                                .setWeightGrossMg(10000)
                                                                )
                                                )
                                )
                                .build(),
                                List.of(ItemMessage.newBuilder()
                                        .setExcelLineReference(2)
                                        .setEmergency(Status.WARNING)
                                        .setMessage(MbocCommon.Message.newBuilder().setRendered("warning")).build())),
                        !hasErrors ? null : new ValidationData(ParsedOffer.newBuilder()
                                .setExcelDataLineIndex(3)
                                .setOffer(
                                        Offer.newBuilder()
                                                .setShopSkuId("test3.sku")
                                                .setTitle("test3")
                                                .setShopPrice("20")
                                                .setShopCategoryName("test category")
                                                .setIsRealizing(true)
                                                .setDescription("description")

                                )
                                .setMasterDataInfo(
                                        MasterDataInfo.newBuilder()
                                                .setProviderProductMasterData(
                                                        MasterDataProto.ProviderProductMasterData.newBuilder()
                                                                .setWeightDimensionsInfo(
                                                                        MdmCommon.WeightDimensionsInfo.newBuilder()
                                                                                .setBoxHeightUm(100000)
                                                                                .setBoxWidthUm(100000)
                                                                                .setBoxLengthUm(100000)
                                                                                .setWeightGrossMg(10000)
                                                                )
                                                )
                                )
                                .build(),
                                List.of(ItemMessage.newBuilder()
                                        .setExcelLineReference(3)
                                        .setEmergency(Status.ERROR)
                                        .setMessage(MbocCommon.Message.newBuilder().setRendered("error")).build()))
                )
                .filter(Objects::nonNull)
                .toList();
    }

    @BeforeEach
    public void setUp() throws IOException {
        // Do not call real xlsx writing process, because of fonts errors on CI
        doReturn(
                CompletableFuture.completedFuture(FIRST_XLSX_UPLOAD_RESULT),
                CompletableFuture.completedFuture(SECOND_XLSX_UPLOAD_RESULT)
        )
                .when(xlsxByMarketTemplateWritingService)
                .writeValidationDataIntoXlsx(any(), anyIterable(), any());

        // Mock resource loader
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(mock(InputStream.class));
        doReturn(resource).when(resourceLoader).getResource(eq(FEED_URL));

        // Mock validation process:
        // - in progress, when uploaded
        // - in progress while checking upload status
        // - in progress while checking upload status
        // - completed while checking upload status
        Response validationInProgressResponse = Response.newBuilder()
                .setRequestId(MBOC_VERIFY_REQUEST_ID)
                .setProcessingStatus(ProcessingStatus.IN_PROGRESS)
                .build();
        doReturn(validationInProgressResponse)
                .when(mboMappingsService)
                .uploadExcelFile(argThat(x -> x.getMode() == RequestMode.VERIFY));

        doReturn(validationInProgressResponse,
                validationInProgressResponse,
                validationInProgressResponse.toBuilder()
                        .setProcessingStatus(ProcessingStatus.COMPLETED)
                        .build())
                .when(mboMappingsService)
                .getUploadStatus(argThat(x -> x.getRequestId() == MBOC_VERIFY_REQUEST_ID));

        // Mock commit process:
        // - in progress, when uploaded
        // - in progress while checking upload status
        // - completed while checking upload status
        Response commitInProgressResponse = Response.newBuilder()
                .setRequestId(MBOC_COMMIT_REQUEST_ID)
                .setProcessingStatus(ProcessingStatus.IN_PROGRESS)
                .build();
        doReturn(commitInProgressResponse)
                .when(mboMappingsService)
                .uploadExcelFile(argThat(x -> x.getMode() == RequestMode.COMMIT));

        doReturn(commitInProgressResponse,
                commitInProgressResponse.toBuilder()
                        .setProcessingStatus(ProcessingStatus.COMPLETED)
                        .build())
                .when(mboMappingsService)
                .getUploadStatus(argThat(x -> x.getRequestId() == MBOC_COMMIT_REQUEST_ID));

    }

    @ParameterizedTest(name = "[{index}] => {0}")
    @CsvSource(value = {
            "Все офферы имеют ошибки;false;false;true;validationCompletelyFailed",
            "Все офферы имеют предупреждения;false;true;false;validationCompletedWithWarnings",
            "Часть офферов имеет предупреждения, часть имеет ошибки;false;true;true;validationFailed",
            "Все офферы успешно прошли валидацию;true;false;false;mbocUploadResultProcessed",
            "Часть офферов успешно прошла валидацию, часть имеет ошибки;true;false;true;validationFailed",
            "Часть офферов успешно прошла валидацию, часть имеет предупреждения;true;true;false;" +
                    "validationCompletedWithWarnings",
            "Часть офферов успешно прошла валидацию, часть имеет предупреждения, часть - ошибки;true;true;true;" +
                    "validationFailed",
    }, delimiter = ';')
    public void testFeedUpload(String ignored,
                               boolean hasOks, boolean hasWarnings, boolean hasErrors, String feedStatus
    ) {
        List<ValidationData> expectedValidationData = getExpectedValidationData(hasOks, hasWarnings, hasErrors);

        mockDownloadUploadResult(Status.OK, expectedValidationData, () -> any(ResultRequest.class));

        // Then

        var feed = createFeed();

        if (hasOks || hasWarnings) {
            // Записали результат загрузки без ошибок (в случае отсутствия ошибок он и будет файлом без ошибок
            int withoutErrorsCount = Stream.of(hasOks, hasWarnings)
                    .filter(y -> y)
                    .mapToInt(y -> 1)
                    .sum();
            verify(xlsxByMarketTemplateWritingService, times(1))
                    .writeValidationDataIntoXlsx(
                            anyList(),
                            argThat(x -> Iterables.elementsEqual(
                                    expectedValidationData.subList(0, withoutErrorsCount), x
                            )),
                            any(InputStream.class)
                    );
        }

        if (hasErrors) {
            // Записали полный результат валидации вместе с ошибками
            verify(xlsxByMarketTemplateWritingService, times(1))
                    .writeValidationDataIntoXlsx(
                            anyList(),
                            argThat(x -> Iterables.elementsEqual(expectedValidationData, x)),
                            any(InputStream.class)
                    );
        }

        verifyNoMoreInteractions(xlsxByMarketTemplateWritingService);

        verify(mboMappingsService, times(1))
                .downloadUploadResult(argThat(x -> x.getRequestId() == MBOC_VERIFY_REQUEST_ID));

        if (!hasWarnings && !hasErrors) {
            verify(mboMappingsService, times(1))
                    .downloadUploadResult(argThat(x -> x.getRequestId() == MBOC_COMMIT_REQUEST_ID));
        }

        Assertions.assertEquals(feedStatus, feed.getStatus());
    }

    @ParameterizedTest(name = "[{index}] => {0}")
    @CsvSource(value = {
            "Все офферы имеют предупреждения;false;true;false",
            "Часть офферов имеет предупреждения, часть имеет ошибки;false;true;true",
            "Часть офферов успешно прошла валидацию, часть имеет ошибки;true;false;true",
            "Часть офферов успешно прошла валидацию, часть имеет предупреждения;true;true;false",
            "Часть офферов успешно прошла валидацию, часть имеет предупреждения, часть - ошибки;true;true;true",
    }, delimiter = ';')
    public void testManualConfirmation(String ignored,
                                       boolean hasOks, boolean hasWarnings, boolean hasErrors
    ) {
        List<ValidationData> expectedValidationData = getExpectedValidationData(hasOks, hasWarnings, hasErrors);

        mockDownloadUploadResult(Status.OK, expectedValidationData, () -> any(ResultRequest.class));

        // Then

        var feed = createFeed();

        Assertions.assertTrue(
                wfService.hasTransitionTo(feed, Feed.Statuses.MANUALLY_CONFIRMED),
                () -> "Нет перехода из статуса %s в статус %s".formatted(
                        feed.getStatus(), Feed.Statuses.MANUALLY_CONFIRMED
                )
        );

        if (!hasOks && !hasWarnings && hasErrors) {
            Assertions.assertThrows(ValidationException.class, () -> bcpService.edit(feed, Map.of(
                    Feed.STATUS, Feed.Statuses.MANUALLY_CONFIRMED
            )));

            return;
        }

        bcpService.edit(feed, Map.of(
                Feed.STATUS, Feed.Statuses.MANUALLY_CONFIRMED
        ));

        verify(mboMappingsService, times(1))
                .uploadExcelFile(argThat(r -> r.getExcelFileUrl().equals(FIRST_XLSX_UPLOAD_RESULT.getUrl())));
        Assertions.assertEquals(Feed.Statuses.MBOC_UPLOAD_RESULT_PROCESSED, feed.getStatus());
    }

    /**
     * Все офферы успешно прошли валидацию, но загрузка сломалась
     */
    @Test
    public void testValidationSuccessfulButCommitWasFailed() {

        var expectedValidationData = getExpectedValidationData(true, false, false);

        mockDownloadUploadResult(Status.OK, expectedValidationData,
                () -> argThat(x -> x.getRequestId() == MBOC_VERIFY_REQUEST_ID));

        mockDownloadUploadResult(Status.ERROR, expectedValidationData,
                () -> argThat(x -> x.getRequestId() == MBOC_COMMIT_REQUEST_ID));

        // Then

        var feed = createFeed();

        // Записали результат загрузки без ошибок
        verify(xlsxByMarketTemplateWritingService, times(1))
                .writeValidationDataIntoXlsx(
                        anyList(),
                        argThat(x -> Iterables.elementsEqual(expectedValidationData, x)),
                        any(InputStream.class)
                );

        verifyNoMoreInteractions(xlsxByMarketTemplateWritingService);

        verify(mboMappingsService, times(1))
                .downloadUploadResult(argThat(x -> x.getRequestId() == MBOC_VERIFY_REQUEST_ID));

        verify(mboMappingsService, times(1))
                .downloadUploadResult(argThat(x -> x.getRequestId() == MBOC_COMMIT_REQUEST_ID));

        Assertions.assertEquals(Feed.Statuses.FAILED_TO_UPLOAD_TO_MBOC, feed.getStatus());
    }

    /**
     * Процесс валидации в целом вернул ошибки (аналог 500 http кода)
     */
    @Test
    public void testWholeValidationError() {


        mockDownloadUploadResult(Status.ERROR, List.of(), () -> any(ResultRequest.class));

        // Then

        var feed = createFeed();

        verifyNoMoreInteractions(xlsxByMarketTemplateWritingService);

        verify(mboMappingsService, times(1))
                .downloadUploadResult(argThat(x -> x.getRequestId() == MBOC_VERIFY_REQUEST_ID));

        Assertions.assertEquals(Feed.Statuses.VALIDATION_CRASHED, feed.getStatus());
    }

    private void mockDownloadUploadResult(
            Status status,
            List<ValidationData> expectedValidationData,
            Supplier<ResultRequest> matcher) {
        doReturn(Result.newBuilder()
                .setStatus(status)
                .addAllParsedOffers(
                        status == Status.ERROR
                                ? List.of()
                                : Iterables.transform(expectedValidationData, ValidationData::parsedOffer))
                .addAllItemMessages(
                        status == Status.ERROR
                                ? List.of()
                                : expectedValidationData.stream()
                                .map(ValidationData::itemMessage)
                                .flatMap(Collection::stream)::iterator)
                .build())
                .when(mboMappingsService)
                .downloadUploadResult(matcher.get());
    }
}
