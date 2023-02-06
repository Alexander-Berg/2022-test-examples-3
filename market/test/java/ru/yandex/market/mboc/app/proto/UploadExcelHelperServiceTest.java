package ru.yandex.market.mboc.app.proto;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.SoftAssertions;
import org.gaul.s3proxy.AuthenticationType;
import org.gaul.s3proxy.junit.S3ProxyRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.Magics;
import ru.yandex.market.mboc.app.offers.ImportExcelService;
import ru.yandex.market.mboc.app.offers.ImportFileService;
import ru.yandex.market.mboc.app.offers.ImportYmlService;
import ru.yandex.market.mboc.app.offers.OfferProtoConverter;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionServiceImpl;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnitsToProtoConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.ImportedOfferToMasterDataConverter.MasterDataConvertResult;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.converter.ErrorAtLine;
import ru.yandex.market.mboc.common.services.converter.validation.IsTemplateFileExcelValidation;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappings.OfferExcelUpload;
import ru.yandex.market.protobuf.readers.VarIntDelimerMessageReader;
import ru.yandex.market.protobuf.tools.MagicChecker;
import ru.yandex.market.protobuf.tools.MessageIterator;
import ru.yandex.misc.thread.ThreadUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.mboc.common.utils.MbocConstants.PROTO_API_USER;

/**
 * @author yuramalinov
 * @created 07.09.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class UploadExcelHelperServiceTest {

    private static final String TEST_BUCKET = "test-bucket";
    @SuppressWarnings("UnstableApiUsage")
    @Rule
    public S3ProxyRule s3Proxy = S3ProxyRule.builder()
        .withCredentials(AuthenticationType.NONE, "access", "secret")
        .build();
    private BackgroundActionServiceImpl backgroundActionService;
    private UploadExcelHelperService uploadExcelHelperService;
    private AmazonS3 s3Client;
    private ImportFileService importFileService;
    private ImportExcelService importExcelService;
    private ImportYmlService importYmlService;
    private SupplierRepositoryMock supplierRepository;
    private IsTemplateFileExcelValidation isTemplateFileExcelValidation;
    private MskuRepository mskuRepository;

    private static List<MasterDataConvertResult> blankConvertResultList(int n) {
        return IntStream.range(0, n)
            .mapToObj(x -> new MasterDataConvertResult(
                x,
                new MasterData(),
                Collections.emptyList()
            ))
            .collect(Collectors.toList());
    }

    @Before
    public void setUp() {
        // Weird issue happens in jclouds library on Windows and it fails to set permissions for local s3 instance dir.
        assumeTrue(!SystemUtils.IS_OS_WINDOWS);

        BackgroundActionRepositoryMock actionRepositoryMock = new BackgroundActionRepositoryMock();
        supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test"));
        supplierRepository.insert(new Supplier(OfferTestUtils.FMCG_SUPPLIER_ID, "fmcg-test", "some-domain.ru",
            "some-organization", MbocSupplierType.FMCG)
        );

        backgroundActionService = new BackgroundActionServiceImpl(actionRepositoryMock, new TransactionTemplateMock(),
            1);
        backgroundActionService.init();

        s3Client = AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(s3Proxy.getAccessKey(), s3Proxy.getSecretKey())))
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                s3Proxy.getUri().toString(), Regions.US_EAST_1.getName()))
            .build();

        s3Client.createBucket(TEST_BUCKET);

        isTemplateFileExcelValidation = new IsTemplateFileExcelValidation();

        importExcelService = Mockito.mock(ImportExcelService.class);
        importYmlService = Mockito.mock(ImportYmlService.class);
        importFileService = Mockito.spy(new ImportFileService(importExcelService, importYmlService));

        mskuRepository = Mockito.mock(MskuRepository.class);

        Mockito.when(importFileService.isYmlFile(Mockito.anyString(), Mockito.any())).thenReturn(false);
        uploadExcelHelperService = new UploadExcelHelperService(backgroundActionService,
            importFileService, HttpClientBuilder.create().build(), actionRepositoryMock,
            new OfferProtoConverter(new CategoryCachingServiceMock(),
                Mockito.mock(OfferCategoryRestrictionCalculator.class), null, -1),
            supplierRepository, mskuRepository, s3Client, isTemplateFileExcelValidation, TEST_BUCKET);
        uploadExcelHelperService.setUploadRetryPause(0);
    }

    @Test
    public void testUploadExcelHelperService() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        ErrorAtLine err1 = new ErrorAtLine(1, MbocErrors.get().excelValueIsRequired("one"));
        ErrorAtLine err2 = new ErrorAtLine(1, MbocErrors.get().excelUnhandledError("two"));
        MasterData masterData = new MasterData();
        masterData.setLifeTime(136, TimeInUnits.TimeUnit.DAY);

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(call ->
                new ImportFileService.ImportResult(1,
                    Collections.singletonList(
                        new ErrorAtLine(1, MbocErrors.get().protoUnknownError("Some kind of error"))),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    Collections.singletonList(
                        new MasterDataConvertResult(
                            0,
                            masterData,
                            Arrays.asList(err1, err2)
                        )
                    )
                ));

        long requestId = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setExcelFileUrl(url.toString())
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .build())
            .getRequestId();

        backgroundActionService.stop();
        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(requestId)
                .build());
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
            softAssertions.assertThat(result.getParsedOffersList()).hasSize(1);

            OfferExcelUpload.ParsedOffer parsedOffers = result.getParsedOffers(0);
            softAssertions.assertThat(parsedOffers.getOffer().getSupplierId())
                .isEqualTo(OfferTestUtils.TEST_SUPPLIER_ID);
            softAssertions.assertThat(parsedOffers.getMasterDataInfo().getLifeTimeWithUnits())
                .isEqualTo(TimeInUnitsToProtoConverter.toProto(new TimeInUnits(136)));
        });
    }

    @Test
    public void testUploadForFMCGSupplier() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(call ->
                new ImportFileService.ImportResult(
                    1,
                    Collections.emptyList(),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0,
                            OfferTestUtils.simpleOffer().setBusinessId(OfferTestUtils.FMCG_SUPPLIER_ID),
                            true)),
                    Collections.emptyList()
                )
            );

        long requestId = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setExcelFileUrl(url.toString())
                .setSupplierId(OfferTestUtils.FMCG_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .build())
            .getRequestId();

        backgroundActionService.stop();
        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(requestId)
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(result.getParsedOffersList()).hasSize(1);
        OfferExcelUpload.ParsedOffer parsedOffers = result.getParsedOffers(0);
        assertThat(parsedOffers.getOffer().getSupplierId()).isEqualTo(OfferTestUtils.FMCG_SUPPLIER_ID);
    }

    @Test
    public void testSuccessAndErrors() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);
        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(call -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                return new ImportFileService.ImportResult(1,
                    Collections.singletonList(
                        new ErrorAtLine(1, MbocErrors.get().protoUnknownError("Some kind of error"))),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    Collections.singletonList(
                        new MasterDataConvertResult(
                            0,
                            new MasterData(),
                            Collections.emptyList())
                    )
                );
            });

        long requestId = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setExcelFileUrl(url.toString())
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .build())
            .getRequestId();

        assertThat(
            uploadExcelHelperService.getUploadStatus(
                OfferExcelUpload.ProcessingRequest.newBuilder()
                    .setRequestId(requestId)
                    .build())
                .getProcessingStatus()
        ).isEqualTo(OfferExcelUpload.ProcessingStatus.IN_PROGRESS);

        youShallNotPass.set(false);
        backgroundActionService.stop();

        assertThat(uploadExcelHelperService.getUploadStatus(
            OfferExcelUpload.ProcessingRequest.newBuilder()
                .setRequestId(requestId)
                .build())
            .getProcessingStatus()
        )
            .isEqualTo(OfferExcelUpload.ProcessingStatus.COMPLETED);

        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(requestId)
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(result.getParsedOffersList()).hasSize(1);

        assertThat(result.getItemMessagesList()).hasSize(1);
        assertThat(result.getItemMessages(0).getMessage().getMessageCode()).contains("unknown");

        ObjectListing objects = s3Client.listObjects(TEST_BUCKET, UploadExcelHelperService.RESPONSE_PREFIX);
        assertThat(objects.getObjectSummaries()).hasSize(1);

        // Check save policy
        Mockito.verify(importExcelService).importExcel(eq(OfferTestUtils.TEST_SUPPLIER_ID), eq("some.xls"),
            Mockito.any(), Mockito.any(),
            eq(PROTO_API_USER), eq(new ImportFileService.ImportSettings(ImportFileService.SavePolicy.DRY_RUN,
                        UploadExcelHelperService.REQUEST_SOURCE)), Mockito.any());
    }

    @Test
    public void testSuccessAndErrorsYmlFile() throws InterruptedException {
        Mockito.when(importFileService.isYmlFile(Mockito.anyString(), Mockito.any())).thenReturn(true);
        s3Client.putObject(TEST_BUCKET, "in/some.yml", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.yml");

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);
        MasterData masterData = new MasterData();
        masterData.setManufacturerCountries(Collections.singletonList("Россия"));
        Mockito.when(importYmlService.importFile(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any()))
            .then(call -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                return new ImportFileService.ImportResult(1,
                    Collections.singletonList(
                        new ErrorAtLine(1, MbocErrors.get().protoUnknownError("Some kind of error"))),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    Collections.singletonList(
                        new MasterDataConvertResult(
                            0,
                            masterData,
                            Collections.emptyList())
                    )
                );
            });

        long requestId = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setExcelFileUrl(url.toString())
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .build())
            .getRequestId();

        assertThat(
            uploadExcelHelperService.getUploadStatus(
                OfferExcelUpload.ProcessingRequest.newBuilder()
                    .setRequestId(requestId)
                    .build())
                .getProcessingStatus()
        ).isEqualTo(OfferExcelUpload.ProcessingStatus.IN_PROGRESS);

        youShallNotPass.set(false);
        backgroundActionService.stop();

        assertThat(uploadExcelHelperService.getUploadStatus(
            OfferExcelUpload.ProcessingRequest.newBuilder()
                .setRequestId(requestId)
                .build())
            .getProcessingStatus()
        )
            .isEqualTo(OfferExcelUpload.ProcessingStatus.COMPLETED);

        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(requestId)
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(result.getParsedOffersList()).hasSize(1);
        assertThat(result.getParsedOffersList().get(0).getOffer()
            .getMasterDataInfo().hasProviderProductMasterData()).isTrue();

        assertThat(result.getItemMessagesList()).hasSize(1);
        assertThat(result.getItemMessages(0).getMessage().getMessageCode()).contains("unknown");

        ObjectListing objects = s3Client.listObjects(TEST_BUCKET, UploadExcelHelperService.RESPONSE_PREFIX);
        assertThat(objects.getObjectSummaries()).hasSize(1);

        // Check save policy
        Mockito.verify(importYmlService).importFile(eq(OfferTestUtils.TEST_SUPPLIER_ID), eq("some.yml"),
            Mockito.any(), Mockito.any(),
            eq(PROTO_API_USER), eq(new ImportFileService.ImportSettings(ImportFileService.SavePolicy.DRY_RUN,
                        UploadExcelHelperService.REQUEST_SOURCE)));
    }

    @Test
    public void testCommitIsPassedCorrectly() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(new ImportFileService.ImportResult(1,
                Collections.singletonList(
                    new ErrorAtLine(1, MbocErrors.get().protoUnknownError("Some kind of error"))),
                Collections.singletonList(
                    new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                        true)
                ),
                Collections.singletonList(
                    new MasterDataConvertResult(
                        0,
                        new MasterData(),
                        Collections.emptyList()
                    )
                )
            ));

        OfferExcelUpload.Response response = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setExcelFileUrl(url.toString())
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.COMMIT)
                .build());

        backgroundActionService.stop();

        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(response.getRequestId())
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(result.getParsedOffersList()).hasSize(1);
        assertThat(result.getItemMessagesList()).hasSize(1);

        // Check save policy
        Mockito.verify(importExcelService).importExcel(eq(OfferTestUtils.TEST_SUPPLIER_ID), eq("some.xls"),
            Mockito.any(), Mockito.any(), eq(PROTO_API_USER),
            eq(new ImportFileService.ImportSettings(ImportFileService.SavePolicy.PARTIAL_BATCH,
                    UploadExcelHelperService.REQUEST_SOURCE)), Mockito.any()); // Check it
    }

    @Test
    public void testRetryException() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);
        AtomicBoolean throwException = new AtomicBoolean(true);

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(call -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                if (throwException.get()) {
                    throwException.set(false);
                    throw new RuntimeException();
                }
                return new ImportFileService.ImportResult(1,
                    Collections.emptyList(),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    Collections.singletonList(
                        new MasterDataConvertResult(
                            0,
                            new MasterData(),
                            Collections.emptyList())
                    )
                );
            });

        long requestId = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setExcelFileUrl(url.toString())
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .build())
            .getRequestId();

        assertThat(
            uploadExcelHelperService.getUploadStatus(
                OfferExcelUpload.ProcessingRequest.newBuilder()
                    .setRequestId(requestId)
                    .build())
                .getProcessingStatus()
        ).isEqualTo(OfferExcelUpload.ProcessingStatus.IN_PROGRESS);

        youShallNotPass.set(false);

        backgroundActionService.stop();

        assertThat(uploadExcelHelperService.getUploadStatus(
            OfferExcelUpload.ProcessingRequest.newBuilder()
                .setRequestId(requestId)
                .build())
            .getProcessingStatus()
        )
            .isEqualTo(OfferExcelUpload.ProcessingStatus.COMPLETED);

        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(requestId)
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(result.getParsedOffersList()).hasSize(1);

        assertThat(result.getItemMessagesList()).hasSize(0);

        ObjectListing objects = s3Client.listObjects(TEST_BUCKET, UploadExcelHelperService.RESPONSE_PREFIX);
        assertThat(objects.getObjectSummaries()).hasSize(1);

        // Check importExcel called two times because of retry
        // Check save policy
        Mockito.verify(importExcelService, Mockito.times(2))
            .importExcel(eq(OfferTestUtils.TEST_SUPPLIER_ID), eq("some.xls"),
                Mockito.any(), Mockito.any(),
                eq(PROTO_API_USER), eq(new ImportFileService.ImportSettings(ImportFileService.SavePolicy.DRY_RUN,
                            UploadExcelHelperService.REQUEST_SOURCE)), Mockito.any()
            );
    }

    @Test
    public void testUnknownException() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        // This will cause unexpected exception as no output for supplier can be produced
        supplierRepository.deleteAll();

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(invocation -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                return new ImportFileService.ImportResult(
                    1,
                    Collections.emptyList(),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    blankConvertResultList(0)
                );
            });

        OfferExcelUpload.Response response = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .setExcelFileUrl(url.toString())
                .build());

        assertThat(response.getProcessingStatus()).isEqualTo(OfferExcelUpload.ProcessingStatus.IN_PROGRESS);

        OfferExcelUpload.Response uploadStatus = uploadExcelHelperService.getUploadStatus(
            OfferExcelUpload.ProcessingRequest.newBuilder()
                .setRequestId(response.getRequestId())
                .build());

        assertThat(uploadStatus.getProcessingStatus())
            .isEqualTo(OfferExcelUpload.ProcessingStatus.IN_PROGRESS);

        youShallNotPass.set(false);
        backgroundActionService.stop();

        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(response.getRequestId())
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.ERROR);
        assertThat(result.hasStatusMessage()).isTrue();
        assertThat(result.getStatusMessage().getMessageCode()).contains("unknown");
        assertThat(result.getStatusMessage().getMustacheTemplate())
            .isEqualTo("Произошла ошибка: {{message}}");
    }

    @Test
    public void testFirstGlobalErrorIsWrittenToStatusMessage() throws InterruptedException {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(invocation -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                return new ImportFileService.ImportResult(
                    1,
                    Arrays.asList(
                        ErrorAtLine.globalError(MbocErrors.get().protoUnknownError("GLOBAL1")),
                        ErrorAtLine.globalError(MbocErrors.get().protoUnknownError("GLOBAL2"))
                    ),
                    Collections.emptyList(),
                    Collections.emptyList()
                );
            });

        OfferExcelUpload.Response response = uploadExcelHelperService.uploadExcelFile(
            OfferExcelUpload.Request.newBuilder()
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .setExcelFileUrl(url.toString())
                .build());

        youShallNotPass.set(false);
        backgroundActionService.stop();

        OfferExcelUpload.Result result = uploadExcelHelperService.downloadUploadResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(response.getRequestId())
                .build());

        assertThat(result.getStatus()).isEqualTo(OfferExcelUpload.Status.ERROR);
        assertThat(result.hasStatusMessage()).isTrue();
        assertThat(result.getStatusMessage().getJsonDataForMustacheTemplate()).contains("GLOBAL1");

        // Second is missed
        assertThat(result.getStatusMessage().getJsonDataForMustacheTemplate()).doesNotContain("GLOBAL2");
        assertThat(result.getStatusMessage().getMustacheTemplate())
            .isEqualTo("Произошла ошибка: {{message}}");

        // All are in output
        assertThat(result.getItemMessagesCount()).isEqualTo(2);
    }

    @Test
    public void testStat() {
        OfferExcelUpload.UploadStatistics statistics = uploadExcelHelperService.getOffersStatistics(
            new ImportFileService.ImportResult(
                1,
                Arrays.asList(
                    new ErrorAtLine(1, MbocErrors.get().protoUnknownError("Some kind of error")),
                    new ErrorAtLine(2, MbocErrors.get().protoUnknownError("Some kind of error"))),
                Arrays.asList(
                    new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                        true),
                    new ImportFileService.OfferResult(1, OfferTestUtils.simpleOffer(),
                        true),
                    new ImportFileService.OfferResult(2, OfferTestUtils.simpleOffer(),
                        true),
                    new ImportFileService.OfferResult(3, OfferTestUtils.simpleOffer(),
                        false),
                    new ImportFileService.OfferResult(4, OfferTestUtils.simpleOffer()
                        .updateApprovedSkuMapping(OfferTestUtils.mapping(42),
                            Offer.MappingConfidence.CONTENT),
                        false),
                    new ImportFileService.OfferResult(5, OfferTestUtils.simpleOffer()
                        .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH) // This one is still approved
                        .updateApprovedSkuMapping(OfferTestUtils.mapping(42),
                            Offer.MappingConfidence.CONTENT),
                        false),
                    new ImportFileService.OfferResult(6,
                        OfferTestUtils.simpleOffer().updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH),
                        false)
                ),
                blankConvertResultList(7)
            )
        );

        assertEquals(2, statistics.getErrorsSkipped());
        assertEquals(1, statistics.getNewOffers());
        assertEquals(7, statistics.getTotal());

        MboMappings.OfferStatusStatistics existingOffers = statistics.getExistingOffers();
        assertEquals(2, existingOffers.getApproved());
        assertEquals(1, existingOffers.getInWork());
        assertEquals(1, existingOffers.getRejected());
    }

    @Test
    public void testWarningsAreIgnoredNow() {
        OfferExcelUpload.UploadStatistics statistics = uploadExcelHelperService.getOffersStatistics(
            new ImportFileService.ImportResult(
                1,
                Arrays.asList(
                    new ErrorAtLine(1, MbocErrors.get().protoUnknownError("Some kind of error")),
                    new ErrorAtLine(2, MbocErrors.get().protoUnknownError("Some kind of warning")
                        .copyWithLevel(ErrorInfo.Level.WARNING)),
                    new ErrorAtLine(3, MbocErrors.get().protoUnknownError("Some kind of info")
                        .copyWithLevel(ErrorInfo.Level.INFO))),
                Arrays.asList(
                    new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                        true),
                    new ImportFileService.OfferResult(1, OfferTestUtils.simpleOffer(),
                        true),
                    new ImportFileService.OfferResult(2, OfferTestUtils.simpleOffer(),
                        true),
                    new ImportFileService.OfferResult(3, OfferTestUtils.simpleOffer(),
                        false)
                ),
                blankConvertResultList(4)
            )
        );

        assertEquals(1, statistics.getErrorsSkipped());
        assertEquals(2, statistics.getNewOffers());
        assertEquals(4, statistics.getTotal());

        MboMappings.OfferStatusStatistics existingOffers = statistics.getExistingOffers();
        assertEquals(0, existingOffers.getApproved());
        assertEquals(1, existingOffers.getInWork());
        assertEquals(0, existingOffers.getRejected());
    }

    @Test
    public void testStreamedUploadHasOfferAndMessage() throws Exception {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(invocation -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                return new ImportFileService.ImportResult(
                    1,
                    Collections.singletonList(
                        new ErrorAtLine(0, MbocErrors.get().protoUnknownError("ERROR1"))
                    ),
                    Collections.singletonList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    Collections.singletonList(
                        new MasterDataConvertResult(
                            0,
                            new MasterData(),
                            Collections.emptyList())
                    )
                );
            });

        OfferExcelUpload.Response response = uploadExcelHelperService.uploadExcelFileStreamed(
            OfferExcelUpload.Request.newBuilder()
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .setExcelFileUrl(url.toString())
                .build());

        youShallNotPass.set(false);
        backgroundActionService.stop();

        OfferExcelUpload.StreamedResult streamedResult = uploadExcelHelperService.downloadUploadStreamedResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(response.getRequestId())
                .build());


        URL totalUrl = new URL(streamedResult.getTotalFileLink());
        URL detailUrl = new URL(streamedResult.getDetailFileLinks());

        OfferExcelUpload.TotalResult totalResult = OfferExcelUpload.TotalResult.newBuilder()
            .mergeFrom(totalUrl.openStream()).build();

        InputStream detailStream = detailUrl.openStream();

        MagicChecker.checkMagic(detailStream, Magics.MagicConstants.MPOM.toString());

        MessageIterator<OfferExcelUpload.ParsedOfferWithMessages> messageIterator = new MessageIterator<>(
            new VarIntDelimerMessageReader<>(OfferExcelUpload.ParsedOfferWithMessages.PARSER, detailStream)
        );

        assertThat(totalResult.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(totalResult.getUploadStatistics().getTotal()).isOne();
        assertThat(totalResult.getUploadStatistics().getErrorsSkipped()).isOne();


        assertThat(messageIterator).toIterable().first().satisfies(parsedOfferWithMessages -> {
            assertThat(parsedOfferWithMessages.getExcelDataLineIndex()).isZero();
            assertThat(parsedOfferWithMessages.getOffer().getEffectiveSupplierId())
                .isEqualTo(OfferTestUtils.TEST_SUPPLIER_ID);
            assertThat(parsedOfferWithMessages.getOffer().getShopSkuId())
                .isEqualTo(OfferTestUtils.DEFAULT_SHOP_SKU);
            assertThat(parsedOfferWithMessages.getItemMessagesCount()).isOne();
            assertThat(parsedOfferWithMessages.getItemMessages(0).getEmergency())
                .isEqualTo(OfferExcelUpload.Status.ERROR);
            assertThat(parsedOfferWithMessages.getItemMessages(0).getMessage().getMessageCode())
                .isEqualTo("mboc.error.proto-unknown-error");
        });

        assertThat(messageIterator).isExhausted();
    }

    @Test
    public void testStreamedUploadHasManyOffersAndMessages() throws Exception {
        s3Client.putObject(TEST_BUCKET, "in/some.xls", "test-content");
        URL url = s3Client.getUrl(TEST_BUCKET, "in/some.xls");

        AtomicBoolean youShallNotPass = new AtomicBoolean(true);

        Mockito.when(importExcelService.importExcel(Mockito.anyInt(), Mockito.anyString(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .then(invocation -> {
                while (youShallNotPass.get()) {
                    ThreadUtils.sleep(10);
                }
                return new ImportFileService.ImportResult(
                    1,
                    Collections.singletonList(
                        new ErrorAtLine(0, MbocErrors.get().protoUnknownError("ERROR1"))
                    ),
                    Arrays.asList(
                        new ImportFileService.OfferResult(0, OfferTestUtils.simpleOffer(),
                            true),
                        new ImportFileService.OfferResult(1, OfferTestUtils.simpleOffer(),
                            true)
                    ),
                    blankConvertResultList(2)
                );
            });

        OfferExcelUpload.Response response = uploadExcelHelperService.uploadExcelFileStreamed(
            OfferExcelUpload.Request.newBuilder()
                .setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID)
                .setMode(OfferExcelUpload.RequestMode.VERIFY)
                .setExcelFileUrl(url.toString())
                .build());

        youShallNotPass.set(false);
        backgroundActionService.stop();

        OfferExcelUpload.StreamedResult streamedResult = uploadExcelHelperService.downloadUploadStreamedResult(
            OfferExcelUpload.ResultRequest.newBuilder()
                .setRequestId(response.getRequestId())
                .build());


        URL totalUrl = new URL(streamedResult.getTotalFileLink());
        URL detailUrl = new URL(streamedResult.getDetailFileLinks());

        OfferExcelUpload.TotalResult totalResult = OfferExcelUpload.TotalResult.newBuilder()
            .mergeFrom(totalUrl.openStream()).build();

        InputStream detailStream = detailUrl.openStream();

        MagicChecker.checkMagic(detailStream, Magics.MagicConstants.MPOM.toString());

        MessageIterator<OfferExcelUpload.ParsedOfferWithMessages> messageIterator = new MessageIterator<>(
            new VarIntDelimerMessageReader<>(OfferExcelUpload.ParsedOfferWithMessages.PARSER, detailStream)
        );


        assertThat(totalResult.getStatus()).isEqualTo(OfferExcelUpload.Status.OK);
        assertThat(totalResult.getUploadStatistics().getTotal()).isEqualTo(2);
        assertThat(totalResult.getUploadStatistics().getErrorsSkipped()).isOne();

        List<OfferExcelUpload.ParsedOfferWithMessages> parsedOfferWithMessagesList = Lists
            .newArrayList(messageIterator);

        assertThat(parsedOfferWithMessagesList).hasSize(2);

        assertThat(parsedOfferWithMessagesList).element(0).satisfies(parsedOfferWithMessages -> {
            assertThat(parsedOfferWithMessages.getExcelDataLineIndex()).isEqualTo(0);
            assertThat(parsedOfferWithMessages.getOffer().getEffectiveSupplierId())
                .isEqualTo(OfferTestUtils.TEST_SUPPLIER_ID);
            assertThat(parsedOfferWithMessages.getOffer().getShopSkuId())
                .isEqualTo(OfferTestUtils.DEFAULT_SHOP_SKU);
            assertThat(parsedOfferWithMessages.getItemMessagesCount()).isOne();
            assertThat(parsedOfferWithMessages.getItemMessages(0).getEmergency())
                .isEqualTo(OfferExcelUpload.Status.ERROR);
            assertThat(parsedOfferWithMessages.getItemMessages(0).getMessage().getMessageCode())
                .isEqualTo("mboc.error.proto-unknown-error");
        });

        assertThat(parsedOfferWithMessagesList).element(1).satisfies(parsedOfferWithMessages -> {
            assertThat(parsedOfferWithMessages.getExcelDataLineIndex()).isEqualTo(1);
            assertThat(parsedOfferWithMessages.getOffer().getEffectiveSupplierId())
                .isEqualTo(OfferTestUtils.TEST_SUPPLIER_ID);
            assertThat(parsedOfferWithMessages.getOffer().getShopSkuId())
                .isEqualTo(OfferTestUtils.DEFAULT_SHOP_SKU);
            assertThat(parsedOfferWithMessages.getItemMessagesCount()).isZero();
        });

    }
}
