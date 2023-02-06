package ru.yandex.market.mbi.api.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.db.DbUnitTester;
import ru.yandex.market.common.test.db.InitByDbUnitListener;
import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.test.context.TestPropertiesInitializer;
import ru.yandex.market.mbi.api.client.RestMbiApiClient;
import ru.yandex.market.mbi.api.controller.shop.ShopFunctionalTest;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;

/**
 * Базовый класс для функциональных тестов mbi-api.
 * Все функциональные тесты должны от него наследоваться.
 * При использовании этого класса поднимается конфиг, максимально приближенный к продовому,
 * сконфигурирован autowiring в тестах, сконфигурирован db-unit, сконфигурирован mbi-api-client.
 * Порт jetty серверу проставляется автоматически при запуске из незанятых. Для обращения к ручкам /mbi-api
 * рекомендуется пользоваться клиентом, пример смотрите в {@link ShopFunctionalTest}.
 *
 * <p>Если ваш тест не запускается с этим базовым классом, по причине того, что в конфиге нет нужных вам таблиц,
 * или замокан, или не замокан сервис, который вам нужен, или еще что-то, то не нужно отказываться от этого класса и
 * создавать свои конфиги. Нужно допиливать используемые тут конфиги.</p>
 *
 * <p>Класс намерянно не использует dirty-контекст, чтобы не понимать для каждого теста конфиг, ибо это занимает
 * некоторое время. Нужно писать тесты так, чтобы они работали в едином контексте.</p>
 *
 * @author Vadim Lyalin
 */
@SpringJUnitConfig(
        locations = "classpath:/ru/yandex/market/mbi/api/config/functional-test-context.xml",
        initializers = {
                TestPropertiesInitializer.class,
                ForgetfulSuppliersInitializer.class
        }
)
@ActiveProfiles(profiles = {
        "functionalTest",
        "development"
})
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends JupiterDbUnitTest {
    protected static final String URL_PREFIX = "http://localhost:";
    @InitByDbUnitListener
    protected DbUnitTester dbUnitTester;
    @Resource
    protected int port;
    @Autowired
    protected RestMbiApiClient mbiApiClient;
    @Autowired
    protected PartnerNotificationClient partnerNotificationClient;

    private Map<String, MbiOpenApiClient> mbiOpenApiClientCache = new ConcurrentHashMap<>();

    @BeforeEach
    void commonSetUp() {
        mbiApiClient.setServiceUrl(baseUrl());
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient);
    }

    public MbiOpenApiClient getMbiOpenApiClient() {
        return mbiOpenApiClientCache.computeIfAbsent(baseUrl(),
                url -> MbiOpenApiClient.newBuilder()
                        // чтобы дебажить
                        .readTimeout(10, TimeUnit.MINUTES)
                        .baseUrl(url)
                        .build());
    }

    protected String baseUrl() {
        return URL_PREFIX + port;
    }
}
