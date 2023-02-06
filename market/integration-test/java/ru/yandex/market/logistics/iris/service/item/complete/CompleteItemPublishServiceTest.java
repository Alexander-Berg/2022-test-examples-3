package ru.yandex.market.logistics.iris.service.item.complete;

import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.service.mdm.publish.complete.item.CompleteItemPublishService;

public class CompleteItemPublishServiceTest extends AbstractContextualTest {

    @Autowired
    private CompleteItemPublishService service;

    /**
     * Проверяем, что при вызове синхронного синка с пустой входной коллекцией и пустой БД:
     * 1. Не будет ошибки при вызове
     * 2. Не произойдет никаких изменений в таблицах item, complete_item
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/complete_item_publish_service/empty_item.xml")
    @DatabaseSetup(value = "classpath:fixtures/setup/complete_item_publish_service/empty_complete_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/setup/complete_item_publish_service/empty_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/setup/complete_item_publish_service/empty_complete_item.xml")
    public void syncPublishOfEmptyCollectionAndEmptyDatabase() {
        service.publish(Collections.emptySet());
    }

    /**
     * Проверяем, что при вызове синхронного синка с пустой входной коллекцией и заполненной БД:
     * 1. Не будет ошибки при вызове
     * 2. Не произойдет никаких изменений в таблицах item, complete_item
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/complete_item_publish_service/filled_item.xml")
    @DatabaseSetup(value = "classpath:fixtures/setup/complete_item_publish_service/empty_complete_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/setup/complete_item_publish_service/filled_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/setup/complete_item_publish_service/empty_complete_item.xml")
    public void syncPublishOfEmptyCollectionAndFilledDatabase() {
        service.publish(Collections.emptySet());
    }

    /**
     * Проверяем, что при вызове синхронного синка с входной коллекцией из неизвестных идентификаторов товаров и заполненной БД:
     * 1. Не будет ошибки при вызове
     * 2. Не произойдет никаких изменений в таблицах item, complete_item
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/complete_item_publish_service/filled_item.xml")
    @DatabaseSetup(value = "classpath:fixtures/setup/complete_item_publish_service/filled_complete_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/setup/complete_item_publish_service/filled_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/setup/complete_item_publish_service/filled_complete_item.xml")
    public void syncPublishOfUnknownIdentifiersAndFilledDatabase() {
        Set<EmbeddableItemIdentifier> identifiers = Sets.newHashSet(
            new EmbeddableItemIdentifier("unknown1", "unknown1"),
            new EmbeddableItemIdentifier("unknown2", "unknown2")
        );

        service.publish(identifiers);
    }

    /**
     * Проверяем, что при вызове синхронного синка с входной коллекцией из известных идентификаторов товаров и заполненной БД:
     * Данные появятся только для тех товаров, у которых были конвертируемые в МДМ аттрибуты.
     * Данные, которые уже лежали в complete_item останутся нетронутыми.
     * Данные, по которым конвертация прошла неуспешно не будут обновлены в complete_item.
     * <p>
     * В частности в данном тесте данные должны быть изменены только для товара  sku = "1", partner = 1
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/complete_item_publish_service/sync_publish/item.xml")
    @DatabaseSetup("classpath:fixtures/setup/complete_item_publish_service/sync_publish/complete_item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/expected/complete_item_publish_service/sync_publish/item.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "classpath:fixtures/expected/complete_item_publish_service/sync_publish/complete_item.xml")
    public void syncPublish() {
        Set<EmbeddableItemIdentifier> identifiers = Sets.newHashSet(
            new EmbeddableItemIdentifier("unknown1", "unknown1"),
            new EmbeddableItemIdentifier("1", "1"),
            new EmbeddableItemIdentifier("unknown2", "unknown2"),
            new EmbeddableItemIdentifier("10", "10")
        );

        service.publish(identifiers);
    }
}