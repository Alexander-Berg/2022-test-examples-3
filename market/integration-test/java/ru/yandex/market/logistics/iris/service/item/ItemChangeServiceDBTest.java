package ru.yandex.market.logistics.iris.service.item;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.dummy.TestPredefinedField;
import ru.yandex.market.logistics.iris.core.index.field.FieldBuilder;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemEntity;
import ru.yandex.market.logistics.iris.repository.ItemChangeRepository;
import ru.yandex.market.logistics.iris.service.item.itemChangeService.ItemChangeServiceDB;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;
import ru.yandex.market.logistics.iris.utils.query.JpaQueriesCount;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ItemChangeServiceDBTest extends AbstractContextualTest {

    @Autowired
    private ItemChangeServiceDB itemChangeService;
    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;
    @SpyBean
    private SystemPropertyService systemPropertyService;
    @Autowired
    private ItemChangeRepository itemChangeRepository;

    @After
    public void reset() {
        Mockito.reset(itemChangeRepository);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/1.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/success_save_with_default_enabled_save_items_changes_data.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successSaveWithDefault() {
        saveData();
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/with_enabled_save_items_changes_data.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/with_enabled_save_items_changes_data.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successSaveWithEnabledSaveItemsChangesData() {
        saveData();
    }

    //Без батча было бы 7 запросов
    @Test
    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:fixtures/setup/item_change/with_enabled_save_items_changes_data.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/success_save_with_batch.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successSaveWithBatch() {
        RequestContextHolder.setContext(new RequestContext("111"));
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        ZonedDateTime dateTime = ZonedDateTime
                .of(LocalDateTime.of(2011, 12, 3, 10, 15, 30), ZoneId.of("-05:00"));

        index.set(TestPredefinedField.DUMMY, "dummy_value", dateTime);
        index.set(TestPredefinedField.YUMMY, "yummy_value", dateTime);
        index.set(TestPredefinedField.GUMMY, "gummy_value", dateTime);
        index.set(FieldBuilder.builder("bummy", String.class)
                .isNullable(false)
                .build(), "bummy_value", dateTime);
        index.set(FieldBuilder.builder("mummy", String.class)
                .isNullable(false)
                .build(), "mummy_value", dateTime);

        itemChangeService.saveItemChanges(getItemEntity(), index, Collections.emptyList());
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/with_disabled_save_items_changes_data.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/with_disabled_save_items_changes_data.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void saveNothingWithDisabledSaveItemsChangesData() {
        saveData();
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/delete_old_data_with_default_limit.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/delete_old_data_with_default_limit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successDeleteOldDataWithDefaultLimit() {
        LocalDateTime deadline = LocalDateTime.of(2020, 2, 2, 13, 18);
        itemChangeService.deleteOldData(
                deadline);
        verify(systemPropertyService).getIntegerProperty(SystemPropertyIntegerKey.LIMIT_FOR_ITEM_CHANGE_DELETE_BATCH);
        verify(itemChangeRepository).deleteAllByCreatedBefore(deadline, 50000);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/delete_old_data_with_custom_limit.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/delete_old_data_with_custom_limit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successDeleteOldDataWithCustomLimit() {
        LocalDateTime deadline = LocalDateTime.of(2020, 2, 2, 13, 18);
        itemChangeService.deleteOldData(deadline);
        verify(systemPropertyService).getIntegerProperty(SystemPropertyIntegerKey.LIMIT_FOR_ITEM_CHANGE_DELETE_BATCH);
        verify(itemChangeRepository, times(3)).deleteAllByCreatedBefore(deadline, 1);
    }

    private void saveData() {
        RequestContextHolder.setContext(new RequestContext("111"));
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(TestPredefinedField.DUMMY, "dummy_value", ZonedDateTime
                .of(LocalDateTime.of(2011, 12, 3, 10, 15, 30), ZoneId.of("-05:00")));

        itemChangeService.saveItemChanges(getItemEntity(), index, Collections.emptyList());
    }

    private ItemEntity getItemEntity() {
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setId(1L);
        itemEntity.setNaturalKey(new EmbeddableItemNaturalKey(
                new EmbeddableItemIdentifier("10790136", "167526"),
                new EmbeddableSource("1", SourceType.MDM)));
        return itemEntity;
    }
}
