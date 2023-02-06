package ru.yandex.direct.web.entity.excel.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.shyiko.mysql.binlog.GtidSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.excel.processing.model.internalad.ExcelImportResult;
import ru.yandex.direct.grid.core.entity.sync.service.MysqlStateService;
import ru.yandex.direct.grid.processing.util.ResponseConverter;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.excel.model.ExcelImportResponse;
import ru.yandex.direct.web.entity.excel.model.ExcelImportResultInfo;
import ru.yandex.direct.web.entity.excel.model.ValidationResponseWithExcelFileUrl;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;
import ru.yandex.direct.web.entity.excel.service.InternalAdExcelWebService;
import ru.yandex.direct.web.entity.excel.service.InternalAdValidationService;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тесты на проверку поведения метода {@link ExcelController#internalAdImport(InternalAdImportRequest, String)}
 * с использованием моков
 */
@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class ExcelControllerInternalAdImportTest {

    @Mock
    private DirectWebAuthenticationSource authenticationSource;

    @Mock
    private InternalAdValidationService internalAdValidationService;

    @Mock
    private InternalAdExcelWebService internalAdExcelWebService;

    @Mock
    private MysqlStateService mysqlStateService;

    @InjectMocks
    private ExcelController controller;

    private User operator;
    private User user;
    private UidAndClientId uidAndClientId;
    private InternalAdImportRequest request;
    private ExcelImportResult excelImportResult;
    private GtidSet.UUIDSet uuidSet;

    @Before
    public void initTestData() {
        operator = TestUsers.generateNewUser()
                .withUid(RandomNumberUtils.nextPositiveLong())
                .withRole(RbacRole.INTERNAL_AD_ADMIN);
        user = TestUsers.generateNewUser()
                .withUid(RandomNumberUtils.nextPositiveLong())
                .withClientId(ClientId.fromLong(RandomNumberUtils.nextPositiveLong()));
        uidAndClientId = UidAndClientId.of(user.getUid(), user.getClientId());
        doReturn(new DirectAuthentication(operator, user))
                .when(authenticationSource).getAuthentication();

        request = new InternalAdImportRequest()
                .withOnlyValidation(false);

        List<Long> adIds = List.of(RandomNumberUtils.nextPositiveLong());
        excelImportResult = new ExcelImportResult()
                .withAdsResult(MassResult.successfulMassAction(adIds, new ValidationResult<>(adIds)))
                .withAdGroupsResult(MassResult.emptyMassAction())
                .withExcelFileUrl(RandomStringUtils.randomAlphanumeric(14));
        doReturn(Result.successful(excelImportResult))
                .when(internalAdExcelWebService).importInternal(operator.getUid(), uidAndClientId, request);

        uuidSet = new GtidSet.UUIDSet(UUID.randomUUID().toString(), Collections.emptyList());
        doReturn(uuidSet)
                .when(mysqlStateService).getCurrentServerGtidSet(user.getClientId());
    }


    @Test
    public void checkInternalAdImport() {
        WebResponse webResponse = controller.internalAdImport(request, null);

        var importResultInfo = new ExcelImportResultInfo()
                .withAddedOrUpdatedAdGroupIds(Collections.emptyList())
                .withAddedOrUpdatedAdIds(ResponseConverter
                        .getSuccessfullyResults(excelImportResult.getAdsResult(), String::valueOf))
                .withMutationId(uuidSet.toString())
                .withExcelFileUrl(excelImportResult.getExcelFileUrl());
        var expectedResponse = new ExcelImportResponse()
                .withResult(importResultInfo);

        assertThat(webResponse)
                .is(matchedBy(beanDiffer(expectedResponse)));

        verify(mysqlStateService)
                .getCurrentServerGtidSet(user.getClientId());
        verifyZeroInteractions(internalAdValidationService);
    }

    @Test
    public void checkInternalAdImport_whenOnlyValidation() {
        request.withOnlyValidation(true);
        WebResponse webResponse = controller.internalAdImport(request, null);

        var importResultInfo = new ExcelImportResultInfo()
                .withAddedOrUpdatedAdGroupIds(Collections.emptyList())
                .withAddedOrUpdatedAdIds(Collections.emptyList())
                .withExcelFileUrl(excelImportResult.getExcelFileUrl());
        var expectedResponse = new ExcelImportResponse()
                .withResult(importResultInfo);

        assertThat(webResponse)
                .is(matchedBy(beanDiffer(expectedResponse)));

        verifyZeroInteractions(mysqlStateService);
        verifyZeroInteractions(internalAdValidationService);
    }

    @Test
    public void checkInternalAdImport_whenValidationFailedOnAdGroups() {
        var importResult = new ExcelImportResult()
                .withAdGroupsResult(failedMassResult())
                .withAdsResult(successMassResult())
                .withExcelFileUrl(RandomStringUtils.randomAlphanumeric(10));
        doReturn(Result.successful(importResult))
                .when(internalAdExcelWebService).importInternal(operator.getUid(), uidAndClientId, request);
        doReturn(new WebValidationResult())
                .when(internalAdValidationService).buildWebValidationResult(any(), any());

        WebResponse webResponse = controller.internalAdImport(request, null);

        var expectedResponse = new ValidationResponseWithExcelFileUrl(new WebValidationResult(),
                importResult.getExcelFileUrl());
        assertThat(webResponse)
                .is(matchedBy(beanDiffer(expectedResponse)));

        verifyZeroInteractions(mysqlStateService);
    }

    @Test
    public void checkInternalAdImport_whenValidationFailedOnAds() {
        var importResult = new ExcelImportResult()
                .withAdsResult(failedMassResult())
                .withAdGroupsResult(successMassResult())
                .withExcelFileUrl(RandomStringUtils.randomAlphanumeric(10));
        doReturn(Result.successful(importResult))
                .when(internalAdExcelWebService).importInternal(operator.getUid(), uidAndClientId, request);
        doReturn(new WebValidationResult())
                .when(internalAdValidationService).buildWebValidationResult(any(), any());

        WebResponse webResponse = controller.internalAdImport(request, null);

        var expectedResponse = new ValidationResponseWithExcelFileUrl(new WebValidationResult(),
                importResult.getExcelFileUrl());
        assertThat(webResponse)
                .is(matchedBy(beanDiffer(expectedResponse)));

        verifyZeroInteractions(mysqlStateService);
    }

    @Test
    public void checkInternalAdImport_whenRequestValidationFailed() {
        Result<ExcelImportResult> failedResult = failedExcelImportResult();
        doReturn(failedResult)
                .when(internalAdExcelWebService).importInternal(operator.getUid(), uidAndClientId, request);
        var expectedResponse = new ValidationResponse(new WebValidationResult());
        doReturn(expectedResponse)
                .when(internalAdValidationService).buildValidationResponse(failedResult);

        WebResponse webResponse = controller.internalAdImport(request, null);

        assertThat(webResponse)
                .isEqualTo(expectedResponse);

        verifyZeroInteractions(mysqlStateService);
    }

    private static MassResult<Long> failedMassResult() {
        var objectIds = List.of(RandomNumberUtils.nextPositiveLong());
        var vr = new ValidationResult<List<Long>, Defect>(
                objectIds, List.of(CommonDefects.invalidValue()), emptyList()
        );
        return MassResult.brokenMassAction(objectIds, vr);
    }

    private static MassResult<Long> successMassResult() {
        var objectIds = List.of(RandomNumberUtils.nextPositiveLong());
        var vr = new ValidationResult<List<Long>, Defect>(objectIds);
        return MassResult.brokenMassAction(objectIds, vr);
    }

    private static Result<ExcelImportResult> failedExcelImportResult() {
        var vr = new ValidationResult<ExcelImportResult, Defect>(
                null, List.of(CommonDefects.invalidValue()), emptyList()
        );
        return Result.broken(vr);
    }

}
