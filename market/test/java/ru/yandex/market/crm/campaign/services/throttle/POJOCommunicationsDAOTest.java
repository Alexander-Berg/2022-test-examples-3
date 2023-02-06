package ru.yandex.market.crm.campaign.services.throttle;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Хоть класс {@link POJOCommunicationsDAO} и тестовый, его поведение должно соответствовать
 * обозначенному контракту. Это соответствие и проверяет данный тест.
 *
 * @author zloddey
 */
public class POJOCommunicationsDAOTest {
    private final POJOCommunicationsDAO dao = new POJOCommunicationsDAO();

    @ParameterizedTest
    @EnumSource(CommunicationsDAOContract.class)
    void contractCompliance(CommunicationsDAOContract contract) {
        contract.verify(dao);
    }
}
