package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.LinkedMultiValueMap;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.EntityType;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.service.services.MarschrouteServices;

import java.time.LocalDate;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MarschrouteServicesSyncFunctionalTest extends RepositoryTest {

    @Autowired
    private MarschrouteServices services;


    /**
     * Сценарий #1:
     * <p>
     * Проверяет сценарий, в котором и в БД Прослойки и в БД Маршрута отсутствовали данные по услугам
     * за указанный день.
     * <p>
     * В результате исполнения таблицы marschroute_service/marschroute_raw_service должны остаться пустыми.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/1/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/1/expected_database.xml",
            assertionMode = NON_STRICT)
    void noServicesStoredAndAvailable() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/1/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #2:
     * <p>
     * Проверяет сценарий, в котором и в БД прослойки отсутствует информация об услугах за указанный день,
     * но в ответе от Маршрута было возвращено несколько услуг.
     * <p>
     * В результате исполнения таблицы marschroute_service/marschroute_raw_service должны быть заполнены этими услугами.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/2/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/2/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void noServicesStoredAndSomeAvailable() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/2/services_orders.json");

        LinkedMultiValueMap<String, String> ordersArguments = new LinkedMultiValueMap<>();
        ordersArguments.add("filter[order_id][]", "EXT000000001");
        ordersArguments.add("filter[order_id][]", "EXT000000000");

        FulfillmentInteraction ordersInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(singletonList("orders"), HttpMethod.GET, ordersArguments))
                .setResponsePath("functional/marschroute_services/2/orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .thenMockFulfillmentRequest(ordersInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #3:
     * <p>
     * Проверяет сценарий, в котором и в БД прослойки присутствовала информация об услугах за указанный день,
     * но в ответе от Маршрута этих услуг не оказалось.
     * <p>
     * В результате исполнения таблицы marschroute_service/marschroute_raw_service должны быть
     * созданы новые версии этих услуг, в которых сумма услуги в итоге оказалась равна 0.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/3/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/3/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void someServicesStoredAndNoneAvailable() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/3/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }


    /**
     * Сценарий #4:
     * <p>
     * Проверяет сценарий, в котором и в БД прослойки присутствовала информация об услугах за указанный день,
     * и в ответе от Маршрута оказались те же самые услуги со значениями, аналогичными значениям в БД.
     * <p>
     * В результате исполнения запроса значения в БД должны остаться не тронутыми.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/4/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/4/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void identicalStoredAndAvailable() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/4/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #5:
     * <p>
     * Проверяет сценарий, в котором и в БД прослойки присутствовала информация об услугах за указанный день,
     * но в ответе вернулась информация по другим услугам.
     * <p>
     * В результате исполнения запроса:
     * Уже существующая в БД услуга должна обнулиться контр услугой.
     * Новая услуга из Маршрута должна быть добавлена в БД.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/5/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/5/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void differentStoredAndAvailable() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        LinkedMultiValueMap<String, String> ordersArguments = new LinkedMultiValueMap<>();
        ordersArguments.add("filter[order_id]", "EXT000000001");

        FulfillmentInteraction ordersInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(singletonList("orders"), HttpMethod.GET, ordersArguments))
                .setResponsePath("functional/marschroute_services/5/orders.json");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/5/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .thenMockFulfillmentRequest(ordersInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #6:
     * <p>
     * Проверяет сценарий, в котором и в БД прослойки присутствовала информация об услугах за указанный день,
     * но в ответе вернулась информация по той же самой услуге,
     * но с отличным значением serviceDateTime (разница в микросекундах).
     * <p>
     * В результате исполнения запроса:
     * Уже существующая в БД услуга должна обнулиться контр услугой.
     * Новая услуга из Маршрута должна быть добавлена в БД.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/6/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/6/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void sameServiceStoredAndAvailableButWithDifferentMicroseconds() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/6/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #7:
     * В БД сохранено несколько услуг с типом UNKNOWN, но производим sync по услугам с типом ORDER.
     * В маршруте за указанный день услуг с типом ORDER не оказалось.
     * <p>
     * В результате исполнения услуги с типом UNKNOWN не должны занулиться.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/7/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/7/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void servicesWithDifferentTypeStoredAndNoneAvailable() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/7/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #8:
     * В БД Маршрута сохранена услуга, которая имеет сумму равную 150.
     * В какой то момент услуга исчезла из Маршрута и нами была сгенерированна корректировка на -150 рублей.
     * <p>
     * Сценарий проверяет, что при повторном хождении в Маршрут и не обнаружении этой услуги в дальнейшем
     * состояние БД останется AS IS.
     * <p>
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/marschroute_services/8/setup_database.xml",
            type = DatabaseOperation.CLEAN_INSERT)
    @ExpectedDatabase(value = "classpath:functional/marschroute_services/8/expected_database.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void discoveryOfAlreadyDeletedService() throws Exception {
        LinkedMultiValueMap<String, String> ordersServicesArguments = new LinkedMultiValueMap<>();
        ordersServicesArguments.add("filter[date]", "01.01.2018");

        FulfillmentInteraction ordersServicesInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("services", "orders"), HttpMethod.GET, ordersServicesArguments))
                .setResponsePath("functional/marschroute_services/8/services_orders.json");

        FunctionalTestScenarioBuilder.start()
                .run(() -> services.syncServices(EntityType.ORDER, LocalDate.of(2018, 1, 1)))
                .thenMockFulfillmentRequest(ordersServicesInteraction)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }
}
