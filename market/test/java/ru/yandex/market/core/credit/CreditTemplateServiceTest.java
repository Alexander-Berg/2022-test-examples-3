package ru.yandex.market.core.credit;

import java.util.List;
import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Функциональные тесты на {@link CreditTemplateServiceImpl}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "CreditTemplateServiceTest.before.csv")
class CreditTemplateServiceTest extends FunctionalTest {

    private static final CreditTemplate EXPECTED_1 = new CreditTemplate.Builder()
            .setId(1000L)
            .setPartnerId(1)
            .setPartnerTemplateId(1L)
            .setBankId(1L)
            .setMaxTermMonths(12)
            .setMinRateScaled((int) (12.4 * CreditTemplateValidator.RATE_POW))
            .setMinPrice(5000L)
            .setMaxPrice(99999L)
            .setConditionsUrl("http://conditions.uk")
            .setType(CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE)
            .setCreditOrganizationType(CreditOrganizationType.BANK)
            .build();

    private static final CreditTemplate EXPECTED_2 = new CreditTemplate.Builder()
            .setId(2000L)
            .setPartnerId(1)
            .setPartnerTemplateId(2L)
            .setBankId(2L)
            .setMaxTermMonths(6)
            .setMinRateScaled(0)
            .setMinPrice(null)
            .setMaxPrice(null)
            .setConditionsUrl("https://conditions.uk/smth")
            .setType(CreditTemplateType.FEED)
            .setCreditOrganizationType(CreditOrganizationType.BANK)
            .build();

    private static final long UID = -1;

    @Autowired
    CreditTemplateService creditTemplateService;

    private static CreditTemplate.Builder createNewTemplateBuilder() {
        return new CreditTemplate.Builder()
                .setPartnerId(3)
                .setBankId(3L)
                .setMaxTermMonths(6)
                .setMinRateScaled((int) (21.8 * CreditTemplateValidator.RATE_POW))
                .setMinPrice(3000L)
                .setMaxPrice(30000L)
                .setConditionsUrl("http://conditions.uk/new_template")
                .setType(CreditTemplateType.DEFAULT_FOR_ALL_IN_RANGE)
                .setCreditOrganizationType(CreditOrganizationType.BANK);
    }

    //тесты на получение
    @Test
    @DisplayName("Получение кредитного шаблона по его идентификатору")
    void testFindById() {
        Optional<CreditTemplate> actual = creditTemplateService.findById(1000, 1);
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(EXPECTED_1, actual.get());
    }

    @Test
    @DisplayName("Получение кредитных шаблонов по идентификатору магазина")
    void testFindByPartnerId() {
        List<CreditTemplate> byPartnerId = creditTemplateService.findByPartnerId(1);
        Assertions.assertEquals(2, byPartnerId.size());
        MatcherAssert.assertThat(byPartnerId, Matchers.containsInAnyOrder(EXPECTED_1, EXPECTED_2));
    }

    @Test
    @DisplayName("Получение кредитных шаблонов по идентификатору магазина и магазинному ид. шаблона")
    void testFindByPartnerTemplateId() {
        Optional<CreditTemplate> byPartnerTemplateId = creditTemplateService.findByPartnerTemplateId(1, 2);
        Assertions.assertTrue(byPartnerTemplateId.isPresent());
        Assertions.assertEquals(EXPECTED_2, byPartnerTemplateId.get());
    }

    @Test
    @DisplayName("Нет шаблона с заданным идентификатором")
    void testTemplateNotFound() {
        Assertions.assertFalse(creditTemplateService.findById(9999, 1).isPresent());
    }

    @Test
    @DisplayName("Шаблон с заданным идентификатором не принадлежит этому магазину")
    void testTemplateIsNotYours() {
        Assertions.assertFalse(creditTemplateService.findById(1000, 2).isPresent());
    }

    @Test
    @DisplayName("У магазина нет кредитных шаблонов - возвращается пустой список")
    void testFindByPartnerIdNoTemplates() {
        List<CreditTemplate> byPartnerId = creditTemplateService.findByPartnerId(2);
        Assertions.assertNotNull(byPartnerId);
        Assertions.assertEquals(0, byPartnerId.size());
    }

    //тесты на добавление
    @Test
    @DisplayName("Добавление кредитного шаблона")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.add.after.csv")
    void testAdd() {
        CreditTemplate newTemplate = createNewTemplateBuilder().build();
        long id1 = creditTemplateService.addCreditTemplate(newTemplate, UID);
        Assertions.assertEquals(1L, id1);
        long id2 = creditTemplateService.addCreditTemplate(newTemplate, UID);
        Assertions.assertEquals(2L, id2);
    }

    @Test
    @DisplayName("Добавление кредитного шаблона с заранее заданным идентификатором")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testAddWithId() {
        CreditTemplate newTemplate = createNewTemplateBuilder()
                .setId(3000L)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> creditTemplateService.addCreditTemplate(newTemplate, UID));
        Assertions.assertEquals("Credit template for create must not have id", exception.getMessage());
    }

    //тесты на обновление
    @Test
    @DisplayName("Обновление кредитного шаблона")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.update.after.csv")
    void testUpdate() {
        CreditTemplate template = new CreditTemplate.Builder()
                .setId(EXPECTED_2.getId())
                .setPartnerId(EXPECTED_2.getPartnerId())
                .setPartnerTemplateId(EXPECTED_2.getPartnerTemplateId())
                .setBankId(3L)
                .setMaxTermMonths(12)
                .setMinRateScaled(20 * CreditTemplateValidator.RATE_POW)
                .setConditionsUrl(EXPECTED_2.getConditionsUrl())
                .setMinPrice(EXPECTED_2.getMinPrice())
                .setMaxPrice(EXPECTED_2.getMaxPrice())
                .setType(EXPECTED_2.getType())
                .setCreditOrganizationType(EXPECTED_2.getCreditOrganizationType())
                .build();
        creditTemplateService.updateCreditTemplate(template, UID);
    }

    @Test
    @DisplayName("Обновление кредитного шаблона с несуществующим идентификатором")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testUpdateNonExistent() {
        CreditTemplate newTemplate = createNewTemplateBuilder()
                .setId(3000L)
                .build();
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> creditTemplateService.updateCreditTemplate(newTemplate, UID));
        Assertions.assertEquals("Cannot update credit template: credit template with id 3000 not found",
                exception.getMessage());
    }

    @Test
    @DisplayName("Обновление кредитного шаблона: время обновления поменялось")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.update.after.csv")
    void testUpdatedAt() {
        CreditTemplate before = creditTemplateService
                .findById(EXPECTED_2.getId(), EXPECTED_2.getPartnerId())
                .orElseThrow(IllegalStateException::new);
        Assertions.assertNotNull(before.getUpdatedAt());
        CreditTemplate template = new CreditTemplate.Builder()
                .setId(EXPECTED_2.getId())
                .setPartnerId(EXPECTED_2.getPartnerId())
                .setPartnerTemplateId(EXPECTED_2.getPartnerTemplateId())
                .setBankId(3L)
                .setMaxTermMonths(12)
                .setMinRateScaled(20 * CreditTemplateValidator.RATE_POW)
                .setConditionsUrl(EXPECTED_2.getConditionsUrl())
                .setMinPrice(EXPECTED_2.getMinPrice())
                .setMaxPrice(EXPECTED_2.getMaxPrice())
                .setType(EXPECTED_2.getType())
                .setCreditOrganizationType(EXPECTED_2.getCreditOrganizationType())
                .build();
        creditTemplateService.updateCreditTemplate(template, UID);
        CreditTemplate after = creditTemplateService
                .findById(EXPECTED_2.getId(), EXPECTED_2.getPartnerId())
                .orElseThrow(IllegalStateException::new);
        Assertions.assertNotNull(after.getUpdatedAt());
        Assertions.assertTrue(before.getUpdatedAt().isBefore(after.getUpdatedAt()));
    }

    //тесты на удаление
    @Test
    @DisplayName("Удаление кредитного шаблона")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.delete.after.csv")
    void testDelete() {
        Long id = EXPECTED_1.getId();
        Assertions.assertNotNull(id);
        creditTemplateService.deleteCreditTemplate(id, EXPECTED_1.getPartnerId(), UID);
    }

    @Test
    @DisplayName("Удаление последнего кредитного шаблона переводит фичу в DONT_WANT")
    @DbUnitDataSet(before = "CreditTemplateServiceTest.delete.last.before.csv",
            after = "CreditTemplateServiceTest.delete.last.after.csv")
    void testDeleteTheLastOne() {
        creditTemplateService.deleteCreditTemplate(1001L, 4, UID);
    }

    @Test
    @DisplayName("Удаление кредитного шаблона с несуществующим идентификатором")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testDeleteNonExistent() {
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> creditTemplateService.deleteCreditTemplate(3000L, EXPECTED_1.getPartnerId(), UID));
        Assertions.assertEquals("Cannot delete credit template: credit template with id 3000 not found",
                exception.getMessage());
    }

    @Test
    @DisplayName("Удаление кредитного шаблона не своего магазина")
    @DbUnitDataSet(after = "CreditTemplateServiceTest.before.csv")
    void testDeleteNotYours() {
        Long id = EXPECTED_1.getId();
        Assertions.assertNotNull(id);
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> creditTemplateService.deleteCreditTemplate(EXPECTED_1.getId(), 3, UID));
        Assertions.assertEquals("Cannot delete credit template: credit template with id " +
                EXPECTED_1.getId() + " not found", exception.getMessage());
    }
}
