package ru.yandex.market.logistics.iris.picker;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.logistics.iris.configuration.ReferenceIndexerTestConfiguration;
import ru.yandex.market.logistics.iris.core.domain.item.Item;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.domain.util.ReferenceIndexWithUpdatedDate;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.dummy.TestPredefinedField;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.Value;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.picker.predefined.PriorityBasedFieldValuePicker;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyStringSetKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.logistics.iris.core.domain.source.Source.CONTENT;
import static ru.yandex.market.logistics.iris.core.domain.source.Source.DATACAMP_MSKU;
import static ru.yandex.market.logistics.iris.core.domain.source.Source.DATACAMP_SSKU;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(ReferenceIndexerTestConfiguration.class)
public class PriorityBasedFieldValuePickerTest {

    private static final Source ADMIN = Source.ADMIN_SOURCE;
    private static final Source WH_145 = new Source("145", SourceType.WAREHOUSE);

    private static final ItemIdentifier ITEM_IDENTIFIER = ItemIdentifier.of("id", "sku");
    private static final Source WH_149 = new Source("149", SourceType.WAREHOUSE);

    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;
    @Mock
    private SystemPropertyService systemPropertyService;

    private PriorityBasedFieldValuePicker picker;

    @Before
    public void init() {
        picker = new PriorityBasedFieldValuePicker(
                ImmutableList.of(ADMIN, WH_145), systemPropertyService);
        doReturn(Set.of("datacamp_msku", "datacamp_ssku"))
                .when(systemPropertyService)
                .getStringSetProperty(SystemPropertyStringSetKey.SOURCE_TYPES_TO_FILTER_OUTPUT);
    }


    /**
     * Проверяем, что при попытке выбрать значение поля среди 0 источников -
     * в результате будет возвращено значение Optional.empty();
     */
    @Test
    public void pickAmongZeroSources() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of());

        Optional<Value<String>> pick = picker.pick(TestPredefinedField.DUMMY, item);

        assertThat(pick).isEmpty();
    }

    /**
     * Проверяем, что при попытке выбрать значение поля среди нескольких источников,
     * среди которых только у 1го заполнено это значение - будет возвращено значение именно этого источника.
     */
    @Test
    public void pickAmongSourcesWithPrioritiesButWithoutValues() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of(
                WH_145, new ReferenceIndexWithUpdatedDate(referenceIndexer.createEmptyIndex(), LocalDateTime.now()),
                ADMIN, new ReferenceIndexWithUpdatedDate(referenceIndexer.createEmptyIndex(), LocalDateTime.now()),
                WH_149, index(TestPredefinedField.DUMMY, null)
        ));
        executeExistingValuePickingScenario(item, TestPredefinedField.DUMMY, null);
    }


    /**
     * Проверяем, что при попытке выбрать значение поля среди нескольких источников,
     * среди которых это значение заполнено у двух из них (один считается приоритетным, другой нет)
     * - будет возвращено значение приоритетного источника.
     */
    @Test
    public void pickAmongSourcesWitAndWithoutPriorities() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of(
                WH_145, index(TestPredefinedField.DUMMY, "13"),
                ADMIN, new ReferenceIndexWithUpdatedDate(referenceIndexer.createEmptyIndex(), LocalDateTime.now()),
                WH_149, index(TestPredefinedField.DUMMY, null)
        ));

        executeExistingValuePickingScenario(item, TestPredefinedField.DUMMY, "13");
    }

    /**
     * Проверяем, что при попытке выбрать значение поля среди нескольких источников,
     * среди которых это значение заполнено у всех трех источников,
     * - будет возвращено значение **наиболее** приоритетного источника
     * (определяется порядком в списке источников, передаваемых в конструкторе).
     */
    @Test
    public void pickAmongSourcesWithPriorities() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of(
                WH_145, index(TestPredefinedField.DUMMY, "13"),
                ADMIN, index(TestPredefinedField.DUMMY, null),
                WH_149, index(TestPredefinedField.DUMMY, "17")
        ));

        executeExistingValuePickingScenario(item, TestPredefinedField.DUMMY, null);
    }

    /**
     * Проверка corner-case'а, когда для NON_NULLABLE поля оказалось значение null -
     * в таком случае такой источник считается некорректным и его значение не должны быть выбрано.
     */
    @Test
    public void pickNonNullableFieldFromSourceWithNullValue() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of(
                WH_145, new ReferenceIndexWithUpdatedDate(
                        referenceIndexer.fromJson("{\"gummy\":{\"value\":null,\"utcTimestamp\":null}}"),
                        LocalDateTime.now())
        ));

        Optional<Value<String>> pick = picker.pick(TestPredefinedField.GUMMY, item);

        assertThat(pick).isEmpty();
    }


    /**
     * Проверяем, что фильтруется значение по источникам.
     */
    @Test
    public void pickAmongFilteredSources() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of(
                DATACAMP_MSKU, index(TestPredefinedField.DUMMY, "2"),
                DATACAMP_SSKU, index(TestPredefinedField.DUMMY, "3"),
                CONTENT, index(TestPredefinedField.DUMMY, "4")
        ));

        executeExistingValuePickingScenario(item, TestPredefinedField.DUMMY, "4");
    }

    private <T> void executeExistingValuePickingScenario(Item item, Field<T> field, T expectedValue) {
        Optional<Value<T>> pick = picker.pick(field, item);

        assertSoftly(assertions -> {
            assertions.assertThat(pick).isPresent();
            pick.ifPresent(value -> {
                assertions.assertThat(value.getValue()).isEqualTo(expectedValue);
            });
        });
    }

    private <T> ReferenceIndexWithUpdatedDate index(Field<T> field, T fieldValue) {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(field, fieldValue, ZonedDateTime.now());

        return new ReferenceIndexWithUpdatedDate(index, LocalDateTime.now());
    }
}
