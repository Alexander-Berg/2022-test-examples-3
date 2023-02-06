package ru.yandex.market.jmf.dataimport.test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.dataimport.DataImportException;
import ru.yandex.market.jmf.dataimport.DataImportService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.db.test.CleanDb;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.HasModificationTime;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.tx.TxService;

@SpringJUnitConfig(InternalDataImportTestConfiguration.class)
@Transactional
@CleanDb
public class DataImportTest {

    @Inject
    private DataImportService dataImportService;
    @Inject
    private DbService dbService;
    @Inject
    private BcpService bcpService;
    @Inject
    private TxService txService;

    /**
     * Проверяет правильность импортирования иерархической структуры, при этом правильность не зависит от
     * последовательности элементов в файле с импортируемыми данными и количества потоков импорта.
     * Список объектов см. {@code ou1.json}.
     */
    @Test
    public void ou1() {
        // Вызов системы
        dataImportService.execute("classpath:/ou1.import.xml");

        // Проверка утверждений
        List<Entity> ous = dbService.list(Query.of(Fqn.parse("ou")));
        Assertions.assertEquals(4, ous.size(), "Импортируемые данные должны содержать 4 отдела");

        Entity parent0 = getOuParent("0");
        Assertions.assertNull(parent0, "Это элемент из корня иерархии. Родитель должен отстутствовать");

        Long staffId1 = getOuParentStaffId("1");
        Assertions.assertEquals(Long.valueOf(0), staffId1, "Неверный родитель");

        Long staffId2 = getOuParentStaffId("2");
        Assertions.assertEquals(Long.valueOf(0), staffId2, "Неверный родитель");

        Long staffId22 = getOuParentStaffId("22");
        Assertions.assertEquals(Long.valueOf(2), staffId22, "Неверный родитель");

        List<Entity> employees = dbService.list(Query.of(Fqn.parse("employee")));
        Assertions.assertEquals(5, employees.size(), "Импортируемые данные должны содержать 5 сотрудников");

        List<Entity> dismissedEmployees = employees.stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getAttribute("archived")))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, dismissedEmployees.size(), "Неверное кол-во архивных сотрудников");
        Assertions.assertEquals("40663", dismissedEmployees.get(0).getAttribute("staffId"));

    }

    /**
     * Проверяет правильность импортирования из CSV файла.
     * Список объектов см. {@code ticket.csv}.
     */
    @Test
    public void ticket() {
        // Проверка утверждений
        List<Entity> tickets = dbService.list(Query.of(Fqn.parse("ticket")));
        Assertions.assertEquals(0, tickets.size(), "Изначально число обращений должно равняться 0");

        // Вызов системы
        dataImportService.execute("classpath:/ticket.import.xml");

        // Проверка утверждений
        tickets = dbService.list(Query.of(Fqn.parse("ticket")));
        Assertions.assertEquals(22, tickets.size(), "Импортируемые данные должны содержать 22 обращения");
    }

    /**
     * Проверяет правильность импортирования из CSV файла в кодировке UTF-8 with BOM.
     * Список объектов см. {@code ticket-utf-8-bom.csv}.
     */
    @Test
    public void ticketUtf8Bom() {
        // Проверка утверждений
        List<Entity> tickets = dbService.list(Query.of(Fqn.parse("ticket$extra")));
        Assertions.assertEquals(0, tickets.size(), "Изначально число обращений должно равняться 0");

        // Вызов системы
        dataImportService.execute("classpath:/ticket-utf-8-bom.import.xml");

        // Проверка утверждений
        tickets = dbService.list(Query.of(Fqn.parse("ticket$extra")));
        Assertions.assertEquals(22, tickets.size(), "Импортируемые данные должны содержать 22 обращения");
    }

    /**
     * Проверяем правильность импорта из XML
     */
    @Test
    public void xmlSourceTest() {
        // Вызов системы
        dataImportService.execute("classpath:/ou_xml.import.xml");

        // Проверка утверждений
        Entity rootParent = getOuParent("7771");
        Assertions.assertNull(rootParent, "OU со staffId=7771 в ou.xml указано без parent");

        String title1 = getOuAttribute("7771", "title");
        Assertions.assertEquals("Ou name 1", title1, "Название должно совпадать с указанным в ox.xml");

        Long ouParent = getOuParentStaffId("7772");
        Assertions.assertEquals(Long.valueOf(7771), ouParent, "OU со staffId=7772 в ou.xml указано parent=7771");

        String title2 = getOuAttribute("7772", "title");
        Assertions.assertEquals("Ou name 2", title2, "Название должно совпадать с указанным в ox.xml");
    }

    /**
     * Проверяем правильность импорта из Excel
     */
    @Test
    public void excelSourceTest() {
        // Вызов системы
        dataImportService.execute("classpath:/simple_excel.import.xml");

        // Проверка утверждений
        List<Entity> tickets = dbService.list(Query.of(Fqn.parse("excelTicket$default")));
        Assertions.assertEquals(3, tickets.size(), "Импортируемые данные должны содержать 3 обращения");

        // Дополнительная проверка, что сплит по конкретному символу работает
        EntityCollectionAssert.assertThat(tickets)
                .filteredOn(entity -> entity.<List<String>>getAttribute("categories").size() > 1)
                .as("Должен быть тикет с 2-мя категориями")
                .hasSize(1)
                .as("Deadline must be present")
                .allMatch(entity -> null != entity.getAttribute("deadline"));
    }

    /**
     * Проверяем работоспособность импорта из DELEGATE
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "ou1_delegate.json", "ou1_delegate.csv", "ou1_delegate.xls", "ou1_delegate.xlsx",
            "ou1_delegate.txt" // проверка работы dataSource по умолчанию
    })
    public void delegateSourceTest(String fileName) {
        // Вызов системы
        dataImportService.execute(
                "classpath:/delegateTestFiles/delegate.import.xml",
                Map.of("name", fileName)
        );

        // Проверка утверждений
        List<Entity> tickets = dbService.list(Query.of(Fqn.parse("ou$default")));
        Assertions.assertEquals(4, tickets.size(), "Импортируемые данные должны содержать 4 обращения");
    }

    /**
     * Проверяем корректность обработки неправильно описанного файла импорта:
     * несколько dataSource по умолчанию.
     */
    @Test
    public void poorDelegateSourceTest() {
        Assertions.assertThrows(
                DataImportException.class,
                () -> dataImportService.execute(
                        "classpath:/delegateTestFiles/poor_delegate.import.xml",
                        Map.of("name", "ou1_delegate.txt")
                )
        );
    }

    @Test
    @Transactional(Transactional.TxType.NEVER)
    public void testRemoveNotImportedCustomizer() {
        txService.runInTx(() -> {
            bcpService.create(Fqn.of("removeCustomizerTest"), Map.of(
                    "naturalId", 10
            ));
            bcpService.create(Fqn.of("removeCustomizerTest"), Map.of(
                    "naturalId", 2
            ));
        });

        var now = Now.withMoment(Now.offsetDateTime().toInstant(),
                () -> Now.withOffset(Duration.ofSeconds(5), () -> {
                    dataImportService.execute("classpath:removeCustomizerTest.import.xml");
                    return Now.offsetDateTime();
                }));

        txService.runInTx(() -> {
            var entitiesToBeRemoved = dbService.list(Query.of(Fqn.of("removeCustomizerTest"))
                    .withFilters(Filters.eq("naturalId", 10)));

            var existingEntities = dbService.list(Query.of(Fqn.of("removeCustomizerTest"))
                    .withFilters(Filters.in("naturalId", List.of(1, 2, 3, 4))));

            EntityCollectionAssert.assertThat(entitiesToBeRemoved)
                    .isEmpty();

            EntityCollectionAssert.assertThat(existingEntities)
                    .hasSize(4)
                    .filteredOn(e -> e.<Long>getAttribute("naturalId") == 2L)
                    .element(0)
                    .extracting(e -> e.<OffsetDateTime>getAttribute(HasModificationTime.MODIFICATION_TIME))
                    // truncated to millis, because our attribute by default is truncated to millis
                    .isEqualTo(now.truncatedTo(ChronoUnit.MILLIS));
        });
    }

    private <T> T getOuAttribute(String staffId, String attributeCode) {
        Entity entity = dbService.getByNaturalId(Fqn.of("ou"), "staffId", staffId);
        return entity.getAttribute(attributeCode);
    }

    private Entity getOuParent(String staffId) {
        return getOuAttribute(staffId, "parent");
    }

    private Long getOuParentStaffId(String staffId) {
        Entity entity = dbService.getByNaturalId(Fqn.of("ou"), "staffId", staffId);
        Entity parent = entity.getAttribute("parent");
        return parent.getAttribute("staffId");
    }
}
