package ru.yandex.market.adv.shop.integration.metrika.service.metrika.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationMockServerTest;
import ru.yandex.market.adv.shop.integration.metrika.factory.counter.CounterRootFactory;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.ContactInfo;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.Shopsdat;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.YtBusinessInfo;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.metrika.internal.client.model.CounterRoot;

@DisplayName("Тесты на RateLimiter в сервисе MetrikaInternalServiceImpl")
@MockServerSettings(ports = {12233, 12235})
class MetrikaInternalServiceTest extends AbstractShopIntegrationMockServerTest {

    @Autowired
    private MetrikaInternalService metrikaInternalService;

    @Autowired
    private CounterRootFactory counterRootFactory;

    MetrikaInternalServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("createCounter: обработка двух последовательных запросов прошла успешно")
    @DbUnitDataSet(after = "MetrikaInternalServiceTest/csv/businessMetrikaCounter_twoRows_success.csv")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_createCounter_twoRequests_success_business"
            ),
            before = "MetrikaInternalServiceTest/json/yt_businessInfo.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_createCounter_twoRequests_success_shopsdat"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_createCounter_twoRequests_success_mbi_contact_all_info"
            )
    )
    @Test
    void createCounter_twoRequests_success() {

        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_11",
                "counter_11_success", Map.of());
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_12",
                "counter_12_success", Map.of());

        run("business_createCounter_twoRequests_success_",
                () -> {
                    CounterRoot counterRoot11 = counterRootFactory.create(11L);
                    CounterRoot counterRoot12 = counterRootFactory.create(12L);
                    metrikaInternalService.createCounter(counterRoot11, 11L);
                    metrikaInternalService.createCounter(counterRoot12, 12L);
                }
        );
    }

    @DisplayName("createCounter: исключительная ситуация при обработке трех запросов одновременно")
    @DbUnitDataSet()
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_createCounter_threeRequests_exception_business"
            ),
            before = "MetrikaInternalServiceTest/json/yt_businessInfo.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_createCounter_threeRequests_exception_shopsdat"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_createCounter_threeRequests_exception_" +
                            "mbi_contact_all_info"
            )
    )
    @Test
    void createCounter_threeRequests_exception() {

        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_11",
                "counter_11_success", Map.of());
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_12",
                "counter_12_success", Map.of());
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_13",
                "counter_13_success", Map.of());

        run("business_createCounter_threeRequests_exception_",
                () -> {
                    Map<Long, CounterRoot> mapCounterRoot = Map.of(
                            11L, counterRootFactory.create(11L),
                            12L, counterRootFactory.create(12L),
                            13L, counterRootFactory.create(13L)
                    );
                    Assertions.assertThatThrownBy(
                            () -> LongStream.of(11L, 12L, 13L)
                                    .parallel()
                                    .forEach(c -> metrikaInternalService.createCounter(mapCounterRoot.get(c), c))
                    ).isInstanceOf(RequestNotPermitted.class);
                }
        );
    }

    @DisplayName("updateCounter: обработка двух последовательных запросов прошла успешно")
    @DbUnitDataSet(
            before = {
                    "MetrikaInternalServiceTest/csv/businessMetrikaCounter_twoRows_success.csv",
                    "MetrikaInternalServiceTest/csv/businessMetrikaDirect_twoRows_success.csv"
            },
            after = {
                    "MetrikaInternalServiceTest/csv/businessMetrikaCounter_twoRows_success.csv",
                    "MetrikaInternalServiceTest/csv/businessMetrikaDirect_twoRows_success.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_updateCounter_twoRequests_success_business"
            ),
            before = "MetrikaInternalServiceTest/json/yt_businessInfo.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_updateCounter_twoRequests_success_shopsdat"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_updateCounter_twoRequests_success_mbi_contact_all_info"
            )
    )
    @Test
    void updateCounter_twoRequests_success() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1", "updateCounter_correctData_11",
                "counter_11_success", Map.of());
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2", "updateCounter_correctData_12",
                "counter_12_success", Map.of());

        run("business_updateCounter_twoRequests_success_",
                () -> {
                    CounterRoot counterRoot11 = counterRootFactory.create(11L);
                    CounterRoot counterRoot12 = counterRootFactory.create(12L);
                    metrikaInternalService.updateCounter(counterRoot11);
                    metrikaInternalService.updateCounter(counterRoot12);
                }
        );
    }

    @DisplayName("updateCounter: исключительная ситуация при обработке трех запросов одновременно")
    @DbUnitDataSet(
            before = {
                    "MetrikaInternalServiceTest/csv/businessMetrikaCounter_twoRows_success.csv",
                    "MetrikaInternalServiceTest/csv/businessMetrikaDirect_twoRows_success.csv"
            },
            after = {
                    "MetrikaInternalServiceTest/csv/businessMetrikaCounter_twoRows_success.csv",
                    "MetrikaInternalServiceTest/csv/businessMetrikaDirect_twoRows_success.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_updateCounter_threeRequests_exception_business"
            ),
            before = "MetrikaInternalServiceTest/json/yt_businessInfo.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_updateCounter_threeRequests_exception_shopsdat"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_updateCounter_threeRequests_exception_" +
                            "mbi_contact_all_info"
            )
    )
    @Test
    void updateCounter_threeRequests_exception() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1", "updateCounter_correctData_11",
                "counter_11_success", Map.of());
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2", "updateCounter_correctData_12",
                "counter_12_success", Map.of());

        run("business_updateCounter_threeRequests_exception_",
                () -> {
                    Map<Long, CounterRoot> mapCounterRoot = Map.of(
                            11L, counterRootFactory.create(11L),
                            12L, counterRootFactory.create(12L)
                    );
                    Assertions.assertThatThrownBy(
                            () -> LongStream.of(11L, 12L, 11L)
                                    .parallel()
                                    .forEach(c -> metrikaInternalService.updateCounter(mapCounterRoot.get(c)))
                    ).isInstanceOf(RequestNotPermitted.class);
                }
        );
    }

    private void mockPathMetrika(String method, String path, String requestFile, String responseFile,
                                 Map<String, List<String>> parameters) {
        mockServerPath(
                method,
                path,
                requestFile == null ? null : "MetrikaInternalServiceTest/json/request/" + requestFile + ".json",
                parameters,
                200,
                "MetrikaInternalServiceTest/json/response/" + responseFile + ".json"
        );
    }
}
