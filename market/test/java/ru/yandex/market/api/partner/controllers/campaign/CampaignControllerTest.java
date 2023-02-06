package ru.yandex.market.api.partner.controllers.campaign;

import java.lang.reflect.Field;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.common.services.auth.blackbox.UserInfo;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ru.yandex.market.api.partner.controllers.campaign.CampaignController}
 *
 * @author nastik
 */
@ExtendWith(MockitoExtension.class)
class CampaignControllerTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 10774;
    private static final long SUPPLIER_AG_SUBCLIENT_CAMPAIGN_ID = 10877;
    private static final long REGULAR_SUPPLIER_CAMPAIGN_ID = 10879;
    private static final Long BLUE_AGENCY_UID = 67282297L;

    private static final Long WHITE_AGENCY_UID = 32282297L;
    private static final Long WHITE_AGENCY_CLIENT_ID = 8004L;

    @Autowired
    private BlackboxService blackboxService;

    @Spy
    @Autowired
    private AgencyService agencyService;

    @DisplayName("Получение stateReason с 26 - достижение дневного бюджета, JSON.")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.before.csv")
    void getCampaignByIdTestJson() {
        ResponseEntity<String> response = makeRequest(Format.JSON);
        String expected = StringTestUtil.getString(getClass(), "expectedCampaignWithStateReason26.json");
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @DisplayName("Получение stateReason с 26 - достижение дневного бюджета, XML.")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.before.csv")
    void getCampaignByIdTestXml() {
        ResponseEntity<String> response = makeRequest(Format.XML);
        String expected = StringTestUtil.getString(getClass(), "expectedCampaignWithStateReason26.xml");

        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @DisplayName("Получение списка кампаний.")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.getCampaignsByUid.before.csv")
    void getCampaignsByUid() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(urlBasePrefix + "/campaigns",
                HttpMethod.GET, Format.JSON);
        String expected = StringTestUtil.getString(getClass(), "expectedCampaigns.json");
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Список логинов для синего на взаимозачете")
    @DbUnitDataSet(before = "CampaignControllerTest.getLogins.before.csv")
    void getLogins() throws Exception {
        Mockito.when(blackboxService.userinfoByUid(anyLong())).thenReturn(mockUserInfo(100));

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s/campaigns/%s/logins",
                urlBasePrefix, 1000571241L), HttpMethod.GET, Format.JSON);
        String expected = StringTestUtil.getString(getClass(), "expectedLogins.json");
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @DisplayName("Получение списка кампаний агентства.")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.agency.before.csv")
    void getCampaignsByUidAgency() {
        when(agencyService.isAgency(WHITE_AGENCY_CLIENT_ID)).thenReturn(true);
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(urlBasePrefix + "/campaigns",
                HttpMethod.GET, Format.JSON, WHITE_AGENCY_UID);
        String expected = StringTestUtil.getString(getClass(), "expectedCampaignsAgency.json");
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @DisplayName("Получение списка кампаний для пользователя, не входящего в белый список")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.getCampaignsByUidForBlackUser.before.csv")
    void getCampaignsByUidForBlackUser() {
        HttpClientErrorException httpClientErrorException = assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(urlBasePrefix + "/campaigns", HttpMethod.GET, Format.JSON,
                        BLUE_AGENCY_UID));
        assertThat(httpClientErrorException.getStatusCode(), Matchers.is(HttpStatus.FORBIDDEN));
    }

    @DisplayName("Получение виртуальной кампании. Должна быть всегда включена.")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.getVirtualCampaign.before.csv")
    void getVirtualCampaign() {
        String response = makeRequest(Format.XML).getBody();
        String expected = StringTestUtil.getString(getClass(), "expectedVirtualEnabled.xml");
        MbiAsserts.assertXmlEquals(expected, response);
    }

    @DisplayName("Получение подагентской кампании для агентства поставщика")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.agency.before.csv")
    void getCampaignsByBlueAgencyCampaign() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(url(SUPPLIER_AG_SUBCLIENT_CAMPAIGN_ID),
                HttpMethod.GET, Format.JSON, BLUE_AGENCY_UID);
        MbiAsserts.assertJsonEquals("{\"campaign\":{\"id\":10877, \"clientId\":8012}}",
                response.getBody());
    }

    @DisplayName("Дотуп запрещен для не агентской кампании агентства поставщика")
    @Test
    @DbUnitDataSet(before = "CampaignControllerTest.agency.before.csv")
    void forbiddenOnGetCampaignsByBlueAgencyCampaign() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.makeRequest(url(REGULAR_SUPPLIER_CAMPAIGN_ID), HttpMethod.GET,
                        Format.JSON, BLUE_AGENCY_UID))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN)));
    }

    @DisplayName("Кампании по логину")
    @ParameterizedTest
    @CsvSource(value = {"67282295; expectedCampaignsByLogin.json", "12345;expectedCampaignsByLoginFiltered.json"},
            delimiter = ';')
    @DbUnitDataSet(before = "CampaignControllerTest.campaignsByLogin.before.csv")
    void getCampaignsByLogin(long uid, String expectedJson) throws Exception {
        String login = "login";
        when(blackboxService.userinfoByLogin(login)).thenReturn(mockUserInfo(uid));
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s/campaigns/by_login/%s",
                urlBasePrefix, login), HttpMethod.GET, Format.JSON, uid);
        String expected = StringTestUtil.getString(getClass(), expectedJson);
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Кампания не найдена")
    void businessInfoByCampaignNotFound() {
        String expected = StringTestUtil.getString(getClass(), "business-info/expectedForbidden.json");
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> getBusinessInfo(1001L, Format.JSON))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN)))
                .satisfies(ex -> MbiAsserts.assertJsonEquals(expected, ex.getResponseBodyAsString()));

        String expectedXml = StringTestUtil.getString(getClass(), "business-info/expectedForbidden.xml");
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> getBusinessInfo(1001L, Format.XML))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.FORBIDDEN)))
                .satisfies(ex -> MbiAsserts.assertXmlEquals(expectedXml, ex.getResponseBodyAsString()));
    }

    @Test
    @DisplayName("Нет бизнеса")
    @DbUnitDataSet(before = "business-info/CampaignControllerTest.before.csv")
    void getBusinessInfoByCampaignNotBusiness() {
        String expected = StringTestUtil.getString(getClass(), "business-info/expectedNotFound.json");
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> getBusinessInfo(17654325L, Format.JSON))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)))
                .satisfies(ex -> MbiAsserts.assertJsonEquals(expected, ex.getResponseBodyAsString()));

        String expectedXml = StringTestUtil.getString(getClass(), "business-info/expectedNotFound.xml");
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> getBusinessInfo(17654325L, Format.XML))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)))
                .satisfies(ex -> MbiAsserts.assertXmlEquals(expectedXml, ex.getResponseBodyAsString()));
    }

    @Test
    @DisplayName("Вся информация")
    @DbUnitDataSet(before = "business-info/CampaignControllerTest.before.csv")
    void getBusinessInfoByCampaignFull() {
        ResponseEntity<String> response1 = getBusinessInfo(17654321L, Format.JSON);
        ResponseEntity<String> response2 = getBusinessInfo(17654323L, Format.JSON);
        MbiAsserts.assertJsonEquals(response1.getBody(), response2.getBody());
        String expected = StringTestUtil.getString(getClass(), "business-info/expectedFullBusinessInfo.json");
        MbiAsserts.assertJsonEquals(expected, response1.getBody());

        ResponseEntity<String> responseXml1 = getBusinessInfo(17654321L, Format.XML);
        ResponseEntity<String> responseXml2 = getBusinessInfo(17654323L, Format.XML);
        MbiAsserts.assertXmlEquals(responseXml1.getBody(), responseXml2.getBody());
        String expectedXml = StringTestUtil.getString(getClass(), "business-info/expectedFullBusinessInfo.xml");
        MbiAsserts.assertXmlEquals(expectedXml, responseXml1.getBody());
    }


    @Test
    @DisplayName("Простая информация без склада")
    @DbUnitDataSet(before = "business-info/CampaignControllerTest.before.csv")
    void getBusinessInfoByCampaignSimple() {
        ResponseEntity<String> response = getBusinessInfo(17654324L, Format.JSON);
        String expected = StringTestUtil.getString(getClass(), "business-info/expectedSimpleBusinessInfo.json");
        MbiAsserts.assertJsonEquals(expected, response.getBody());

        ResponseEntity<String> responseXml = getBusinessInfo(17654324L, Format.XML);
        String expectedXml = StringTestUtil.getString(getClass(), "business-info/expectedSimpleBusinessInfo.xml");
        MbiAsserts.assertXmlEquals(expectedXml, responseXml.getBody());
    }


    private ResponseEntity<String> getBusinessInfo(long campaignId, Format format) {
        return FunctionalTestHelper.makeRequest(String.format("%s/campaigns/%s/business-info",
                urlBasePrefix, campaignId), HttpMethod.GET, format);
    }

    private ResponseEntity<String> makeRequest(Format format) {
        return FunctionalTestHelper.makeRequest(url(CAMPAIGN_ID), HttpMethod.GET, format);
    }

    private String url(long campaignId) {
        return String.format("%s/campaigns/%d",
                urlBasePrefix, campaignId);
    }

    private UserInfo mockUserInfo(long uid) throws Exception {
        Class<?> clazz = UserInfo.class;
        Object cc = clazz.getConstructor().newInstance();
        Field uidField = cc.getClass().getDeclaredField("uid");
        uidField.setAccessible(true);
        uidField.set(cc, uid);
        Field loginField = cc.getClass().getDeclaredField("login");
        loginField.setAccessible(true);
        loginField.set(cc, "login" + uid);
        return (UserInfo) cc;
    }

}
