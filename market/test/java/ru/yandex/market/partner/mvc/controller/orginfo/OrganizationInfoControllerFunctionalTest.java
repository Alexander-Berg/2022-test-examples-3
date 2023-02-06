package ru.yandex.market.partner.mvc.controller.orginfo;

import java.text.MessageFormat;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.abo.api.entity.spark.CompanyExtendedReport;
import ru.yandex.market.abo.api.entity.spark.data.OKOPF;
import ru.yandex.market.abo.api.entity.spark.data.Report;
import ru.yandex.market.abo.api.entity.spark.data.ReportInfo;
import ru.yandex.market.abo.api.entity.spark.data.ResponseSparkStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.abo._public.AboPublicService;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.partner.mvc.controller.campaign.model.ShopSubtype;
import ru.yandex.market.partner.mvc.controller.orginfo.model.OrganizationInfoValidator;
import ru.yandex.market.partner.mvc.exception.ErrorSubcode;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.common.test.util.JsonTestUtil.parseJson;

/**
 * Функциональные тесты для {@link OrganizationInfoController}.
 */
@DbUnitDataSet(before = "OrganizationInfoControllerFunctionalTest.csv")
class OrganizationInfoControllerFunctionalTest extends FunctionalTest {
    private static final String BAD_PARAM_TEMPLATE = "'{'" +
            "  \"code\":\"BAD_PARAM\"," +
            "  \"details\":'{'\"field\":\"{0}\",\"subcode\":\"{1}\"}" +
            "}";

    @Autowired
    private AboPublicService aboPublicService;


    /**
     * Проверить получение юридической информации из СПАРК (с помощью АБО, см {@link AboPublicService}).
     */
    @ParameterizedTest
    @EnumSource(ResponseSparkStatus.class)
    void testSparkInfo(final ResponseSparkStatus status) {
        final OrganizationType organizationType = OrganizationType.OOO;
        final String registrationNumber = "5147746248689";
        final long uid = 2517;

        final CompanyExtendedReport extendedReport = createCompanyExtendedReport(registrationNumber, status);
        doReturn(extendedReport).when(aboPublicService).getOgrnInfo(eq(registrationNumber), eq(uid));

        final String url = baseUrl + buildSparkInfoUrl(organizationType, registrationNumber, uid);
        final ResponseEntity<String> response = FunctionalTestHelper.get(url);

        JsonTestUtil.assertEquals(response, getClass(), String.format("GetSparkInfoTest-%s.json", status));

        verify(aboPublicService, times(1)).getOgrnInfo(eq(registrationNumber), eq(uid));
        verifyNoMoreInteractions(aboPublicService);
    }

    /**
     * Проверить получение юридической информации из СПАРК (с помощью АБО, см {@link AboPublicService}).
     * Возвращает правильный тип организации независимо от переданного типа.
     */
    @ParameterizedTest
    @EnumSource(OrganizationType.class)
    void testSparkInfoOrganizationType(final OrganizationType organizationType) {
        final ResponseSparkStatus status = ResponseSparkStatus.OK;
        final String registrationNumber = "5147746248689";
        final long uid = 2517;

        final CompanyExtendedReport extendedReport = createCompanyExtendedReport(registrationNumber, status);
        doReturn(extendedReport).when(aboPublicService).getOgrnInfo(eq(registrationNumber), eq(uid));

        final String url = baseUrl + buildSparkInfoUrl(organizationType, registrationNumber, uid);
        final ResponseEntity<String> response = FunctionalTestHelper.get(url);

        JsonTestUtil.assertEquals(response, getClass(), "GetSparkInfoTest-OK.json");

        verify(aboPublicService, times(1)).getOgrnInfo(eq(registrationNumber), eq(uid));
        verifyNoMoreInteractions(aboPublicService);
    }

    /**
     * Проверить получение юридической информации из СПАРК (с помощью АБО, см {@link AboPublicService}).
     * Возвращает правильный тип организации независимо когда тип на входе не передаем.
     */
    @Test
    void testSparkInfoOrganizationTypeNull() {
        final ResponseSparkStatus status = ResponseSparkStatus.OK;
        final String registrationNumber = "5147746248689";
        final long uid = 2517;

        final CompanyExtendedReport extendedReport = createCompanyExtendedReport(registrationNumber, status);
        doReturn(extendedReport).when(aboPublicService).getOgrnInfo(eq(registrationNumber), eq(uid));

        final String url = baseUrl + buildSparkInfoUrl(null, registrationNumber, uid);
        final ResponseEntity<String> response = FunctionalTestHelper.get(url);

        JsonTestUtil.assertEquals(response, getClass(), "GetSparkInfoTest-OK.json");

        verify(aboPublicService, times(1)).getOgrnInfo(eq(registrationNumber), eq(uid));
        verifyNoMoreInteractions(aboPublicService);
    }

    /**
     * Создание орг инфо.
     * Входные и выходные данные одинаковые (идентификаторы на фронт не отдаются).
     */
    @Test
    @DbUnitDataSet(after = "orgInfoCreation.after.csv")
    void testCreateOrgInfo() {
        checkGoodRequest();
    }

    /**
     * Изменение орг инфо (добавляется в organization_info_all) и должно во вьюхе поменяться,
     * но в тестах не вьюха, а как таблица замоканая organization_info.
     */
    @Test
    @DbUnitDataSet(
            before = "orgInfoCreated.csv",
            after = "orgInfoUpdate.after.csv"
    )
    void testUpdateOrgInfo() {
        checkGoodRequest();
    }

    /**
     * Проверить что для ИП параметр "юридический адрес" является опциональным.
     */
    @Test
    @DbUnitDataSet(after = "orgInfoUpdateIP.after.csv")
    void testUpdateOrgInfoForIPWithoutJuridicalAddress() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"IP\",\n" +
                "  \"name\": \"ИП Name\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkGood(requestJson, "GetOrganizationInfoTest-IP.json");
    }

    /**
     * Изменение орг инфо,проверка валидации на пустое поле.
     */
    @Test
    void testEmptyOrgTypeUpdateOrgInfo() {
        final String requestJson = "{\n" +
                "  \"name\": \"Name\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "organizationType", ErrorSubcode.MISSING);
    }

    @Test
    void testEmptyNameUpdateOrgInfo() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "name", ErrorSubcode.MISSING);
    }

    @Test
    void testEmptyRegNumUpdateOrgInfo() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"Name\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "registrationNumber", ErrorSubcode.MISSING);
    }

    @Test
    void testInvalidLongName() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"" + generateString(OrganizationInfoValidator.FIELD_NAME_MAX_LENGTH + 1) + "\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "name", ErrorSubcode.INVALID);
    }

    @Test
    void testInvalidLongJuridicalAddress() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"Name\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"juridicalAddress\": \""
                + generateString(OrganizationInfoValidator.FIELD_ADDRESS_MAX_LENGTH + 1) + "\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "juridicalAddress", ErrorSubcode.INVALID);
    }

    @Test
    void testInvalidRegNumUpdateOrgInfo() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"Name\",\n" +
                "  \"registrationNumber\": \"cvetocheck\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "registrationNumber", ErrorSubcode.INVALID);
    }

    @Test
    void testEmptyJurAddressUpdateOrgInfo() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"Name\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "juridicalAddress", ErrorSubcode.MISSING);
    }

    @Test
    void testEmptySourceUpdateOrgInfo() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"Name\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, "source", ErrorSubcode.MISSING);
    }

    /**
     * Проверяет корректную регистрацию физ. лица для SMB.
     */
    @Test
    @DbUnitDataSet(before = "OrganizationInfoControllerFunctionalTest.smb.csv",
            after = "orgInfoCreation-physic.after.csv")
    void testSuccessPhysicCreation() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"PHYSIC\",\n" +
                "  \"name\": \"Василий Иванович ё Пупкин-О'Генри\",\n" +
                "  \"registrationNumber\": \"437730231589\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkGood(requestJson, "GetOrganizationInfoTest-physic.json");
    }

    /**
     * Проверяет, что нельзя регистрировать физ. лицо для не SMB и отлавивает навалидное ФИО.
     */
    @Test
    void testInvalidPhysicFio() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"PHYSIC\",\n" +
                "  \"name\": \"Василий_Иванович/Пупкин\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\"\n" +
                "}";
        checkBadParam(requestJson, new String[]{"name", "type"},
                new String[]{ErrorSubcode.INVALID, ErrorSubcode.INVALID});
    }

    /**
     * Получение орг инфо.
     */
    @Test
    @DbUnitDataSet(
            before = "orgInfoCreated.csv",
            after = "orgInfoCreated.csv"
    )
    void testGetOrgInfo() {
        ResponseEntity<String> resp = FunctionalTestHelper.get(baseUrl + buildOrgInfoUrl());
        JsonTestUtil.assertEquals(resp, getClass(), "GetOrganizationInfoTest.json");
    }

    @Test
    @DbUnitDataSet(before = "orgInfoCreated.csv")
    void testOrgInfoTypes() {
        checkOrganizationType(null, "GetOrganizationTypesTest-all.json");
        checkOrganizationType(null, "GetOrganizationTypesTest-all.json");
    }

    @Test
    @DbUnitDataSet(before = "orgInfoRegions.csv")
    void testOrgInfoTypesPositive() {
        final String all = "GetOrganizationTypesTest-all.json";
        final String rkb = "GetOrganizationTypesTest-rkb.json";
        final String u = "GetOrganizationTypesTest-u.json";
        final String o = "GetOrganizationTypesTest-o.json";
        final String physic = "GetOrganizationTypesTest-physic.json";

        checkOrganizationType(null, all);
        checkOrganizationType(RegionConstants.RUSSIA, rkb);
        checkOrganizationType(RegionConstants.RUSSIA, ShopSubtype.REGULAR, rkb);
        checkOrganizationType(RegionConstants.RUSSIA, ShopSubtype.SMB, physic);
        checkOrganizationType(RegionConstants.KAZAKHSTAN, rkb);
        checkOrganizationType(RegionConstants.BELARUS, rkb);
        checkOrganizationType(RegionConstants.MOSCOW, rkb);
        checkOrganizationType(RegionConstants.UKRAINE, u);
        checkOrganizationType(Long.MAX_VALUE, o);
    }

    @Test
    void testOrgInfoTypesZeroRegion() {
        expectBadRequest(() -> getOrganizationTypes(0L, null));
    }

    @Test
    void testOrgInfoTypesNegativeRegion() {
        expectBadRequest(() -> getOrganizationTypes(-1L, null));
    }

    private String buildSparkInfoUrl(final OrganizationType type, final String registrationNumber, final long uid) {
        return String.format(
                "/organizationInfo/spark?datasourceId=774&registrationNumber=%s&type=%s&_user_id=%d",
                registrationNumber, type, uid
        );
    }

    private String buildOrgInfoUrl() {
        return "/organizationInfo?datasourceId=774&_user_id=12345";
    }

    private String buildOrgTypesUrl(final Long regionId, ShopSubtype shopSubtype) {
        return UriComponentsBuilder.newInstance()
                .path("/organizationInfo/types")
                .queryParam("region", regionId)
                .queryParam("shop_subtype", shopSubtype)
                .toUriString();
    }

    private void checkOrganizationType(final Long regionId, final String jsonFileName) {
        checkOrganizationType(regionId, null, jsonFileName);
    }

    private void checkOrganizationType(final Long regionId, @Nullable ShopSubtype shopSubtype,
                                       final String jsonFileName) {
        final ResponseEntity<String> resp = getOrganizationTypes(regionId, shopSubtype);
        JsonTestUtil.assertEquals(resp, getClass(), jsonFileName);
    }

    private ResponseEntity<String> getOrganizationTypes(Long regionId, ShopSubtype shopSubtype) {
        return FunctionalTestHelper.get(baseUrl + buildOrgTypesUrl(regionId, shopSubtype));
    }

    private void checkGoodRequest() {
        final String requestJson = "{\n" +
                "  \"organizationType\": \"OOO\",\n" +
                "  \"name\": \"Name\",\n" +
                "  \"registrationNumber\": \"1023500000160\",\n" +
                "  \"juridicalAddress\": \"JURIDICAL ADDRESS\",\n" +
                "  \"source\": \"YA_MONEY\",\n" +
                "  \"infoUrl\": \"https://mycuteshop.com/org-info\",\n" +
                "  \"inn\": \"\"\n" +
                "}";
        checkGood(requestJson, "GetOrganizationInfoTest.json");
    }

    private void checkGood(final String requestJson, final String expectedJsonResponseFile) {
        final HttpEntity<String> request = createHttpEntity(requestJson);
        final String url = baseUrl + buildOrgInfoUrl();
        final ResponseEntity<String> resp = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(resp, getClass(), expectedJsonResponseFile);
    }

    private void checkBadParam(final String requestJson, final String[] fieldName, final String[] errorCode) {
        StringBuilder request = new StringBuilder("[");
        for (int i = 0; i < fieldName.length; i++) {
            if (i > 0) {
                request.append(",");
            }
            request.append(MessageFormat.format(BAD_PARAM_TEMPLATE, fieldName[i], errorCode[i]));
        }
        request.append("]");

        sendNotFullRequest(requestJson, request.toString());
    }

    private void checkBadParam(final String requestJson, final String fieldName, final String errorCode) {
        checkBadParam(requestJson, new String[]{fieldName}, new String[]{errorCode});
    }

    private HttpEntity<String> createHttpEntity(final String requestJson) {
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(requestJson, headers);
    }

    private void sendNotFullRequest(final String requestJson, final String responseJson) {
        final HttpEntity<String> request = createHttpEntity(requestJson);
        final HttpClientErrorException ex = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + buildOrgInfoUrl(), request));
        final String body = ex.getResponseBodyAsString();

        final JsonElement actualResult = parseJson(body).getAsJsonObject().get("errors");
        final JsonElement expectedResult = parseJson(responseJson);

        MatcherAssert.assertThat(actualResult.toString(), actualResult, equalTo(expectedResult));
    }

    private void expectBadRequest(Executable executable) {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                executable
        );
        String message = httpClientErrorException.getMessage();
        MatcherAssert.assertThat(
                message,
                containsString(HttpStatus.BAD_REQUEST.getReasonPhrase())
        );
    }

    private CompanyExtendedReport createCompanyExtendedReport(
            final String registrationNumber, final ResponseSparkStatus status
    ) {
        final CompanyExtendedReport extendedReport = EnhancedRandom.random(CompanyExtendedReport.class);
        final Report report = extendedReport.getReport();
        report.setIsActing(Boolean.TRUE);
        report.setOgrn(registrationNumber);
        report.setInn("1234567890");
        report.setKpp("773101001");
        report.setShortNameRus("ООО Name");
        report.getFederalTaxRegistration().setRegAuthorityAddress("FACT ADDRESS");
        report.getAddress().setAddress("JURIDICAL ADDRESS");
        report.getAddress().setPostCode("123456");
        report.setOkopf(new OKOPF("65", "12300", "Общества с ограниченной ответственностью"));
        final ReportInfo reportInfo = extendedReport.getReportInfo();
        reportInfo.setSparkStatus(status);
        return extendedReport;
    }

    private String generateString(final int length) {
        return StringUtils.leftPad(StringUtils.EMPTY, length, 'A');
    }

}
