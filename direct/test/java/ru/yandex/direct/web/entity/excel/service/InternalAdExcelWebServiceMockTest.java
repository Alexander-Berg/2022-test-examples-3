package ru.yandex.direct.web.entity.excel.service;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.image.service.ImageService;
import ru.yandex.direct.core.entity.internalads.service.TemplateResourceService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.excel.processing.model.internalad.ExcelFetchedData;
import ru.yandex.direct.excel.processing.model.internalad.ExcelImportResult;
import ru.yandex.direct.excel.processing.model.internalad.InternalAdGroupRepresentation;
import ru.yandex.direct.excel.processing.model.internalad.InternalBannerRepresentation;
import ru.yandex.direct.excel.processing.service.internalad.InternalAdExcelImportService;
import ru.yandex.direct.excel.processing.service.internalad.InternalAdExcelService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.entity.excel.model.UploadedImageInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.mds.MdsHosts;
import ru.yandex.inside.mds.MdsNamespace;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.ip.HostPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.createSheetFetchedData;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.getDefaultInternalAdGroupRepresentation;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.getDefaultInternalBannerRepresentation;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.web.entity.excel.ExcelTestData.getDefaultInternalAdImportRequest;
import static ru.yandex.direct.web.entity.excel.service.ExcelWebConverter.toMdsFileKey;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdExcelWebServiceMockTest {

    @Mock
    private InternalAdExcelService internalAdExcelService;

    @Mock
    private InternalAdExcelImportService internalAdExcelImportService;

    @Mock
    private MdsHolder mdsHolder;

    @Mock
    private TemplateResourceService templateResourceService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private ImageService imageService;

    @Mock
    private UserService userService;

    @Spy
    @InjectMocks
    private InternalAdExcelWebService service;

    @Mock
    private InputStreamSource inputStreamSource;

    private Long operatorUid;
    private UidAndClientId owner;
    private InternalAdImportRequest request;
    private ExcelFetchedData excelFetchedData;
    private String url;
    private InternalAdGroupRepresentation adGroupRepresentation;
    private InternalBannerRepresentation bannerRepresentation;

    @Before
    public void initTestData() {
        doReturn(inputStreamSource)
                .when(mdsHolder).download(any());
        doReturn(true)
                .when(inputStreamSource).exists();

        operatorUid = RandomNumberUtils.nextPositiveLong();
        owner = UidAndClientId.of(RandomNumberUtils.nextPositiveLong(),
                ClientId.fromLong(RandomNumberUtils.nextPositiveLong()));
        request = getDefaultInternalAdImportRequest();

        adGroupRepresentation = getDefaultInternalAdGroupRepresentation();
        adGroupRepresentation.getAdGroup().setId(RandomNumberUtils.nextPositiveLong());
        var adGroupsSheet = createSheetFetchedData(List.of(adGroupRepresentation));

        bannerRepresentation = getDefaultInternalBannerRepresentation(adGroupRepresentation.getAdGroup())
                .setAdGroupName(adGroupRepresentation.getAdGroup().getName());
        var adsSheet = createSheetFetchedData(List.of(bannerRepresentation));
        excelFetchedData = ExcelFetchedData.create(adGroupsSheet, List.of(adsSheet));
        doReturn(Result.successful(excelFetchedData))
                .when(internalAdExcelService).getAndValidateDataFromExcelFile(eq(owner.getClientId()), any(), any());

        ExcelImportResult result = new ExcelImportResult()
                .withAdGroupsResult(MassResult.emptyMassAction())
                .withAdsResult(MassResult.emptyMassAction());
        doReturn(result)
                .when(internalAdExcelImportService)
                .importFromExcel(eq(operatorUid), eq(owner), anyBoolean(), any(), eq(excelFetchedData));

        url = RandomStringUtils.randomAlphanumeric(15);
        doReturn(url)
                .when(service).uploadIfNeedExcelFileAndGetUrl(anyBoolean(), eq(mdsHolder),
                any(MdsFileKey.class), eq(owner.getClientId()), eq(excelFetchedData), eq(false));
    }


    @Test
    public void checkImportInternalResult() {
        Result<ExcelImportResult> result = service.importInternal(operatorUid, owner, request);

        var expectedResult = new ExcelImportResult()
                .withExcelFileUrl(url)
                .withAdGroupsResult(MassResult.emptyMassAction())
                .withAdsResult(MassResult.emptyMassAction());
        assertThat(result.getResult())
                .is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void checkImportInternal_WhenGetBrokenResultOfDataFromExcelFile() {
        var brokenResult = Result.broken(
                ValidationResult.failed(RandomNumberUtils.nextPositiveLong(), CommonDefects.invalidValue()));
        doReturn(brokenResult)
                .when(internalAdExcelService).getAndValidateDataFromExcelFile(eq(owner.getClientId()), any(), any());

        Result<ExcelImportResult> result = service.importInternal(operatorUid, owner, request);

        assertThat(result)
                .is(matchedBy(beanDiffer(brokenResult)));
        verifyZeroInteractions(internalAdExcelImportService);
    }

    @Test
    public void checkImportInternal_NeedUploadFile_WhenHasNewUploadedImages() {
        request.withOnlyValidation(true)
                .withAdsImages(List.of(new UploadedImageInfo()
                        .withFileName("somePic.jpg")
                        .withImageHash(RandomStringUtils.randomAlphanumeric(11))));

        service.importInternal(operatorUid, owner, request);

        verify(service)
                .enrichUploadedImageHashes(eq(owner.getClientId()), eq(excelFetchedData.getAdsSheets()),
                        eq(request.getAdsImages()));
        verify(service)
                .uploadIfNeedExcelFileAndGetUrl(eq(true), eq(mdsHolder),
                        any(MdsFileKey.class), eq(owner.getClientId()), eq(excelFetchedData), eq(false));
    }

    @Test
    public void checkImportInternal_NotEnrichUploadedImageHashes_WhenAdsImagesFromRequestIsEmpty() {
        request.withOnlyValidation(true)
                .withAdsImages(Collections.emptyList());

        service.importInternal(operatorUid, owner, request);

        verify(service, never())
                .enrichUploadedImageHashes(eq(owner.getClientId()), eq(excelFetchedData.getAdsSheets()),
                        eq(request.getAdsImages()));
        verify(service)
                .uploadIfNeedExcelFileAndGetUrl(eq(false), eq(mdsHolder),
                        any(MdsFileKey.class), eq(owner.getClientId()), eq(excelFetchedData), eq(false));
    }

    @Test
    public void checkImportInternal_NotEnrichUploadedImageHashes_WhenImportModeIsOnlyAdGroups() {
        request.withOnlyValidation(true)
                .withImportMode(InternalAdImportMode.ONLY_AD_GROUPS)
                .withAdsImages(List.of(new UploadedImageInfo()
                        .withFileName("somePic.jpg")
                        .withImageHash(RandomStringUtils.randomAlphanumeric(11))));

        service.importInternal(operatorUid, owner, request);

        verify(service, never())
                .enrichUploadedImageHashes(eq(owner.getClientId()), eq(excelFetchedData.getAdsSheets()),
                        eq(request.getAdsImages()));
        verify(service)
                .uploadIfNeedExcelFileAndGetUrl(eq(false), eq(mdsHolder),
                        any(MdsFileKey.class), eq(owner.getClientId()), eq(excelFetchedData), eq(false));
    }

    @Test
    public void checkUploadIfNeedExcelFileAndGetUrl_WhenNeedUploadFile() {
        MdsFileKey fileKey = toMdsFileKey(request.getExcelFileKey());
        doCallRealMethod()
                .when(service)
                .uploadIfNeedExcelFileAndGetUrl(eq(true), eq(mdsHolder),
                        eq(fileKey), eq(owner.getClientId()), eq(excelFetchedData), eq(false));
        String expectedUrl = RandomStringUtils.randomAlphanumeric(21);
        doReturn(expectedUrl)
                .when(service).saveToMds(eq(owner.getClientId()), any());

        String url = service.uploadIfNeedExcelFileAndGetUrl(true, mdsHolder,
                fileKey, owner.getClientId(), excelFetchedData, false);

        verify(service)
                .saveToMds(eq(owner.getClientId()), any());
        assertThat(url)
                .isEqualTo(expectedUrl);
    }

    @Test
    public void checkUploadIfNeedExcelFileAndGetUrl_WhenNotNeedUploadFile() {
        MdsFileKey fileKey = toMdsFileKey(request.getExcelFileKey());
        doCallRealMethod()
                .when(service)
                .uploadIfNeedExcelFileAndGetUrl(eq(false), eq(mdsHolder),
                        eq(fileKey), eq(owner.getClientId()), eq(excelFetchedData), eq(false));
        String host = RandomStringUtils.randomAlphanumeric(11);
        int port = 1234;
        String namespace = RandomStringUtils.randomAlphanumeric(7);
        doReturn(new MdsHosts(new HostPort(host, port), null, null))
                .when(mdsHolder).getHosts();
        doReturn(new MdsNamespace(namespace, null))
                .when(mdsHolder).getNamespace();

        String url = service.uploadIfNeedExcelFileAndGetUrl(false, mdsHolder,
                fileKey, owner.getClientId(), excelFetchedData, false);

        verify(service, never())
                .saveToMds(eq(owner.getClientId()), any());
        String expectedUrl = String.format("http://%s:%d/get-%s/%d/%s", host, port, namespace,
                fileKey.getGroup(), fileKey.getFilename());
        assertThat(url)
                .isEqualTo(expectedUrl);
    }

}
