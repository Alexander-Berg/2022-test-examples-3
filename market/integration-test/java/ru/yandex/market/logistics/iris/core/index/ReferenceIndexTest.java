package ru.yandex.market.logistics.iris.core.index;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import ru.yandex.market.logistics.iris.core.exception.index.field.IllegalFieldException;
import ru.yandex.market.logistics.iris.core.exception.index.field.IllegalFieldValueException;
import ru.yandex.market.logistics.iris.core.index.change.ChangeType;
import ru.yandex.market.logistics.iris.core.index.change.ReferenceIndexChange;
import ru.yandex.market.logistics.iris.core.index.dummy.TestPredefinedField;
import ru.yandex.market.logistics.iris.core.index.dummy.TestReferenceIndexChange;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.core.index.strategy.ReferenceIndexCheckUpdatedFieldBasedOnTimestampStrategy;
import ru.yandex.market.logistics.iris.core.index.strategy.ReferenceIndexCheckUpdatedFieldBasedOnTrustworthyVersionStrategy;
import ru.yandex.market.logistics.iris.core.index.strategy.ReferenceIndexCheckUpdatedFieldStrategy;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.logistics.iris.core.index.JsonMatchers.matchingJson;
import static ru.yandex.market.logistics.iris.core.index.JsonMatchers.matchingJsonWithoutOrder;

@RunWith(BlockJUnit4ClassRunner.class)
public class ReferenceIndexTest {


    private static final String DUMMY = "{\"dummy\":{\"value\":\"dummy_value\",\"utcTimestamp\":\"1970-01-01T00:00:00\"}}";
    private static final String YUMMY = "{\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}}";
    private static final String DUMMY_YUMMY = "{" +
        "\"dummy\":{\"value\":\"dummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}," +
        "\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
        "}";

    private static final String DUMMY_NULL = "{\"dummy\":{\"value\":null,\"utcTimestamp\":\'2016-01-23T12:34:56\'}}";

    private static final String DUMMY_VALUE = "dummy_value";
    private static final ZonedDateTime UPDATED_TIMESTAMP = ZonedDateTime.of(LocalDateTime.of(2016, 1, 23, 12, 34, 56), UTC);

    private static final ReferenceIndexCheckUpdatedFieldStrategy
            CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TRUSTWORTHY_VERSION =
            new ReferenceIndexCheckUpdatedFieldBasedOnTrustworthyVersionStrategy();
    private static final ReferenceIndexCheckUpdatedFieldStrategy
            CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP =
            new ReferenceIndexCheckUpdatedFieldBasedOnTimestampStrategy();

    private ChangeTrackingReferenceIndexer referenceIndexer = ReferenceIndexerTestFactory.getIndexer();


    private ChangeTrackingReferenceIndex emptyIndex;

    @Before
    public void setUp() {
        emptyIndex = referenceIndexer.createEmptyIndex();
    }

    /**
     * Пробуем очистить значение у поля YUMMY.
     * Ранее значение у этого поля отсутствовало.
     * <p>
     * Ожидаемое поведение:
     * Значение поля DUMMY осталочь неизменным.
     */
    @Test
    public void removeMissingValue() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY);

        String previousValue = index.remove(TestPredefinedField.YUMMY);

        String json = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isNull();
            assertions.assertThat(json).is(matchingJson(DUMMY));
        });
    }

    /**
     * Пробуем удалить существующее значение YUMMY.
     * <p>
     * Ожидаемое поведение:
     * Из индекса будет удалено указанное значение.
     * Значение поля DUMMY останется прежним.
     */
    @Test
    public void removeExistingValue() {
        String dummyYummyInitial = "{" +
            "\"dummy\":{\"value\":\"dummy_value\", \"utcTimestamp\":\"2016-01-23T12:34:56\"}," +
            "\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"2016-01-23T12:34:56\"}" +
            "}";

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(dummyYummyInitial);

        String previousValue = index.remove(TestPredefinedField.YUMMY);

        String json = index.toJson(referenceIndexer);

        String expectedJson = "{\"dummy\":{\"value\":\"dummy_value\", \"utcTimestamp\":\'2016-01-23T12:34:56\'}}";

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isEqualTo("yummy_value");
            assertions.assertThat(json).is(matchingJsonWithoutOrder(expectedJson));
        });
    }


    /**
     * Пробуем удалить null'овое значение DUMMY.
     * <p>
     * Ожидаемое поведение:
     * Из индекса будет удалено указанное значение.
     * Значение поля YUMMY останется прежним.
     */
    @Test
    public void removeNullValue() {
        String initialDummyYummy = "{" +
            "\"dummy\":{\"value\":null,\"utcTimestamp\":\"1970-01-01T00:00:00\"}," +
            "\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
            "}";

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialDummyYummy);

        String previousValue = index.remove(TestPredefinedField.DUMMY);

        String json = index.toJson(referenceIndexer);

        String expectedJson = "{\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}}";

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isNull();
            assertions.assertThat(json).is(matchingJsonWithoutOrder(expectedJson));
        });
    }

    /**
     * Пробуем установить значение поля DUMMY (ранее значения не было).
     * <p>
     * Ожидаемое поведение:
     * Индекс должен дополниться новым значением,
     * Ранее заполненные значения должны остаться прежними.
     */
    @Test
    public void setValueToEmptyField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(YUMMY);

        String previousValue = index.set(TestPredefinedField.DUMMY, DUMMY_VALUE, ZonedDateTime.of(LocalDateTime.of(2016, 1, 23, 12, 34, 56), UTC));

        String json = index.toJson(referenceIndexer);

        String expected = "{" +
            "\"dummy\":{\"value\":\"dummy_value\", \"utcTimestamp\":\'2016-01-23T12:34:56\'}," +
            "\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
            "}";

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isNull();
            assertions.assertThat(json).is(matchingJson(expected));
        });
    }

    /**
     * Пробуем установить новое значение поля DUMMY взамен существующего.
     * <p>
     * Ожидаемое поведение:
     * Индекс должен заменить старое значение на новое.
     * Прочие поля должны остаться неизменными.
     */
    @Test
    public void setValueToFilledField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);

        String previousValue = index.set(TestPredefinedField.DUMMY, "another_value", UPDATED_TIMESTAMP);

        String json = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isEqualTo(DUMMY_VALUE);
            assertions.assertThat(json).is(
                matchingJson("{" +
                    "\"dummy\":{\"value\":\"another_value\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}," +
                    "\"yummy\":{\"value\":\"yummy_value\",\"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
                    "}"));
        });
    }


    /**
     * Пробуем установить новое значение поля DUMMY (=null) взамен существующего.
     * <p>
     * Ожидаемое поведение:
     * Индекс должен заменить старое значение на новое (=null).
     * Прочие поля должны остаться неизменными.
     */
    @Test
    public void setNullValueToField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);

        String previousValue = index.set(TestPredefinedField.DUMMY, null, ZonedDateTime.of(LocalDateTime.of(2016, 1, 23, 12, 34, 56), UTC));

        String json = index.toJson(referenceIndexer);

        String expectedDummyYummy = "{" +
            "\"dummy\":{\"value\":null, \"utcTimestamp\":\'2016-01-23T12:34:56\'}," +
            "\"yummy\":{\"value\":\"yummy_value\", \"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
            "}";

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isEqualTo("dummy_value");
            assertions.assertThat(json).is(matchingJson(expectedDummyYummy));
        });
    }


    /**
     * Проверяем, что при попытке получить значение из отстутствующего поля -
     * в ответ будет возвращен null, а индекс останется прежним.
     */
    @Test
    public void getMissingFieldValue() {
        String initialJson = YUMMY;
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialJson);

        String value = index.get(TestPredefinedField.DUMMY);

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(value).isNull();
            assertions.assertThat(actualJson).is(matchingJson(initialJson));
        });
    }

    /**
     * Проверяем, что при попытке получить значене из поля, в котором записано значение null -
     * в ответ будет возвращен null, а индекс останется неизменным.
     */
    @Test
    public void getNullFieldValue() {
        String initialDummyYummy = "{" +
            "\"dummy\":{\"value\":null,\"utcTimestamp\":\"1970-01-01T00:00:00\"}," +
            "\"yummy\":{\"value\":\"yummy_value\",\"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
            "}";

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialDummyYummy);

        String value = index.get(TestPredefinedField.DUMMY);

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(value).isNull();
            assertions.assertThat(actualJson).is(matchingJson(initialDummyYummy));
        });
    }

    /**
     * Проверяем, что при попытке получить значение из поля, в котором записано значение -
     * мы успешно получим его в ответ, а индекс останется неизменным.
     */
    @Test
    public void getExistingFieldValue() {
        String initialJson = DUMMY_YUMMY;
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialJson);

        String value = index.get(TestPredefinedField.DUMMY);

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(value).isEqualTo(DUMMY_VALUE);
            assertions.assertThat(actualJson).is(matchingJson(initialJson));
        });
    }


    /**
     * Проверяем, что при попытке проверить наличие у поля, которого не существует
     * - в ответ будет возвращено значение false, а сам индекс останется неизменным.
     */
    @Test
    public void containsOnMissingField() {
        String initialJson = YUMMY;
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialJson);

        boolean contains = index.contains(TestPredefinedField.DUMMY);

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(contains).isFalse();
            assertions.assertThat(actualJson).is(matchingJson(initialJson));
        });
    }

    /**
     * Проверяем, что при попытке проверить наличие у поля, чье значение null -
     * в ответ будет возвращено значение true, а сам индекс останется неизменным.
     */
    @Test
    public void containsOnNullField() {
        String initialDummyYummy = "{" +
            "\"dummy\":{\"value\":null,\"utcTimestamp\":\"1970-01-01T00:00:00\"}," +
            "\"yummy\":{\"value\":\"yummy_value\",\"utcTimestamp\":\"1970-01-01T00:00:00\"}" +
            "}";

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialDummyYummy);

        boolean contains = index.contains(TestPredefinedField.DUMMY);

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(contains).isTrue();
            assertions.assertThat(actualJson).is(matchingJson(initialDummyYummy));
        });
    }

    /**
     * Проверяем, что при попытке проверить наличие у поля, которое имеет не null'овое значение -
     * в ответ будет возвращено значение true, а сам индекс останется неизменным.
     */
    @Test
    public void containsOnExistingField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);

        boolean contains = index.contains(TestPredefinedField.DUMMY);

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly(assertions -> {
            assertions.assertThat(contains).isTrue();
            assertions.assertThat(actualJson).is(matchingJson(DUMMY_YUMMY));
        });
    }

    /**
     * Проверяем, что у пустого индекса,
     * над которым пока не производили действий коллекция изменений пустая.
     */
    @Test
    public void noChangesForNewCreatedIndex() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertThat(changes).isEmpty();
    }

    /**
     * Проверяем, что у индекса, созданного на основе не пустого JSON
     * над которым пока не производили новых действий коллекция изменения пустая
     */
    @Test
    public void noChangesForExistingCreatedIndex() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);
        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertThat(changes).isEmpty();
    }

    /**
     * Проверяем, что при изменении 1го поля мы увидим в истории это изменение.
     */
    @Test
    public void setSingleValue() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        String previousValue = index.set(TestPredefinedField.DUMMY, "value", UPDATED_TIMESTAMP);

        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertSoftly(assertions -> {
            assertions.assertThat(changes).hasSize(1);
            assertions.assertThat(previousValue).isNull();

            ReferenceIndexChange<?> actual = changes.iterator().next();

            assertChange(assertions, actual, new TestReferenceIndexChange<>(
                TestPredefinedField.DUMMY,
                FieldValue.of("value", TestPredefinedField.DUMMY, UPDATED_TIMESTAMP),
                ChangeType.SET
            ));
        });
    }

    /**
     * Проверяем, что при изменении нескольких полей оба изменения
     * сохранились корректно и в правильном порядке.
     */
    @Test
    public void setMultipleValues() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        String previousDummyValue = index.set(TestPredefinedField.DUMMY, "val1", UPDATED_TIMESTAMP);
        String previousYummyValue = index.set(TestPredefinedField.YUMMY, "val2", UPDATED_TIMESTAMP);

        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertSoftly(assertions -> {
            assertions.assertThat(changes).hasSize(2);

            assertions.assertThat(previousDummyValue).isNull();
            assertions.assertThat(previousYummyValue).isNull();

            ReferenceIndexChange<?> actualFirstChange = changes.get(0);
            ReferenceIndexChange<?> actualSecondChange = changes.get(1);

            assertChange(assertions, actualFirstChange, new TestReferenceIndexChange<>(
                TestPredefinedField.DUMMY,
                FieldValue.of("val1", TestPredefinedField.DUMMY, UPDATED_TIMESTAMP),
                ChangeType.SET
            ));

            assertChange(assertions, actualSecondChange, new TestReferenceIndexChange<>(
                TestPredefinedField.YUMMY,
                FieldValue.of("val2", TestPredefinedField.YUMMY, UPDATED_TIMESTAMP),
                ChangeType.SET
            ));
        });
    }


    /**
     * Проверяем, что если значение поля после установки осталось прежним - мы не записали информацию об изменении.
     */
    @Test
    public void setTheSameValueForField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);
        String previousValue = index.set(TestPredefinedField.DUMMY, DUMMY_VALUE, ZonedDateTime.now());

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isEqualTo(DUMMY_VALUE);
            assertions.assertThat(index.getChanges()).isEmpty();
        });
    }

    /**
     * Проверяем, что если значение не было удалено, то изменение не будет сохранено.
     */
    @Test
    public void noValueRemoved() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        String previousValue = index.remove(TestPredefinedField.DUMMY);

        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isNull();
            assertions.assertThat(changes).isEmpty();
        });
    }

    /**
     * Проверяем, что при удалении существующего значения поля,
     * <p>
     * в изменения будет записана информация об удалении.
     */
    @Test
    public void actualValueRemoved() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);
        String previousValue = index.remove(TestPredefinedField.DUMMY);

        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertSoftly(assertions -> {
            assertions.assertThat(previousValue).isEqualTo(DUMMY_VALUE);
            assertions.assertThat(changes).hasSize(1);
            ReferenceIndexChange<?> actualChange = changes.iterator().next();

            assertChange(assertions, actualChange, new TestReferenceIndexChange<>(
                TestPredefinedField.DUMMY,
                null,
                ChangeType.REMOVE
            ));
        });
    }

    /**
     * Проверяем, что при удалении существующего значения поля, равного null,
     * в изменения будет записана информация об удалении этого поля.
     */
    @Test
    public void nullValueRemoved() {
        String dummyNullSet = "{" +
            "\"dummy\":{\"value\":null,\"utcTimestamp\":null}," +
            "\"yummy\":{\"value\":\"yummy_value\",\"utcTimestamp\":null}" +
            "}";

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(dummyNullSet);

        String previousValue = index.remove(TestPredefinedField.DUMMY);
        List<ReferenceIndexChange<?>> changes = index.getChanges();

        assertSoftly(assertions -> {
            assertions.assertThat(changes).hasSize(1);
            assertions.assertThat(previousValue).isNull();

            ReferenceIndexChange<?> actualChange = changes.get(0);

            assertChange(assertions, actualChange, new TestReferenceIndexChange<>(
                TestPredefinedField.DUMMY,
                null,
                ChangeType.REMOVE
            ));
        });
    }


    /**
     * Проверяем, что если в ранее пустое поле устанавливаем значение null -
     * информация об изменении будет записана и в индекс и в лог изменений.
     */
    @Test
    public void nullValueSetToEmptyField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        String previousValue = index.set(TestPredefinedField.DUMMY, null, ZonedDateTime.of(LocalDateTime.of(2016, 1, 23, 12, 34, 56), UTC));
        List<ReferenceIndexChange<?>> changes = index.getChanges();

        String actualJson = index.toJson(referenceIndexer);

        assertSoftly((assertions) -> {
            assertions.assertThat(previousValue).isNull();
            assertions.assertThat(changes).hasSize(1);

            assertions.assertThat(actualJson).is(matchingJsonWithoutOrder(DUMMY_NULL));
            ReferenceIndexChange<?> change = changes.get(0);
            assertChange(assertions, change, new TestReferenceIndexChange<>(
                TestPredefinedField.DUMMY,
                FieldValue.of(null, TestPredefinedField.DUMMY, UPDATED_TIMESTAMP),
                ChangeType.SET
            ));
        });
    }


    /**
     * Проверяем, что при вызове проверки наличия определенной связки поле + (не null значение) у индекса,
     * у которого это поле не заполнено совсем - будет возвращено значение false;
     */
    @Test
    public void containsExactlyWithNonNullOnEmptyField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();

        boolean containsExactly = index.containsExactly(TestPredefinedField.DUMMY, DUMMY_VALUE);

        assertThat(containsExactly).isFalse();
    }

    /**
     * Проверяем, что при вызове проверки наличия определенной связки поле + (null значение) у индекса,
     * у которого это поле не заполнено совсем - будет возвращено значение false;
     */
    @Test
    public void containsExactlyWithNullOnEmptyField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();

        boolean containsExactly = index.containsExactly(TestPredefinedField.DUMMY, null);

        assertThat(containsExactly).isFalse();
    }

    /**
     * Проверяем, что при вызове проверки наличия определенной связки поле + (не null значение) у индекса,
     * у которого это поле заполнено другим значением - будет возвращено значение false;
     */
    @Test
    public void containsExactlyWithNonNullOnFieldWithDifferentValue() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);

        boolean containsExactly = index.containsExactly(TestPredefinedField.DUMMY, "abc");

        assertThat(containsExactly).isFalse();
    }

    /**
     * Проверяем, что при вызове проверки наличия определенной связки поле + (null значение) у индекса,
     * у которого это поле так же заполнено null'овым значением - будет возвращено значение true;
     */
    @Test
    public void containsExactlyWithNullFieldWithNullValue() {
        String initialDummyYummy = "{" +
            "\"dummy\":{\"value\":null,\"utcTimestamp\":null}," +
            "\"yummy\":{\"value\":\"yummy_value\",\"utcTimestamp\":null}" +
            "}";

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialDummyYummy);

        boolean containsExactly = index.containsExactly(TestPredefinedField.DUMMY, null);

        assertThat(containsExactly).isTrue();
    }

    /**
     * Проверяем, что при вызове проверки наличия определенной связки поле + (не null значение) у индекса,
     * у которого это поле так же заполнено аналогичным значением - будет возвращено значение true;
     */
    @Test
    public void containsExactlyTheSameValue() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY);

        boolean containsExactly = index.containsExactly(TestPredefinedField.DUMMY, DUMMY_VALUE);

        assertThat(containsExactly).isTrue();
    }

    /**
     * Пробуем установить non null значение в поле,
     * которое имеет ограничение на только non null значения.
     * Ожидаем, что поле будет успешно проинициализированно переданным значением.
     */
    @Test
    public void setNonNullToNonNullableField() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        ZonedDateTime updatedDateTIme = ZonedDateTime.of(LocalDateTime.of(2016, 1, 23, 12, 34, 56), ZoneOffset.UTC);

        index.set(TestPredefinedField.GUMMY, "x", updatedDateTIme);

        String json = index.toJson(referenceIndexer);

        assertThat(json).is(matchingJson("{\"gummy\":{\"value\":\"x\", \"utcTimestamp\":\"2016-01-23T12:34:56\"}}"));
    }

    /**
     * Пробуем установить значение null в поле,
     * которое имеет ограничение на только non null значения.
     * Ожидаем получить IllegalFieldValueException.
     */
    @Test(expected = IllegalFieldValueException.class)
    public void setNullToNonNullableField() {
        this.emptyIndex.set(TestPredefinedField.GUMMY, null, ZonedDateTime.now());
    }

    /**
     * Пробуем вызвать метод set, указав в качестве поля null
     * Ожидаем получить IllegalFieldException.
     */
    @Test(expected = IllegalFieldException.class)
    public void callSetMethodWithNullField() {
        this.emptyIndex.set(null, null, ZonedDateTime.now());
    }

    /**
     * Пробуем вызвать метод remove, указав в качестве поля null
     * Ожидаем получить IllegalFieldException.
     */
    @Test(expected = IllegalFieldException.class)
    public void callRemoveWithNullField() {
        this.emptyIndex.remove(null);
    }

    /**
     * Пробуем вызвать метод contains, указав в качестве поля null
     * Ожидаем получить IllegalFieldException.
     */
    @Test(expected = IllegalFieldException.class)
    public void callContainsWithNullField() {
        this.emptyIndex.contains(null);
    }

    /**
     * Пробуем вызвать метод containsExactly, указав в качестве поля null
     * Ожидаем получить IllegalFieldException.
     */
    @Test(expected = IllegalFieldException.class)
    public void callContainsExactlyWithNullField() {
        this.emptyIndex.containsExactly(null, null);
    }

    /**
     * Пробуем вызвать метод get, указав в качестве поля null
     * Ожидаем получить IllegalFieldException.
     */
    @Test(expected = IllegalFieldException.class)
    public void callGetWithNullField() {
        this.emptyIndex.get(null);
    }

    /**
     * Проверяем, что метод получения заполненных полей вернет пустую коллекцию для пустого индекса.
     */
    @Test
    public void getFieldsFromEmptyIndex() {
        Collection<Field<?>> fields = this.emptyIndex.getFields();

        assertThat(fields).isEmpty();
    }

    /**
     * Проверяем, что метод получения заполненных полей вернет именно те поля, что присутствует в индексе.
     */
    @Test
    public void getFieldsFromFilledIndex() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);

        Collection<Field<?>> fields = index.getFields();

        assertThat(fields).containsExactlyInAnyOrder(TestPredefinedField.DUMMY, TestPredefinedField.YUMMY);
    }

    /**
     * Проверяем, что методы set/get корректно работают в связке с getFields.
     */
    @Test
    public void getFieldsFromFilledIndexWithOneOfTheFieldsRemoved() {
        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(DUMMY_YUMMY);
        index.remove(TestPredefinedField.DUMMY);
        index.set(TestPredefinedField.GUMMY, "asd", UPDATED_TIMESTAMP);

        Collection<Field<?>> fields = index.getFields();

        assertThat(fields).containsExactlyInAnyOrder(TestPredefinedField.YUMMY, TestPredefinedField.GUMMY);
    }

    /**
     * Проверяем, что после слияния одного пустого индекса в другой оба останутся пустыми.
     */
    @Test
    public void mergeEmptyIntoEmpty() {
        ChangeTrackingReferenceIndex first = referenceIndexer.createEmptyIndex();
        ChangeTrackingReferenceIndex second = referenceIndexer.createEmptyIndex();

        second.merge(first, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        assertThat(first.toJson(referenceIndexer)).is(matchingJsonWithoutOrder("{}"));
        assertThat(second.toJson(referenceIndexer)).is(matchingJsonWithoutOrder("{}"));
    }

    /**
     * Проверяем, что после слияния заполненного индекса в пустой все поля и значения будут успешно перенесены.
     */
    @Test
    public void mergeFilledIntoEmptyIndex() {
        String index = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

        ChangeTrackingReferenceIndex from = referenceIndexer.fromJson(index);
        ChangeTrackingReferenceIndex to = referenceIndexer.createEmptyIndex();

        to.merge(from, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        assertThat(to.toJson(referenceIndexer)).is(matchingJsonWithoutOrder(index));
    }

    /**
     * Проверяем, что после слияния индекса, в котором заполнено одно поле и индекса, у которого заполнено другое поле
     * в итоге получится индекс с обоими полями.
     */
    @Test
    public void mergeFilledWithOneFieldIntoFilledWithAnotherField() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";
        String to = "{\"gummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-23T13:34:56\"}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(
            "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}," +
                "\"gummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-23T13:34:56\"}}"
        ));
    }

    /**
     * Проверяем, что после слияния двух индексов, у которых имеется разное значение одного и того же поля,
     * но имеющее идентичный utcTimestamp - значение будет заменено.
     */
    @Test
    public void mergeIndexesWithTheSameFieldWithTheSameTimestamp() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";
        String to = "{\"dummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(from));
    }

    /**
     * Проверяем, что после слияния двух индексов, у которых имеется одинаковое значение одного и того же поля,
     * но имеющее разный utcTimestamp - значение не будет заменено и utcTimestamp.
     */
    @Test
    public void mergeIndexesWithTheSameFieldWithDifferentTimestamp() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:55:56\"}}";
        String to = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(to));
    }

    /**
     * Проверяем, что после слияния двух индексов, у которых имеется разное значение одного и того же поля,
     * однако у индекса, в которого мы осуществляем слияние более позднее значение utcTimestamp.
     * В результате внешнее значение должно быть проигнорировано.
     */
    @Test
    public void mergeIndexesWithTheSameFieldWithOlderTimestamp() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";
        String to = "{\"dummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-23T11:34:56\"}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(from));
    }

    /**
     * Проверяем, что после слияния двух индексов, у которых имеется разное значение одного и того же поля,
     * однако у индекса, в которого мы осуществляем слияние более раннее значение utcTimestamp.
     * В результате внешнее значение должно быть установлено у индекса.
     */
    @Test
    public void mergeIndexesWithTheSameFieldWithNewerTimestamp() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T11:34:56\"}}";
        String to = "{\"dummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TIMESTAMP);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(to));
    }

    /**
     * Проверяем, что при слиянии двух индексов, у нового содержится обновленное поле trustworthy_version,
     * все поля будут обновлены (таймстемпы не учитываются).
     */
    @Test
    public void mergeIndexesWithNewTrustworthyVersion() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T11:34:56\"}, " +
                "\"trustworthy_version\": {\"value\":5,\"utcTimestamp\":\"2016-01-23T11:34:56\"}}";
        String to = "{\"dummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-21T12:34:56\"}," +
                "\"trustworthy_version\": {\"value\":6,\"utcTimestamp\":\"2016-01-25T11:34:56\"}}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TRUSTWORTHY_VERSION);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(to));
    }

    /**
     * Проверяем, что при слиянии двух индексов, у нового содержится устаревшее поле trustworthy_version,
     * все поля будут обновлены (таймстемпы не учитываются).
     */
    @Test
    public void mergeIndexesWithOldTrustworthyVersion() {
        String from = "{\"dummy\":{\"value\":\"abc\",\"utcTimestamp\":\"2016-01-23T11:34:56\"}, " +
                "\"trustworthy_version\": {\"value\":7,\"utcTimestamp\":\"2016-01-23T11:34:56\"}}";
        String to = "{\"dummy\":{\"value\":\"bcd\",\"utcTimestamp\":\"2016-01-21T12:34:56\"}," +
                "\"trustworthy_version\": {\"value\":6,\"utcTimestamp\":\"2016-01-25T11:34:56\"}}}";

        ChangeTrackingReferenceIndex fromIndex = referenceIndexer.fromJson(from);
        ChangeTrackingReferenceIndex toIndex = referenceIndexer.fromJson(to);

        toIndex.merge(fromIndex, CHECK_UPDATED_FIELD_STRATEGY_BASED_ON_TRUSTWORTHY_VERSION);

        String actualJson = toIndex.toJson(referenceIndexer);

        assertThat(actualJson).is(matchingJsonWithoutOrder(from));
    }


    /**
     * Проверяем, что для даты поля с 1970.01.01 в UTC будут возвращены значения
     * utcTimestamp (так же равный 1970.01.01)
     * utcTimestampMillis (равный 0)
     * а для поля с датой 1970.01.07 в UTC будут возвращены значения
     * utcTimestamp (так же равный 1970.01.01)
     * utcTimestampMillis (равный 604800000 = неделя)
     */
    @Test
    public void getUtcTimestampMillis() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();

        ZonedDateTime valueDateTime = ZonedDateTime.of(LocalDate.of(1970, 1, 1).atStartOfDay(), UTC);

        index.set(TestPredefinedField.DUMMY, "value", valueDateTime);
        index.set(TestPredefinedField.GUMMY, "value", valueDateTime.plusWeeks(1));


        SoftAssertions.assertSoftly(assertions -> {
            ZonedDateTime expectedDateTime = LocalDate.of(1970, 1, 1).atStartOfDay(UTC);

            Optional<FieldValue<String>> dummyFieldValue = index.getFieldValue(TestPredefinedField.DUMMY);
            assertions.assertThat(dummyFieldValue).isPresent();

            Optional<ZonedDateTime> dummyUtcTimestamp = dummyFieldValue.map(FieldValue::getUtcTimestamp);
            Optional<Long> dummyUtcTimestampMillis = dummyFieldValue.map(FieldValue::getUtcTimestampMillis);

            assertions.assertThat(dummyUtcTimestamp).hasValue(expectedDateTime);
            assertions.assertThat(dummyUtcTimestampMillis).hasValue(0L);

            Optional<FieldValue<String>> gummyFieldValue = index.getFieldValue(TestPredefinedField.GUMMY);
            assertions.assertThat(gummyFieldValue).isPresent();

            Optional<ZonedDateTime> gummyUtcTimestamp = gummyFieldValue.map(FieldValue::getUtcTimestamp);;
            Optional<Long> gummyUtcTimestampMillis = gummyFieldValue.map(FieldValue::getUtcTimestampMillis);

            assertions.assertThat(gummyUtcTimestamp).hasValue(expectedDateTime.plusWeeks(1));
            assertions.assertThat(gummyUtcTimestampMillis).hasValue(604800000L);
        });
    }

    private void assertChange(SoftAssertions assertions,
                              ReferenceIndexChange<?> actual,
                              ReferenceIndexChange<?> expected) {
        assertions.assertThat(actual.getValue()).isEqualTo(expected.getValue());
        assertions.assertThat(actual.getField()).isEqualTo(expected.getField());
        assertions.assertThat(actual.getChangeType()).isEqualTo(expected.getChangeType());
    }
}
