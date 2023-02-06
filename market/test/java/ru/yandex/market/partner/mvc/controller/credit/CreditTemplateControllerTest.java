package ru.yandex.market.partner.mvc.controller.credit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.credit.CreditOrganizationType;
import ru.yandex.market.core.credit.CreditTemplateType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты на {@link CreditTemplateController}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "csv/CreditTemplateControllerTest.before.csv")
class CreditTemplateControllerTest extends FunctionalTest {

    private static final String GET_URL = "%s/campaign/%d/creditTemplates";
    private static final String POST_URL = "%s/campaign/%d/creditTemplate";
    private static final String DELETE_URL = "%s/campaign/%d/creditTemplate/%d";

    @DisplayName("Получение всех кредитных шаблонов магазина")
    @Test
    void testGet() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(String.format(GET_URL, baseUrl, 10211));
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        JsonTestUtil.assertEquals(entity, getClass(), "json/CreditTemplateControllerTest.get.json");
    }

    @DisplayName("Получение всех кредитных шаблонов магазина, если у него их нет")
    @Test
    void testGetNoTemplates() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(String.format(GET_URL, baseUrl, 10212));
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        JsonTestUtil.assertEquals(entity, getClass(), "json/CreditTemplateControllerTest.getEmpty.json");
    }

    @DisplayName("Добавление нового кредитного шаблона")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.post.after.csv")
    void testAddNewTemplate() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(null)
                .setPartnerId(23L)
                .setBankId(36L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.DEFAULT_FOR_ALL)
                .setCreditOrganizationType(CreditOrganizationType.BANK)
                .build();

        ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10213), requestDTO);
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        JsonTestUtil.assertEquals(entity, getClass(), "json/CreditTemplateControllerTest.add.json");
    }

    @DisplayName("Добавление нового кредитного шаблона без указания bank_id")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.post.microcredit.after.csv")
    void testAddNewTemplateWithoutBankId() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(null)
                .setPartnerId(23L)
                .setBankId(null)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.DEFAULT_FOR_ALL)
                .setCreditOrganizationType(CreditOrganizationType.MICROCREDIT)
                .build();

        ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10213), requestDTO);
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        JsonTestUtil.assertEquals(entity, getClass(), "json/CreditTemplateControllerTest.add.microcredit.json");
    }

    @DisplayName("Добавление нового кредитного шаблона c типом Яндекс.Касса")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.post.microcredit.after.csv")
    void testAddNewTemplateYandexKassa() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(null)
                .setPartnerId(23L)
                .setBankId(null)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.DEFAULT_FOR_ALL)
                .setCreditOrganizationType(CreditOrganizationType.YANDEX_KASSA)
                .build();

        ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10213), requestDTO);
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        JsonTestUtil.assertEquals(entity, getClass(), "json/CreditTemplateControllerTest.add.yakassa.json");
    }

    @DisplayName("Добавление нового кредитного шаблона: несуществующий bank_id")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.before.csv")
    void testAddNewTemplateBadBankId() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(null)
                .setPartnerId(23L)
                .setBankId(37L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.FEED)
                .build();

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10213), requestDTO));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @DisplayName("Добавление нового кредитного шаблона: попытка добавить второй дефолтный шаблон")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.before.csv")
    void testAddNewTemplateSecondDefault() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(null)
                .setPartnerId(21L)
                .setBankId(1L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.DEFAULT_FOR_ALL)
                .setCreditOrganizationType(CreditOrganizationType.BANK)
                .build();

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10211), requestDTO));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @DisplayName("Добавление нового кредитного шаблона: попытка добавить чересчур много шаблонов")
    @Test
    void testAddNewTemplateTooMany() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setPartnerId(22L)
                .setBankId(1L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.FEED)
                .setCreditOrganizationType(CreditOrganizationType.BANK)
                .build();

        for (int i = 0; i < 50; i++) {
            FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10212), requestDTO);
        }

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10212), requestDTO));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @DisplayName("Добавление нового кредитного шаблона не триггерит смену статусов FAIL, REVOKE, FAIL_MANUAL, остальные переводит в NEW")
    @Test
    @DbUnitDataSet(before = "csv/CreditTemplateControllerTest.failedStatuses.before.csv",
            after = "csv/CreditTemplateControllerTest.failedStatuses.after.csv")
    void testAddCheckStatuses() {
        CreditTemplateRequestDTO.Builder builder = new CreditTemplateRequestDTO.Builder()
                .setId(null)
                .setBankId(36L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.FEED)
                .setCreditOrganizationType(CreditOrganizationType.BANK);

        for (int i = 24; i <= 29; i++) {
            CreditTemplateRequestDTO requestDTO = builder.setPartnerId(i).build();
            ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, i), requestDTO);
            Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        }
    }

    @DisplayName("Обновление кредитного шаблона")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.post.update.after.csv")
    void testUpdateTemplate() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(2000L)
                .setPartnerId(21L)
                .setBankId(1L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.FEED)
                .setCreditOrganizationType(CreditOrganizationType.BANK)
                .build();
        ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10211), requestDTO);
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @DisplayName("Обновление дефолтного кредитного шаблона")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.post.updateDefault.after.csv")
    void testUpdateDefaultTemplate() {
        CreditTemplateRequestDTO requestDTO = new CreditTemplateRequestDTO.Builder()
                .setId(1000L)
                .setPartnerId(21L)
                .setBankId(1L)
                .setMinRateScaled(1356000)
                .setMaxTermMonths(24)
                .setConditionsUrl("https://mybestshop.su/credits/conditions.html")
                .setType(CreditTemplateType.DEFAULT_FOR_ALL)
                .setCreditOrganizationType(CreditOrganizationType.BANK)
                .build();
        ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, 10211), requestDTO);
        Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @DisplayName("Изменение кредитного шаблона не триггерит смену статусов FAIL, REVOKE, FAIL_MANUAL, остальные переводит в NEW")
    @Test
    @DbUnitDataSet(before = "csv/CreditTemplateControllerTest.failedStatuses.before.csv",
            after = "csv/CreditTemplateControllerTest.failedStatuses.after.csv")
    void testUpdateCheckStatuses() {
        CreditTemplateRequestDTO.Builder builder = new CreditTemplateRequestDTO.Builder()
                .setBankId(36L)
                .setConditionsUrl("yandex.ru")
                .setMaxTermMonths(1)
                .setMinRateScaled(1200000)
                .setType(CreditTemplateType.FEED)
                .setCreditOrganizationType(CreditOrganizationType.BANK);

        for (int i = 24; i <= 29; i++) {
            CreditTemplateRequestDTO requestDTO = builder
                    .setPartnerId(i)
                    .setId(1000L + i)
                    .build();
            ResponseEntity<String> entity = FunctionalTestHelper.post(String.format(POST_URL, baseUrl, i), requestDTO);
            Assertions.assertEquals(HttpStatus.OK, entity.getStatusCode());
        }
    }

    @DisplayName("Удаление кредитного шаблона")
    @Test
    @DbUnitDataSet(after = "csv/CreditTemplateControllerTest.delete.after.csv")
    void testDeleteTemplate() {
        FunctionalTestHelper.delete(String.format(DELETE_URL, baseUrl, 10211, 2000));
    }

    @DisplayName("Удаление последнего кредитного шаблона триггерит смену статусов, если он был у магазина последним " +
            "и статус не был REVOKE")
    @Test
    @DbUnitDataSet(before = "csv/CreditTemplateControllerTest.failedStatuses.before.csv",
            after = "csv/CreditTemplateControllerTest.failedStatuses.delete.after.csv")
    void testDeleteCheckStatuses() {
        FunctionalTestHelper.delete(String.format(DELETE_URL, baseUrl, 10211, 1000L));
        for (int i = 24; i <= 29; i++) {
            FunctionalTestHelper.delete(String.format(DELETE_URL, baseUrl, i, 1000L + i));
        }
    }
}
