package ru.yandex.market.logistics.iris.service.item;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.change.ChangeType;
import ru.yandex.market.logistics.iris.core.index.dummy.TestPredefinedField;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemChangeEntity;
import ru.yandex.market.logistics.iris.entity.item.ItemEntity;
import ru.yandex.market.logistics.iris.entity.item.MetaInformationField;
import ru.yandex.market.logistics.iris.service.item.itemChangeService.ItemChangeServiceYT;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ItemChangeServiceYtTest extends AbstractContextualTest {

    @Autowired
    private ItemChangeServiceYT itemChangeServiceYT;
    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;

    @Before
    public void init() {
        when(yt.tables()).thenReturn(Mockito.mock(YtTables.class));
        RequestContextHolder.clearContext();
    }

    @After
    public void destroy() {
        Mockito.reset(yt.tables());
    }

    @Test
    public void saveWithDefault() {
        List<ItemChangeEntity> itemChangeEntities = save();
        assertTrue(itemChangeEntities.isEmpty());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_change/3.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/success_save_item_change_for_yt.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void saveWithEnabledSaveItemsChangesToYT() {
        List<ItemChangeEntity> itemChangeEntities = save();
        assertFalse(itemChangeEntities.isEmpty());
    }

    private List<ItemChangeEntity> save() {
        RequestContextHolder.setContext(new RequestContext("111"));
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(TestPredefinedField.DUMMY, "dummy_value", ZonedDateTime
                .of(LocalDateTime.of(2011, 12, 3, 10, 15, 30), ZoneId.of("-05:00")));

        return itemChangeServiceYT.saveItemChanges(getItemEntity(), index, Collections.emptyList());
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
