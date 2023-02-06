package ru.yandex.market.partner.mvc.controller.delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.dbunit.database.DatabaseConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.partner.PartnerApplicationTestHelper;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.report.PdfTestUtil;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link DeliveryPartnerApplicationController}.
 */
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
@DbUnitDataSet(

        before = "data/delivery-partner-application-controller-test-before.csv"
)
class DeliveryPartnerApplicationControllerTest extends FunctionalTest {

    private static final long TEST_CAMPAIGN_ID_1 = 11001L;
    private static final long TEST_CAMPAIGN_ID_2 = 11002L;
    private static final long TEST_CAMPAIGN_ID_3 = 11003L;
    private static final long TEST_CAMPAIGN_ID_4 = 11004L;
    private static final long TEST_REQUEST_ID_0 = 1000L;
    private static final long TEST_REQUEST_ID_1 = 1101L;

    private static final long CITY_ID = 1000L;
    private static final long COUNTRY_ID = 2000L;

    private static final Integer DELIVERY_INIT_VALIDATION_NOTIFICATION_TYPE = 1576129239;
    private static final Integer DELIVERY_SALES_INIT_VALIDATION_NOTIFICATION_TYPE = 1581398795;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RegionService regionService;
    @Autowired
    private TestableClock testableClock;
    @Autowired
    private NotificationService notificationService;

    private static Stream<Arguments> requestNotFoundParameters() {
        return Stream.of(
                Arguments.of(PartnerApplicationStatus.CLOSED),
                Arguments.of(PartnerApplicationStatus.CANCELLED)
        );
    }

    private static Stream<Arguments> successfulSubmitParameters() {
        return Stream.of(
                Arguments.of(PartnerApplicationStatus.NEW),
                Arguments.of(PartnerApplicationStatus.NEED_INFO)
        );
    }

    private static Stream<Arguments> testSubmitFromInvalidStatusParameters() {
        return Arrays.stream(PartnerApplicationStatus.values())
                .filter(s -> s != PartnerApplicationStatus.NEW
                        && s != PartnerApplicationStatus.NEED_INFO
                        && s != PartnerApplicationStatus.CLOSED
                        && s != PartnerApplicationStatus.CANCELLED)
                .map(Arguments::of);
    }

    @Nonnull
    private static Stream<Arguments> testGetPdfParameters() {
        return Stream.of(
                Arguments.of(
                        "для юр.лица",
                        TEST_CAMPAIGN_ID_1,
                        "/mvc/delivery/pdf/daas_request_ur.txt"
                ),
                Arguments.of(
                        "для ИП",
                        TEST_CAMPAIGN_ID_2,
                        "/mvc/delivery/pdf/daas_request_ip.txt"
                ),
                Arguments.of(
                        "для ручного указания",
                        TEST_CAMPAIGN_ID_4,
                        "/mvc/delivery/pdf/daas_request_manual.txt"
                )
        );
    }

    @BeforeEach
    void setUp() {
        testableClock.setFixed(Instant.parse("2019-10-02T11:00:00.00Z"), ZoneOffset.UTC);
        when(regionService.isSubregionOf(eq(CITY_ID), eq(COUNTRY_ID))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        testableClock.clearFixed();
    }

    /**
     * Тест проверяет, что создание новой заявки на подключение к доставке работает корректно.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-after.csv"
    )
    void testCreateDeliveryApplication() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-response.json").build());
    }

    /**
     * Тест проверяет, что создание новой заявки на подключение к доставке работает корректно для ИП.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-ip-after.csv"
    )
    void testCreateDeliveryApplicationIp() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-valid-ogrn-ip.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-ip-create-response.json").build());
    }

    /**
     * Тест проверяет, что КПП обнуляется для ИП.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-ip-after.csv"
    )
    void testKppDeliveryApplicationIp() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-kpp-ip.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-ip-create-response.json").build());
    }

    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-with-market-id-after.csv"
    )
    void testCreateDeliveryApplicationIpWithMarketId() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-with-market-id.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-with-market-id-response.json").build());
    }

    /**
     * Тест проверяет, что создание новой заявки на подключение к доставке работает корректно при наличии новых полей
     * для ручной установки типа организации / должности / документа основания.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-manual-org-type-and-position-after.csv"
    )
    void testCreateDeliveryApplicationWithManualOrganizationAndPosition() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-with-manual-org-type-and-position.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-response-with-manual-org-type-and-position.json").build());
    }

    /**
     * Тест проверяет, что изменение существующей заявки на подключение к доставке работает корректно.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-update-delivery-application-before.csv",
            after = "data/test-update-delivery-application-after.csv"
    )
    void testUpdateDeliveryApplication() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-response.json").build());
    }

    /**
     * Нельзя менять засабмиченную заявку.
     */
    @Test
    @DbUnitDataSet(
            before = "data/edit-submitted-delivery-partner-application-before.csv",
            after = "data/edit-submitted-delivery-partner-application-before.csv"
    )
    void testUpdateSubmittedApplication() {

        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        getJson("data/delivery-application-request.json").toString()
                )
        );

        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-already-submitted-application.json");
    }

    /**
     * Тест проверяет, что изменение существующей заявки на подключение к доставке работает корректно.
     * Передаем с маркетИД.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-update-delivery-application-before.csv",
            after = "data/test-update-delivery-application-after-market-id.csv"
    )
    void testUpdateDeliveryApplicationWithMarketId() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-with-market-id.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-with-market-id-response.json").build());
    }

    /**
     * Тест проверяет, что существует возможность сохранить заявку частично, при этом остальные поля будут
     * иметь значение {@code null}, а значение {@code validations.canDownload == false}.
     */
    @Test
    void testPartialCreateDeliveryApplication() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                JsonTestUtil.getJsonHttpEntity(getClass(), "data/delivery-application-partial-request.json")
        );

        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-partial-response.json");
    }

    /**
     * Тест проверяет, что вызов ручки {@code GET /delivery/application} для магазина, у которого не было заявок,
     * возвращает пустую заявку.
     */
    @Test
    void testGetApplicationForNewShop() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-get-empty-response.json");
    }

    /**
     * При первом запросе получаем форму в минимальном состоянии, которая является валидной для последующего обновления.
     */
    @Test
    void testUpdateApplicationByInitialForm() throws IOException {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        ResponseEntity<String> responseUpdateEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                JsonTestUtil.getJsonHttpEntity(extractForm(responseEntity.getBody()))
        );
        JsonTestUtil.assertEquals(responseUpdateEntity, getClass(),
                "data/delivery-application-partial-response.json");
    }

    /**
     * Создание заявки, у которой фактический адрес совпадает с юридическим.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-with-equals-addresses-after.csv"
    )
    void testCreateDeliveryApplicationWithEqualsAddresses() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-with-equals-addresses.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-response-with-equals-addresses.json").build());
    }

    /**
     * Создание заявки, у которой фактический адрес отсутствует и не совпадает с юридическим.
     *
     * <p>Проверяем отсутвие возможности засабмитить такую заявку (значение {@code validations.canSubmit == false}).
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-create-delivery-application-before.csv",
            after = "data/test-create-delivery-application-without-address-after.csv"
    )
    void testCreateDeliveryApplicationWithoutPostAddresses() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(
                getPostEditURL(TEST_CAMPAIGN_ID_1),
                getJson("data/delivery-application-request-without-post-address.json").toString());

        JsonTestUtil.assertEquals(responseEntity,
                getJson("data/delivery-application-create-response-without-post-address.json").build());

        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-application-without-post-address.json");
    }

    private String extractForm(String application) throws IOException {
        return JsonTestUtil.parseJson(application)
                .getAsJsonObject().get("result")
                .getAsJsonObject().get("form")
                .getAsJsonObject().toString();
    }

    /**
     * Тест проверяет, что вызов ручки {@code GET /delivery/application} для магазина
     * возвращает корректный dto-объект с комментарием.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-get-delivery-application-before.csv"
    )
    void testGetApplicationWithComment() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-get-with-comment.json");
    }

    /**
     * Тест проверяет, что вызов ручки {@code GET /delivery/application} для магазина
     * возвращает корректный dto-объект с вручную указанным типом организации / должностью / документом основанием.
     */
    @Test
    @DbUnitDataSet(before = "data/test-get-delivery-application-with-manual-org-type-and-position-before.csv"
    )
    void testGetApplicationWithWithManualOrganizationAndPosition() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-create-response-with-manual-org-type-and-position.json");
    }

    /**
     * Тест проверяет, что вызов ручки {@code GET /delivery/application} для магазина, у которого
     * заявка находится в статусе {@link PartnerApplicationStatus#CLOSED} возвращает пустую заявку.
     */
    @ParameterizedTest
    @MethodSource("requestNotFoundParameters")
    @DbUnitDataSet(
            before = "data/test-get-application-for-shop-with-closed-application.before.csv"
    )
    void testGetApplicationForShopWithClosedApplication(PartnerApplicationStatus partnerApplicationStatus) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_0, partnerApplicationStatus);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-get-empty-response.json");
    }

    /**
     * Тест проверяет, что вызов ручки {@code GET /delivery/application} для магазина с заполненой заявкой,
     * возвращает корректное содержимое заявки.
     */
    @Test
    @DbUnitDataSet(
            before = "data/test-get-filled-application.before.csv"
    )
    void testGetFilledApplication() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-create-response-with-market-id.json");
    }

    /**
     * Тест проверяет, что при сохранении заявки с невалидными данными, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidDto() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(), "data/delivery-application-invalid.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(), "data/validation-error-response.json");
    }

    /**
     * При сохранении заявки с невалидным документом, на основании которого действует организация, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidFormationTypeOoo() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(),
                                "data/delivery-application-request-invalid-formation-type-ooo.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-error-response-formation-type.json");
    }

    /**
     * При сохранении заявки с невалидным документом, на основании которого действует ИП, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidFormationTypeIp() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(),
                                "data/delivery-application-request-invalid-formation-type-ip.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-error-response-formation-type.json");
    }

    /**
     * Тест проверяет, что при сохранении заявки с невалидными данными ОГРН для ИП, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidOgrnIp() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(),
                                "data/delivery-application-request-invalid-ogrn-ip.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-error-response-ogrn-ip.json");
    }

    /**
     * Тест проверяет, что при сохранении заявки с невалидными данными ОГРН для ООП, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidOgrnOoo() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(),
                                "data/delivery-application-request-invalid-ogrn-ooo.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-error-response-ogrn-ooo.json");
    }

    /**
     * Тест проверяет, что при сохранении заявки с невалидными данными ИНН для ИП, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidInnIp() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(),
                                "data/delivery-application-request-invalid-inn-ip.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-error-response-inn.json");
    }

    /**
     * Тест проверяет, что при сохранении заявки с невалидными данными ИНН для ООП, возникает ошибка 400.
     */
    @Test
    @DbUnitDataSet
    void testUpdateApplicationWithInvalidInnOoo() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        getPostEditURL(TEST_CAMPAIGN_ID_1),
                        JsonTestUtil.getJsonHttpEntity(getClass(),
                                "data/delivery-application-request-invalid-inn-ooo.json")
                )
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-error-response-inn.json");
    }

    /**
     * Тест проверяет, что для полностью заполненной заявки в статусе {@link PartnerApplicationStatus#NEW} или
     * {@link PartnerApplicationStatus#NEW} значение {@code validations.canSubmit == true}.
     */
    @ParameterizedTest
    @MethodSource("successfulSubmitParameters")
    @DbUnitDataSet(before = {
            "data/test-get-filled-application.before.csv",
            "data/test-get-ready-for-submit-application.before.csv"})
    void testGetReadyForSubmitApplication(PartnerApplicationStatus status) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_1, status);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(
                responseEntity,
                getExpectedJson(status, "data/delivery-application-get-ready-for-submit-response.json")
        );
    }

    /**
     * Для заявки подписанта, действующего по доверенности без загруженного подтвержадющего документа,
     * значение {@code validations.canSubmit == false}.
     */
    @ParameterizedTest
    @MethodSource("successfulSubmitParameters")
    @DbUnitDataSet(before = {
            "data/test-get-filled-application-poa.before.csv",
            "data/test-get-ready-for-submit-application.before.csv"})
    void testGetNotReadyForSubmitApplicationPoa(PartnerApplicationStatus status) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_1, status);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(
                responseEntity,
                getExpectedJson(status, "data/delivery-application-get-not-ready-for-submit-response.json")
        );
    }

    /**
     * Тест проверяет, что для полностью заполненной заявки в статусе {@link PartnerApplicationStatus#NEW} или
     * {@link PartnerApplicationStatus#NEW} значение {@code validations.canSubmit == true}
     * для подписанта, действующего по доверенности.
     */
    @ParameterizedTest
    @MethodSource("successfulSubmitParameters")
    @DbUnitDataSet(before = {
            "data/test-get-filled-application-poa.before.csv",
            "data/test-get-ready-for-submit-application-poa.before.csv"})
    void testGetReadyForSubmitApplicationPoa(PartnerApplicationStatus status) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_1, status);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(
                responseEntity,
                getExpectedJson(status, "data/delivery-application-get-ready-for-submit-poa-response.json")
        );
    }

    /**
     * Тест проверяет, что прикрепленные документы отдаются корректно.
     */
    @Test
    @DbUnitDataSet(
            before = {"data/test-get-filled-application.before.csv", "data/test-get-documents.before.csv"}
    )
    void testGetDocuments() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getGetApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-get-filled-response-with-documents.json");
    }

    /**
     * Тест проверяет, что сабмит заполненной заявки в статусе {@link PartnerApplicationStatus#NEW} или
     * {@link PartnerApplicationStatus#NEED_INFO} переводит заявку в статус {@link PartnerApplicationStatus#INIT}.
     */
    @ParameterizedTest
    @MethodSource("successfulSubmitParameters")
    @DbUnitDataSet(
            before = {"data/test-get-filled-application.before.csv",
                    "data/test-get-ready-for-submit-application.before.csv"}
    )
    void testSuccessfulSubmit(PartnerApplicationStatus status) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_1, status);

        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-submit-response.json");

        ArgumentCaptor<NotificationSendContext> notificationCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, times(2)).send(notificationCaptor.capture());

        List<NotificationSendContext> notifications = notificationCaptor.getAllValues();

        assertThat(notifications, Matchers.hasSize(2));
        assertEquals(notifications.get(0).getTypeId(), DELIVERY_INIT_VALIDATION_NOTIFICATION_TYPE);
        assertEquals(notifications.get(1).getTypeId(), DELIVERY_SALES_INIT_VALIDATION_NOTIFICATION_TYPE);
    }

    /**
     * Сабмит заполненной заявки в статусе {@link PartnerApplicationStatus#NEW} или
     * {@link PartnerApplicationStatus#NEED_INFO} переводит заявку в статус {@link PartnerApplicationStatus#INIT}
     * для подписанта, действующего на основании доверенности.
     */
    @ParameterizedTest
    @MethodSource("successfulSubmitParameters")
    @DbUnitDataSet(
            before = {"data/test-get-filled-application-poa.before.csv",
                    "data/test-get-ready-for-submit-application-poa.before.csv"}
    )
    void testSuccessfulPoaSubmit(PartnerApplicationStatus status) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_1, status);

        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1));
        JsonTestUtil.assertEquals(responseEntity, getClass(),
                "data/delivery-application-submit-poa-response.json");
        ArgumentCaptor<NotificationSendContext> notificationCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, times(2)).send(notificationCaptor.capture());

        List<NotificationSendContext> notifications = notificationCaptor.getAllValues();

        assertThat(notifications, Matchers.hasSize(2));
        assertEquals(notifications.get(0).getTypeId(), DELIVERY_INIT_VALIDATION_NOTIFICATION_TYPE);
        assertEquals(notifications.get(1).getTypeId(), DELIVERY_SALES_INIT_VALIDATION_NOTIFICATION_TYPE);
    }

    /**
     * Сабмит заявки подписанта, действующего на основании доверенности, без загруженного документа выдает ошибку 400.
     */
    @Test
    @DbUnitDataSet(
            before = {"data/test-get-filled-application-poa.before.csv",
                    "data/test-get-ready-for-submit-application.before.csv"}
    )
    void testSubmitPoaWithoutSignatoryDoc() {

        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );

        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-application-without-signatory-doc.json");
    }

    /**
     * Сабмит заявки подписанта, действующего на основании сертификата о регистрации, без загруженного документа выдает
     * ошибку 400.
     */
    @Test
    @DbUnitDataSet(before = {
            "data/test-get-filled-application-certificate.before.csv",
            "data/test-get-ready-for-submit-application.before.csv"
    })
    void testSubmitCertificateWithoutSignatoryDoc() {

        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );

        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-application-without-signatory-doc.json");
    }

    /**
     * Тест проверяет, что при сабмите пустой заявки возникает
     * ошибка 400 и описание ошибки содержит детальное описание полей.
     */
    @Test
    @DbUnitDataSet(before = "data/test-submit-empty-delivery-application.before.csv")
    void testSubmitEmptyDeliveryApplication() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-empty-application.json");
    }

    /**
     * Тест проверяет, что при сабмите заявки с незаполненными полями для ручного ввода
     * и соответствующим списочным значением OTHER возникает ошибка 400
     * и описание ошибки содержит детальное описание полей.
     */
    @Test
    @DbUnitDataSet(before = "data/test-get-filled-application-without-manual-fields.before.csv")
    void testSubmitEmptyManualFieldsDeliveryApplication() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-empty-manual-fields-application.json");
    }

    /**
     * Тест проверяет, что при сабмите несуществующей заявки возникает
     * ошибка 404.
     */
    @Test
    @DbUnitDataSet(before = "data/test-submit-non-existing-application.before.csv")
    void testSubmitNonExistingApplication() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-nonexisting-application.json");
    }

    /**
     * Тест проверяет, что при сабмите (вызов заявки, находящейся в статусе
     * отличном от {@link PartnerApplicationStatus#NEW}
     * или {@link PartnerApplicationStatus#NEED_INFO} возникает ошибка 400.
     */
    @ParameterizedTest
    @MethodSource("testSubmitFromInvalidStatusParameters")
    @DbUnitDataSet(before = "data/test-submit-from-invalid-status.before.csv")
    void testSubmitFromInvalidStatus(PartnerApplicationStatus status) {
        PartnerApplicationTestHelper.setApplicationStatus(jdbcTemplate, TEST_REQUEST_ID_1, status);
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(getSubmitApplicationURL(TEST_CAMPAIGN_ID_1))
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(), "data/validation-invalid-status.json");
    }

    @DisplayName("Генерация PDF документа заполненной заявки")
    @ParameterizedTest(name = "[{index}] {0}")
    @DbUnitDataSet(
            before = "data/test-get-application-pdf.before.csv"
    )
    @MethodSource("testGetPdfParameters")
    void testGetApplicationPdf(@SuppressWarnings("unused") String caseName, long campaignId, String expectedPdfFile)
            throws IOException {
        ResponseEntity<byte[]> response = FunctionalTestHelper.get(getDownloadPdfURL(campaignId), byte[].class);
        HttpHeaders headers = response.getHeaders();

        assertThat(headers.getContentDisposition().getFilename(), equalTo("dass-service-agreement.pdf"));
        assertThat(headers.getContentType(), equalTo(MediaType.APPLICATION_PDF));

        PdfTestUtil.assertPdfTextEqualsFile(
                response.getBody(),
                IOUtils.toString(
                        getClass().getResourceAsStream(expectedPdfFile),
                        StandardCharsets.UTF_8
                ),
                1);
    }

    @DisplayName("Ошибка генерация PDF документа незаполненной заявки")
    @Test
    @DbUnitDataSet(before = "data/test-submit-empty-delivery-application.before.csv")
    void testGetApplicationPdfError() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getDownloadPdfURL(TEST_CAMPAIGN_ID_1), byte[].class)
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-cannot-generate-pdf.json");
    }

    @DisplayName("Ошибка генерация PDF документа для несуществующей заявки")
    @Test
    void testGetApplicationPdfWithoutApplication() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getDownloadPdfURL(TEST_CAMPAIGN_ID_1), byte[].class)
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-submit-nonexisting-application.json");
    }

    @DisplayName("Ошибка генерация PDF документа из-за отсутствия бизнес овнера")
    @Test
    @DbUnitDataSet(before = "data/test-get-application-pdf-without-business-owner.before.csv")
    void testGetApplicationPdfWithoutBusinessOwner() {
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getDownloadPdfURL(TEST_CAMPAIGN_ID_3), byte[].class)
        );
        JsonTestUtil.assertResponseErrorMessage(e, getClass(),
                "data/validation-no-businessowner.json");
    }

    private String getExpectedJson(PartnerApplicationStatus status, String fileName) {
        return StringTestUtil.getString(getClass(), fileName)
                .replace("${statusId}", String.valueOf(status.getId()));
    }

    private JsonTestUtil.JsonTemplateBuilder getJson(final String fileName) {
        return JsonTestUtil.fromJsonTemplate(getClass(), fileName);
    }

    private String getPostEditURL(final long campaignId) {
        return baseUrl + "/delivery/application/edits?id=" + campaignId + "&_user_id=123";
    }

    private String getGetApplicationURL(final long campaignId) {
        return baseUrl + "/delivery/application?id=" + campaignId + "&_user_id=123";
    }

    private String getDownloadPdfURL(final long campaignId) {
        return baseUrl + "/delivery/application/document?id=" + campaignId + "&_user_id=123";
    }

    private String getSubmitApplicationURL(final long campaignId) {
        return baseUrl + "/delivery/application/submit?id=" + campaignId + "&_user_id=123";
    }
}
