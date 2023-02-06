package ru.yandex.market.partner.test.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;

/**
 * Базовый класс для функциональных тестов mbi-partner.
 * ВСЕ тесты использующий Spring context ДОЛЖНЫ наследоваться от него.
 *
 * @author Vladislav Bauer
 */
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends EmptyDbFunctionalTest {
    /**
     * Маппер нужно использовать для ручной сериализации тела запросов, в которых есть коллекции. Без этого в json имя
     * свойства для коллекции берется из {@link javax.xml.bind.annotation.XmlElement}, а не
     * {@link javax.xml.bind.annotation.XmlElementWrapper}
     */
    protected static final ObjectMapper OBJECT_MAPPER = new ApiObjectMapperFactory().createJsonMapper();

    /**
     * Метод для тех случаев, когда нужно очистить БД один раз перед большой пачкой тестов,
     * а между ними ничего делать не надо,
     *
     * @return список таблиц, которые очищать не надо
     */
    public static String[] nonTruncatedTables() {
        return FunctionalTest.class
                .getAnnotation(PreserveDictionariesDbUnitDataSet.class)
                .annotationType().getAnnotation(DbUnitDataSet.class)
                .nonTruncatedTables();
    }

    @Autowired
    protected PartnerNotificationClient partnerNotificationClient;

    @BeforeEach
    void commonSetUp() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient);
    }
}
