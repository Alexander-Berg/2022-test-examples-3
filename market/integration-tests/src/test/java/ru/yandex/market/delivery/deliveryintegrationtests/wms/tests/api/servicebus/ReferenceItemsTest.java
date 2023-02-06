package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.servicebus;

import io.qameta.allure.Epic;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.RadiatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;

import java.util.concurrent.TimeUnit;

@Resource.Classpath("wms/test.properties")
@DisplayName("Service-Bus API: ReferenceItems")
@Epic("API Tests")
public class ReferenceItemsTest {
    private static final Logger log = LoggerFactory.getLogger(ReferenceItemsTest.class);

    @Property("test.vendorId")
    private long vendorId;

    private final RadiatorClient radiatorClient = new RadiatorClient();
    private final ServiceBus serviceBus = new ServiceBus();
    private final String itemPrefix = "root.response.itemReferences.itemReference.item.";

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    @Test
    @Tag("notForMultitesting")
    @DisplayName("putReferenceItems: Создание нового товара")
    public void putReferenceItemsTest() {
        log.info("Testing putReferenceItems");

        String article = UniqueId.getString();

        serviceBus.putReferenceItems(vendorId, article);

        radiatorClient.getReferenceItems(vendorId, article)
                .body(itemPrefix + "unitId.vendorId", Matchers.is(String.valueOf(vendorId)))
                .body(itemPrefix + "unitId.article", Matchers.is(article))
                .body(itemPrefix + "count", Matchers.is("0"));
    }

    @Test
    @Tag("notForMultitesting")
    @DisplayName("putReferenceItems: Обновление описания товара")
    public void putReferenceItemsWithRandomName() {
        log.info("Testing putReferenceItemsWithRandomName");

        String randomName = UniqueId.getString();
        String article = UniqueId.getString();

        serviceBus.putReferenceItems(vendorId, article); //создаем новый товар
        serviceBus.putReferenceItems(vendorId, article, randomName); //обновляем имя созданного товара

        radiatorClient.refreshReferenceCache();

        Retrier.clientRetry(() -> radiatorClient.getReferenceItems(vendorId, article)
                        .body(itemPrefix + "name",
                                Matchers.is(randomName)),
                Retrier.RETRIES_MEDIUM,
                Retrier.TIMEOUT_TINY,
                TimeUnit.SECONDS
        );
    }
}
