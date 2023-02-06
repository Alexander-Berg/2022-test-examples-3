package ru.yandex.market.partner.mvc.controller.application;


import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.state.event.PartnerAppChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerAppDataOuterClass;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.balance.BalanceService.ROBOT_MBI_BALANCE_UID;
import static ru.yandex.market.mbi.util.MoreMbiMatchers.jsonPropertyMatches;


/**
 * Функциональные тесты для {@link PartnerApplicationController}.
 */
@DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.csv")
public class PartnerApplicationControllerFunctionalTest extends FunctionalTest {

    private static final long UID = 100500L;
    private static final long CLIENT_ID = 100500L;
    @Autowired
    private BalanceContactService contactService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private CheckouterShopApi checkouterShopApi;
    @Autowired
    private CheckouterAPI checkouterClient;
    @Autowired
    private LogbrokerEventPublisher<PartnerAppChangesProtoLBEvent> logbrokerPartnerAppChangesEventPublisher;
    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        doReturn(Instant.parse("2022-07-05T01:01:01Z")).when(clock).instant();
    }

    private static Stream<Arguments> getArgumentsForWhiteShopDefaultPrepayRequest() {
        return Stream.of(
                Arguments.of("Возвращаем форму для магазина, у которого указано и имя магазина, и домен,",
                        10001L, "data/json/defaultApplication.white.withDomain.json"),
                Arguments.of("Возвращаем форму для магазина, у которого указано только имя магазина",
                        10011L, "data/json/defaultApplication.white.withoutDomain.json")
        );
    }

    @BeforeEach
    void init() {
        environmentService.setValue("application.return.contact.validation.disabled", "true");
        environmentService.setValue("PartnerApplicationController.disable.edit.contract", "true");
        when(contactService.getClientIdByUid(UID)).thenReturn(CLIENT_ID);
        when(balanceService.getClient(CLIENT_ID)).thenReturn(new ClientInfo(CLIENT_ID, ClientType.PHYSICAL));
    }

    @Test
    void testGetInitialSupplierApplicationRightAfterRegistrationWithMarketId() {
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10774, UID));
        String expected = fromFile(
                "data/json/getInitialSupplierApplicationRightAfterRegistrationWithMarketIdResponse.json"
        );
        expected = expected.replace("@{currentdate}", LocalDate.now().toString());

        JsonTestUtil.assertEquals(
                response,
                expected
        );
    }

    @Test
    void testGetInitialSupplierApplicationWithoutCpaYamRequestHistoryEntry() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));
        String expected = fromFile("data/json/getInitialSupplierApplicationWithoutCpaYamRequestHistoryEntryResponse.json");
        expected = expected.replace("@{currentdate}", LocalDate.now().toString());

        JsonTestUtil.assertEquals(
                response,
                expected
        );
    }

    @Test
    void testApplicationUpdate() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/applicationUpdateRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/applicationUpdateResponse.json"
        );

        //проверяем отправку эвента в логброкер
        ArgumentCaptor<PartnerAppChangesProtoLBEvent> captor =
                ArgumentCaptor.forClass(PartnerAppChangesProtoLBEvent.class);
        Mockito.verify(logbrokerPartnerAppChangesEventPublisher).publishEventAsync(captor.capture());
        PartnerAppDataOuterClass.PartnerAppData event = captor.getValue().getPayload();
        assertThat(event.getGeneralInfo().getActionType()).isEqualTo(GeneralData.ActionType.CREATE);
        assertThat(event.getRequestId()).isEqualTo(1L);
        assertThat(event.getPartnerIdsList()).containsExactly(3L);
    }

    @Test
    void testApplicationUpdateState() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/applicationUpdateStateRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/applicationUpdateStateResponse.json"
        );
    }

    @Test
    void testApplicationUpdateSelfEmployed() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/applicationUpdateSelfEmployedRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/applicationUpdateSelfEmployedResponse.json"
        );

        //проверяем отправку эвента в логброкер
        ArgumentCaptor<PartnerAppChangesProtoLBEvent> captor =
                ArgumentCaptor.forClass(PartnerAppChangesProtoLBEvent.class);
        Mockito.verify(logbrokerPartnerAppChangesEventPublisher).publishEventAsync(captor.capture());
        PartnerAppDataOuterClass.PartnerAppData event = captor.getValue().getPayload();
        assertThat(event.getGeneralInfo().getActionType()).isEqualTo(GeneralData.ActionType.CREATE);
        assertThat(event.getRequestId()).isEqualTo(1L);
        assertThat(event.getPartnerIdsList()).containsExactly(3L);
    }

    @Test
    void testApplicationUpdateSelfEmployedDBS() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110779L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/applicationUpdateSelfEmployedDBSRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10779L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10779L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/applicationUpdateSelfEmployedDBSResponse.json"
        );

        //проверяем отправку эвента в логброкер
        ArgumentCaptor<PartnerAppChangesProtoLBEvent> captor =
                ArgumentCaptor.forClass(PartnerAppChangesProtoLBEvent.class);
        Mockito.verify(logbrokerPartnerAppChangesEventPublisher).publishEventAsync(captor.capture());
        PartnerAppDataOuterClass.PartnerAppData event = captor.getValue().getPayload();
        assertThat(event.getGeneralInfo().getActionType()).isEqualTo(GeneralData.ActionType.CREATE);
        assertThat(event.getRequestId()).isEqualTo(1L);
        assertThat(event.getPartnerIdsList()).containsExactly(6L);
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionTest.testApplicationUpdateExisting.before.csv",
            after = "data/PartnerApplicationControllerFunctionTest.testApplicationUpdateExisting.after.csv"
    )
    void testApplicationUpdateExisting() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateExistingRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateExistingResponse.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionTest.testSelfEmployedApplicationUpdateExisting.before.csv"
    )
    void testSelfEmployedApplicationUpdateExisting() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partnerApplicationControllerSelfEmployedApplicationUpdateExistingRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerSelfEmployedApplicationUpdateExistingResponse.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionTest.testApplicationUpdateReplicationComplete.before" +
                    ".csv",
            after = "data/PartnerApplicationControllerFunctionTest.testApplicationUpdateReplicationComplete.after.csv"
    )
    void testApplicationUpdateReplicationComplete() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateReplicationCompleteRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateReplicationCompleteResponse.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionTest.testApplicationUpdateReplicationComplete.before" +
                    ".csv",
            after = "data/PartnerApplicationControllerFunctionTest.testApplicationUpdateReplicationComplete.before.csv"
    )
    void testApplicationUpdateReplicationCompleteWrongField() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        // language=json
        String requestTest = "" +
                "{" +
                "    \"organizationInfo\":{" +
                "        \"accountNumber\":\"1111111111\"," +
                "        \"name\":\"changed\"," +
                "        \"corrAccountNumber\":\"1111111111\"," +
                "        \"bik\":\"1111111111\"," +
                "        \"bankName\":\"Yndx Bank\"" +
                "    }," +
                "    \"vatInfo\":{" +
                "        \"taxSystem\":1," +
                "        \"vat\":1," +
                "        \"vatSource\":1," +
                "        \"deliveryVat\":1" +
                "    }" +
                "}";
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "organizationInfo.name", "INVALID")));

    }

    @Test
    void testApplicationUpdateWithMarketId() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/testApplicationUpdateWithMarketIdRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/testApplicationUpdateWithMarketIdResponse.json"
        );
    }

    @Test
    void testPartialApplicationUpdate() {

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/testPartialApplicationUpdateRequest.json"
        );

        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);

        request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/testPartialApplicationUpdate1Request.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10774, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/testPartialApplicationUpdateResponse.json"
        );
    }

    @Test
    void testPartialApplicationUpdateWithMarketIdDel() {
        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partialApplicationUpdateWithMarketIdDelRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);

        request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partialApplicationUpdateWithMarketIdDel1Request.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10774, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partialApplicationUpdateWithMarketIdDelResponse.json"
        );
    }

    /**
     * Проверяем удаление документа заявления поставщика.
     */
    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalWithDocTest.csv",
            after = "data/PartnerApplicationControllerFunctionalWithDocTest.after.csv")
    void testApplicationUpdateClearDocuments() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        String returnContact = "{" +
                "        \"email\":\"ivan@yandex.ru\"," +
                "        \"firstName\":\"Ivan\"," +
                "        \"lastName\":\"Pryanikov\"," +
                "        \"phoneNumber\":\"+79151002030\"," +
                "        \"isEnabled\":true" +
                "    }";
        String requestTest = "" +
                "{" +
                "    \"name\":\"my edited shop\"," +
                "    \"domain\":\"edit1.my.shop.ru\"," +
                "    \"organizationInfo\":{" +
                "        \"name\":\"IP My Shop\"," +
                "        \"type\":\"4\"," +
                "        \"ogrn\":\"123456789012345\"," +
                "        \"inn\":\"7743880975\"," +
                "        \"kpp\":\"123456789\"," +
                "        \"postcode\":\"111333\"," +
                "        \"factAddress\":\"Moscow, Lva Tolstogo, 16\"," +
                "        \"juridicalAddress\":\"Moscow, Lva Tolstogo, 16\"," +
                "        \"accountNumber\":\"12345678901234567890\"," +
                "        \"corrAccountNumber\":\"23456789012345678901\"," +
                "        \"bik\":\"456808023\"," +
                "        \"bankName\":\"Sber Bank\"," +
                "        \"licenseNumber\":\"12341252356\"," +
                "        \"licenseDate\":\"2018-01-10\"," +
                "        \"isAutoFilled\":false" +
                "    }," +
                "    \"contactInfo\":{" +
                "        \"name\":\"Vasia Petrovich Pupkin\"," +
                "        \"email\":\"vasia.pupkin123@yandex.ru\"," +
                "        \"phoneNumber\":\"+7 916 1234455\"" +
                "    }," +
                "    \"signatory\":{" +
                "        \"name\":\"Vasia Petrovich Pupkin\"," +
                "        \"docType\":\"1\"," +
                "        \"docInfo\":\"As IP\"," +
                "        \"position\":\"Director\"" +
                "    }," +
                "    \"vatInfo\":{" +
                "        \"taxSystem\":0," +
                "        \"vat\":2," +
                "        \"vatSource\":0," +
                "        \"deliveryVat\":2" +
                "    }," +
                "    \"documents\":[]," +
                "    %s" +
                "}";
        requestTest = String.format(requestTest, "\"returnContact\": " + returnContact);
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalWithDocDeclinedTest.csv")
    void testApplicationUpdateWithDocumentsInBadStatus() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));
        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerFunctionalWithDocDeclined.json"
        );
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalWithDocTest.csv")
    void testApplicationUpdateWithDocuments() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));
        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerFunctionalWithDoc.json"
        );

    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.updateContact.before.csv",
    after = "data/PartnerApplicationControllerFunctionalTest.updateContact.after.csv")
    void testContactUpdate() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partnerApplicationControllerFunctionalTestUpdateContactRequest.json"
        );
        FunctionalTestHelper.post(partnerContactEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        String expected = fromFile("data/json/partnerApplicationControllerFunctionalTestUpdateContactResponse.json");
        JsonTestUtil.assertEquals(
                response,
                expected);
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testNewStatusOnCancelled.before.csv"
    )
    void testNewStatusOnCancelled() {
        // given
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));
        JsonObject resultJson = ((JsonObject) JsonTestUtil.parseJson(response.getBody())).getAsJsonObject("result");

        //В случае CANCELLED-заявки данные формы возвращаются, но исчезает requestId, а статус ставится в NEW
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/newStatusOnCancelled.json"
        );
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.updateContactSuccess.before.csv")
    void testContactUpdateWithRequestSuccess() {
        // given
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/contactUpdateWithRequestSuccessRequest.json"
        );

        // when
        FunctionalTestHelper.post(partnerContactEditsUrl(10776L, UID), request);
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        // then
        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/contactUpdateWithRequestSuccessResponse.json"
        );

        var argClient = ArgumentCaptor.forClass(FullClientInfo.class);
        verify(balanceService).createClient(argClient.capture(), eq(UID), anyLong());
        var clientInfo = argClient.getValue();
        assertThat(clientInfo.getId()).isEqualTo(711L);
        assertThat(clientInfo.getEmail()).isEqualTo("some@email.com");
        assertThat(clientInfo.getName()).isEqualTo("ООО orgName");

        var argPerson = ArgumentCaptor.forClass(PersonStructure.class);
        var numberOfContracts = 2;
        verify(balanceService, times(numberOfContracts)).createOrUpdatePerson(argPerson.capture(), eq(ROBOT_MBI_BALANCE_UID));
        var persons = argPerson.getAllValues();
        var personIncome = persons.stream().filter(p -> p.getPersonId() == 811).findFirst().get();
        assertThat(personIncome.getClientId()).isEqualTo(711L);
        assertThat(personIncome.getLongName()).isEqualTo("ООО orgName");
        assertThat(personIncome.getName()).isEqualTo("orgName");
        assertThat(personIncome.getString("EMAIL")).isEqualTo("some@email.com");
        assertThat(personIncome.getIsPartner()).isFalse();
        var personOutcome = persons.stream().filter(p -> p.getPersonId() == 2811).findFirst().get();
        assertThat(personOutcome.getClientId()).isEqualTo(711L);
        assertThat(personOutcome.getLongName()).isEqualTo("ООО orgName");
        assertThat(personOutcome.getName()).isEqualTo("orgName");
        assertThat(personOutcome.getString("EMAIL")).isEqualTo("some@email.com");
        assertThat(personOutcome.getIsPartner()).isTrue();
    }

    @Test
    void testPartialApplicationUpdate_usingFullFio() {
        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partialApplicationUpdateUsingFullFioRequest.json"
        );

        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);

        request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partialApplicationUpdateUsingFullFio1Request.json"
        );

        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);
        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10774, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partialApplicationUpdateUsingFullFioResponse.json"
        );
    }

    @Test
    void testApplicationStatusUpdate_unsuccessful() {
    /*
    Сохраняем черновик заявки, в котором поле ОГРН имеет некорректное кол-во символов.
    Поле номер счета тоже содержит некорректное кол-во символов.
     */
        String requestTest = fromFile("data/json/PartnerApplicationControllerFunctionalTest.invalid.json");
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10774, UID), request);

    /*
    Проверяем, что при неправильном формате данных в черновике, изменения статуса заявки невозможно.
     */
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                ))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdate.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdate.after.csv"
    )
    void testApplicationStatusUpdateSuccessful() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10774, UID),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testSelfEmployedStatusUpdate.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testSelfEmployedStatusUpdate.after.csv"
    )
    void testSelfEmployedApplicationStatusUpdateSuccessful() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10774, UID),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testSelfEmployedStatusUpdateDbs.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testSelfEmployedStatusUpdateDbs.after.csv"
    )
    void testSelfEmployedApplicationStatusUpdateSuccessfulDbs() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110779L);
        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10779, UID),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testSelfEmployedInitWrongOrgType.before.csv"
    )
    void testSelfEmployedApplicationStatusUpdateWrongOrgType() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                )
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testSelfEmployedInitWrongAutoFilled.before.csv"
    )
    void testSelfEmployedApplicationStatusUpdateWrongAutoFilled() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                )
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateProcessed.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateProcessed.after.csv"
    )
    void testApplicationStatusUpdateProcessed() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10774, UID),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateReturnContact.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateReturnContact.after.csv"
    )
    void testApplicationStatusUpdateReturnContactSuccessful() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10774, UID),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTestPdf.dsbs.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTestPdf.dsbs.after.csv")
    void testApplicationStatusUpdateReturnContactSuccessfuDsbs() {
        when(contactService.getClientIdByUid(1001L)).thenReturn(110776L);

        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10001L, 1001L),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateReturnContactFbs.before.csv",
                    "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateReturnContact.before.csv"
            })
    void testApplicationStatusUpdateReturnContactFbs() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                ))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateProcessed.before.csv")
    void testApplicationStatusUpdateWrongStatus() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"1\"}")
                ))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.testStatusUpdateNpdFiltered.csv")
    void testApplicationStatusUpdateWrongNpdStatus() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                ))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    @Test
    @DisplayName("Проверка обновления статуса заявки при пустых контактах для возврата и общения с покупателями")
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.updateStatusEmptyReturnContacts.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.updateStatusEmptyReturnContacts.after.csv"
    )
    void testApplicationStatusUpdateWithEmptyReturnContacts() {
        environmentService.setValue("application.return.contact.check.empty.disabled", "true");
        environmentService.setValue("application.business.contact.check.empty.disabled", "true");
        when(contactService.getClientIdByUid(1001L)).thenReturn(110776L);

        FunctionalTestHelper.put(partnerApplicationEditStatusUrl(10001L, 1001L),
                JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}"));
    }

    @Test
    @DisplayName("Успешное обновление статуса заявки для B2B_SELLER")
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testUpdateStatusSuccessB2bSeller.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testUpdateStatusSuccessB2bSeller.after.csv"
    )
    void testApplicationStatusUpdateSuccessB2bSeller() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110774L);
        FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                );
    }

    @Test
    @DisplayName("Обновление статуса заявки для B2B_SELLER не на ОСН")
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testUpdateStatusBadRequestB2bSeller.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testUpdateStatusBadRequestB2bSeller.after.csv"
    )
    void testApplicationStatusUpdateBadRequestB2bSeller() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> FunctionalTestHelper.put(
                        partnerApplicationEditStatusUrl(10774, UID),
                        JsonTestUtil.getJsonHttpEntity("{\"status\": \"0\"}")
                ))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));

    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("getArgumentsForWhiteShopDefaultPrepayRequest")
    @DbUnitDataSet(before = "data/PartnerApplicationControllerTest.whiteShop.noPrepay.csv")
    void testGetDefaultPrepayRequestForWhiteShop(String testName,
                                                 long campaignId,
                                                 String expectedResponseFile) {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(campaignId, UID));

        assertThat(response).has(sameResultAsFile(expectedResponseFile));
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerTest.whiteShop.noPrepay.csv",
            after = "data/PartnerApplicationControllerTest.whiteShop.after.csv")
    void testCreateAndUpdateApplicationForWhiteShop() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

    /*
    Создание заявки на предоплату.
     */
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10001L, UID),
                JsonTestUtil.getJsonHttpEntity(fromFile("data/json/application.white.createRequest.json")));

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10001L, UID));

        assertThat(response).has(sameResultAsFile("data/json/application.white.afterCreation.response.json"));
    /*
    Обновление заявки на предоплату.
     */
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10001L, UID),
                JsonTestUtil.getJsonHttpEntity(fromFile("data/json/application.white.updateRequest.json")));

        response = FunctionalTestHelper.get(partnerApplicationUrl(10001L, UID));

        assertThat(response).has(sameResultAsFile("data/json/application.white.afterUpdate.response.json"));
    }

    /**
     * Проверяем что при обновлении 1 заявки, обновится и вторая
     */
    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerTest.whiteShop.contactUpdate.before.csv",
            after = "data/PartnerApplicationControllerTest.whiteShop.contactUpdate.after.csv")
    void testWhiteShopContactsUpdate() {
        checkShopContactsUpdate();
        verify(balanceService, times(1)).createClient(any(), eq(UID), anyLong());
    }

    @Test
    @DbUnitDataSet(before = {"data/PartnerApplicationControllerTest.whiteShop.contactUpdateWithoutContract.before.csv",
            "data/PartnerApplicationControllerTest.whiteShop.contactUpdateWithLink.before.csv"},
            after = "data/PartnerApplicationControllerTest.whiteShop.contactUpdateWithoutContract.after.csv")
    void testDbsShopWithLinkWithoutContractContactsUpdate() {
        checkShopContactsUpdate();
        verifyNoInteractions(balanceService);
    }

    private void checkShopContactsUpdate() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        FunctionalTestHelper.post(partnerContactEditsUrl(10001L, UID),
                JsonTestUtil.getJsonHttpEntity(fromFile("data/json/application.white.contactUpdateRequest.json")));

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10001L, UID));

        assertThat(response).has(sameResultAsFile("data/json/application.white.afterContactUpdate.response.json"));
    }

    @Test
    void testApplicationUpdateAllWithoutPersonReturnContacts() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/applicationUpdateAllWithoutPersonReturnContactsRequest.json"
        );

        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/applicationUpdateAllWithoutPersonReturnContactsResponse.json"
        );
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.updateContact.before.csv")
    void testApplicationUpdateSupplierFastReturnEnabled() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        String requestSetFlagToTrue = //language=json
                "{\"supplierFastReturnEnabled\": true}";
        var request = JsonTestUtil.getJsonHttpEntity(requestSetFlagToTrue);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        assertThat(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject()
                .get("result").getAsJsonObject()
                .get("supplierFastReturnEnabled").getAsBoolean()).isTrue();

        String requestSetFlagToFalse = //language=json
                "{\"supplierFastReturnEnabled\": false}";
        request = JsonTestUtil.getJsonHttpEntity(requestSetFlagToFalse);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        assertThat(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject()
                .get("result").getAsJsonObject()
                .get("supplierFastReturnEnabled").getAsBoolean()).isFalse();
    }

    @Test
    @DbUnitDataSet(before = {"data/PartnerApplicationControllerFunctionalTest.updateContact.before.csv",
            "data/PartnerApplicationControllerFunctionalTest.fastReturn.before.csv"})
    void testApplicationUpdateSupplierFastReturnEnabledForShopAndSupplier() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        String requestSetFlagToTrue = //language=json
                "{\"supplierFastReturnEnabled\": true}";
        var request = JsonTestUtil.getJsonHttpEntity(requestSetFlagToTrue);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        assertThat(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject()
                .get("result").getAsJsonObject()
                .get("supplierFastReturnEnabled").getAsBoolean()).isTrue();
    }

    @Test
    @DbUnitDataSet(before = "data/PartnerApplicationControllerFunctionalTest.updateContact.before.csv")
    void testContactUpdateSupplierFastReturnEnabled() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        String requestSetFlagToTrue = //language=json
                "{\"supplierFastReturnEnabled\": true}";
        var request = JsonTestUtil.getJsonHttpEntity(requestSetFlagToTrue);
        FunctionalTestHelper.post(partnerContactEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        assertThat(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject()
                .get("result").getAsJsonObject()
                .get("supplierFastReturnEnabled").getAsBoolean()).isTrue();

        String requestSetFlagToFalse = //language=json
                "{\"supplierFastReturnEnabled\": false}";
        request = JsonTestUtil.getJsonHttpEntity(requestSetFlagToFalse);
        FunctionalTestHelper.post(partnerContactEditsUrl(10776L, UID), request);

        response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        assertThat(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject()
                .get("result").getAsJsonObject()
                .get("supplierFastReturnEnabled").getAsBoolean()).isFalse();
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testApplicationGetIsAutoFilled.before.csv")
    void testApplicationGetIsAutoFilled() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));
        assertThat(response).has(
                sameResultAsFile("data/json/application.testApplicationGetIsAutoFilled.response.json"));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testApplicationGetIsAutoFilled.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testApplicationClearIsAutoFilled.after.csv"
    )
    void testApplicationClearIsAutoFilled() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        String requestTest = fromFile("data/json/application.testApplicationClearIsAutoFilled.request.json");
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testApplicationClearAutoFilledState.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testApplicationClearAutoFilledState.after.csv"
    )
    void testApplicationClearAutoFilledState() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        String requestTest = fromFile("data/json/application.testApplicationClearAutoFilledState.request.json");
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testApplicationSetIsAutoFilled.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testApplicationSetIsAutoFilled.after.csv"
    )
    void testApplicationSetIsAutoFilled() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        String requestTest = fromFile("data/json/application.testApplicationSetIsAutoFilled.request.json");
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);
    }

    @Test
    @DbUnitDataSet(before =
            "data/PartnerApplicationControllerFunctionTest.testApplicationWithContractDisabledUpdate.before.csv")
    void testApplicationWithContractDisabledUpdate() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        // language=json
        String requestTest = "" +
                "{" +
                "    \"name\":\"my edited shop\"," +
                "    \"domain\":\"edit1.my.shop.ru\"," +
                "    \"organizationInfo\":{" +
                "        \"name\":\"OAO My Shop\"," +
                "        \"type\":\"7\"," +
                "        \"ogrn\":\"1234567890123\"," +
                "        \"inn\":\"8280348699\"," +
                "        \"kpp\":\"123456789\"," +
                "        \"postcode\":\"111333\"," +
                "        \"factAddress\":\"Moscow, Lva Tolstogo, 16\"," +
                "        \"juridicalAddress\":\"Moscow, Lva Tolstogo, 16\"," +
                "        \"accountNumber\":\"12345678901234567890\"," +
                "        \"corrAccountNumber\":\"12345678901234567890\"," +
                "        \"bik\":\"456808023\"," +
                "        \"bankName\":\"Sber Bank\"," +
                "        \"licenseNumber\":\"12341252356\"," +
                "        \"licenseDate\":\"2018-01-10\"," +
                "        \"workSchedule\":\"24 na 7\"," +
                "        \"isAutoFilled\":false" +
                "    }," +
                "    \"contactInfo\":{" +
                "        \"firstName\":\"Венечка\"," +
                "        \"lastName\":\"Ерофеев\"," +
                "        \"email\":\"vasia.pupkin123@yandex.ru\"," +
                "        \"phoneNumber\":\"+7 916 1234455\"," +
                "        \"shopPhoneNumber\":\"+7 916 1234454\"," +
                "        \"shopAddress\":\"my shop address\"" +
                "    }," +
                "    \"signatory\":{" +
                "        \"firstName\":\"Владимир\"," +
                "        \"lastName\":\"Путан\"," +
                "        \"docType\":\"1\"," +
                "        \"docInfo\":\"As IP\"," +
                "        \"position\":\"Director\"" +
                "    }," +
                "    \"vatInfo\":{" +
                "        \"taxSystem\":0," +
                "        \"vat\":2," +
                "        \"vatSource\":0," +
                "        \"deliveryVat\":2" +
                "    }," +
                "    \"documents\":[]," +
                "  \"returnContacts\": [\n" +
                "    {\n" +
                "      \"email\": \"ivan@yandex.ru\",\n" +
                "      \"lastName\": \"Андреев\",\n" +
                "      \"firstName\": \"Егор\",\n" +
                "      \"secondName\": \"Александрович\",\n" +
                "      \"phoneNumber\": \"+79151002030\",\n" +
                "      \"type\": \"PERSON\",\n" +
                "      \"isEnabled\": true\n" +
                "    }" +
                "]," +
                "  \"returnContact\": {\n" +
                "      \"email\": \"ivan@yandex.ru\",\n" +
                "      \"lastName\": \"Андреев\",\n" +
                "      \"firstName\": \"Егор\",\n" +
                "      \"secondName\": \"Александрович\",\n" +
                "      \"phoneNumber\": \"+79151002030\",\n" +
                "      \"type\": \"PERSON\",\n" +
                "      \"isEnabled\": true\n" +
                "  }" +
                "}";
        var request = JsonTestUtil.getJsonHttpEntity(requestTest);
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() ->
                        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request))
                .has(HamcrestCondition.matching(HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)));
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testUpdateB2bSeller.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testUpdateB2bSeller.after.csv"
    )
    void testApplicationUpdateB2bSellerSuccess() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateB2bSellerSuccessRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateB2bSellerSuccessResponse.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "data/PartnerApplicationControllerFunctionalTest.testUpdateB2bSeller.before.csv",
            after = "data/PartnerApplicationControllerFunctionalTest.testUpdateB2bSeller.after.csv"
    )
    void testApplicationUpdateB2bSellerFail() {
        when(contactService.getClientIdByUid(UID)).thenReturn(110776L);

        var request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateB2bSellerFailRequest.json"
        );
        FunctionalTestHelper.post(partnerApplicationEditsUrl(10776L, UID), request);

        ResponseEntity<String> response = FunctionalTestHelper.get(partnerApplicationUrl(10776L, UID));

        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "data/json/partnerApplicationControllerApplicationUpdateB2bSellerFailResponse.json"
        );
    }

    private Condition<ResponseEntity<String>> sameResultAsFile(String resultFromFile) {
        String expected = fromFile(resultFromFile);
        var matcher =
                MoreMbiMatchers.responseBodyMatches(jsonPropertyMatches("result",
                        MbiMatchers.jsonEquals(expected, List.of(
                                new Customization("updatedAt", (v1, v2) -> true),
                                new Customization("organizationInfo.licenseDate", (v1, v2) -> true),
                                new Customization("requestId", (v1, v2) -> true))
                        )));
        return HamcrestCondition.matching(matcher);
    }

    private String partnerApplicationUrl(long campaignId, long euid) {
        return baseUrl + "/partner/application?euid=" + euid +
                "&id=" + campaignId;
    }

    private String partnerApplicationEditsUrl(long campaignId, long euid) {
        return baseUrl + "/partner/application/edits?euid=" + euid +
                "&id=" + campaignId;
    }

    private String partnerContactEditsUrl(long campaignId, long euid) {
        return baseUrl + "/partner/application/contacts/edits?euid=" + euid +
                "&id=" + campaignId;
    }

    private String partnerApplicationEditStatusUrl(long campaignId, long euid) {
        return baseUrl + "/partner/application/status?euid=" + euid +
                "&id=" + campaignId;
    }

    private String fromFile(String filePath) {
        return StringTestUtil.getString(getClass(), filePath);
    }
}
