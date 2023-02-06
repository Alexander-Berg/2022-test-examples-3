package ru.yandex.direct.useractionlog.writer.generator;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.binlogclickhouse.schema.FieldValue;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.useractionlog.dict.DictResponsesAccessor;
import ru.yandex.direct.useractionlog.model.RowModelPair;
import ru.yandex.direct.useractionlog.model.TestRowModel;

@ParametersAreNonnullByDefault
public class DeduplicationFieldsStrategyTest {
    private static final DictResponsesAccessor EMPTY_ACCESSOR =
            new DictResponsesAccessor(Collections.emptyMap());

    private final DeduplicationFieldsStrategy.Builder handlerBuilder = DeduplicationFieldsStrategy.builder()
            .alwaysWriteFieldsInBefore("important")
            .withFieldGroup("group1first", "group1second", "group1third")
            .withFieldGroup(Arrays.asList(
                    Pair.of("group2firstBit1", value -> Integer.parseInt(value) & 1),
                    Pair.of("group2firstBit2", value -> Integer.parseInt(value) & 1)))
            .withFieldGroup("group3distinct1", "group34shared", "group3distinct2")
            .withFieldGroup("group4distinct1", "group34shared", "group4distinct2");

    /**
     * Простой случай - дедупликация при обновлении, без каких-либо групп и сложных функций получения групп.
     */
    @Test
    public void handleUpdate() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("same1", "same"),
                        Pair.of("diff1", "one"),
                        Pair.of("same2", ""),
                        Pair.of("diff2", ""),
                        Pair.of("same3", null),
                        Pair.of("diff3", null)))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("same1", "same"),
                        Pair.of("diff1", "two"),
                        Pair.of("same2", ""),
                        Pair.of("diff2", null),
                        Pair.of("same3", null),
                        Pair.of("diff3", "")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("diff1", "one"),
                        Pair.of("diff2", ""),
                        Pair.of("diff3", null)))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("diff1", "two"),
                        Pair.of("diff2", null),
                        Pair.of("diff3", "")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Из пустого изменения должно получиться пустое изменение.
     */
    @Test
    public void emptySourceReturnsEmpty() throws Exception {
        RowModelPair source = new RowModelPair<>(new TestRowModel(), new TestRowModel());
        RowModelPair expected = new RowModelPair<>(new TestRowModel(), new TestRowModel());

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Если все поля не изменились, то должно получиться пустое изменение.
     */
    @Test
    public void allDuplicatesReturnsEmpty() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("baz", null)))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("baz", null)))));
        RowModelPair expected = new RowModelPair<>(new TestRowModel(), new TestRowModel());
        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Если все поля изменились, то возвращается как есть.
     * Простой случай, без каких-либо групп и сложных функций получения групп.
     */
    @Test
    public void allUniqueReturnsSame() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", null),
                        Pair.of("baz", "")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "yyy"),
                        Pair.of("bar", ""),
                        Pair.of("baz", null)))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(new LinkedHashMap<>(source.before.getMap())),
                new TestRowModel(new LinkedHashMap<>(source.after.getMap())));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Если поле добавилось или удалилось, это считается как изменение, поле не вырезается.
     */
    @Test
    public void differentFieldNames() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("only1", "only1value"),
                        Pair.of("foo", "foo"),
                        Pair.of("bar", "bar1")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "foo"),
                        Pair.of("bar", "bar2"),
                        Pair.of("only2", "only2value")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("only1", "only1value"),
                        Pair.of("bar", "bar1")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("bar", "bar2"),
                        Pair.of("only2", "only2value")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Для некоторых полей можно сделать исключение - они не будут удаляться из before даже если не изменились.
     * А вот из after всё равно будут удаляться, если не изменились.
     */
    @Test
    public void keepImportant() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("important", "same")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("important", "same")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Collections.singletonList(Pair.of("important", "same")))),
                new TestRowModel());

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Можно сделать так, чтобы ни одно поле никогда не удалялось из before, независимо от того,
     * изменилось оно или нет.
     */
    @Test
    public void keepsEverythingInBefore() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("important", "same")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("important", "same")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("foo", "xxx"),
                        Pair.of("bar", ""),
                        Pair.of("important", "same")))),
                new TestRowModel());

        handlerBuilder.keepsEverythingInBefore().build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Простая группа, без особых функций извлечения значений. Если хотя бы одно значение из группы полей изменилось,
     * то в before остаётся вся группа. Из after поля вырезаются как обычно.
     */
    @Test
    public void plainGroupKeep() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group1first", "sameFirst"),
                        Pair.of("foo", "foo"),
                        Pair.of("group1second", "sameSecond"),
                        Pair.of("group1third", "oldThird")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group1third", "newThird"),
                        Pair.of("group1second", "sameSecond"),
                        Pair.of("group1first", "sameFirst"),
                        Pair.of("foo", "foo")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group1first", "sameFirst"),
                        Pair.of("group1second", "sameSecond"),
                        Pair.of("group1third", "oldThird")))),
                new TestRowModel(fromPairs(Collections.singletonList(
                        Pair.of("group1third", "newThird")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Простая группа, без особых функций извлечения значений. Если ни одно значение из группы полей не изменилось,
     * то поля вырезаются из before и из after как обычно.
     */
    @Test
    public void plainGroupRemove() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group1first", "sameFirst"),
                        Pair.of("foo", "foo"),
                        Pair.of("group1second", "sameSecond"),
                        Pair.of("group1third", "sameThird")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group1third", "sameThird"),
                        Pair.of("group1second", "sameSecond"),
                        Pair.of("group1first", "sameFirst"),
                        Pair.of("foo", "foo")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(),
                new TestRowModel());

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Две простые группы, и одно и то же поле входит сразу в две группы.
     * Если изменяется общее поле, то в before будут записаны обе группы.
     * В after будет записано только изменившееся поле.
     */
    @Test
    public void sharedFieldInPlainGroups() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group34shared", "old-group34shared"),
                        Pair.of("group3distinct1", "old-group3distinct1"),
                        Pair.of("group3distinct2", "old-group3distinct2"),
                        Pair.of("group4distinct1", "old-group4distinct1"),
                        Pair.of("group4distinct2", "old-group4distinct2")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group34shared", "new-group34shared"),  // изменение
                        Pair.of("group3distinct1", "old-group3distinct1"),
                        Pair.of("group3distinct2", "old-group3distinct2"),
                        Pair.of("group4distinct1", "old-group4distinct1"),
                        Pair.of("group4distinct2", "old-group4distinct2")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group34shared", "old-group34shared"),
                        Pair.of("group3distinct1", "old-group3distinct1"),
                        Pair.of("group3distinct2", "old-group3distinct2"),
                        Pair.of("group4distinct1", "old-group4distinct1"),
                        Pair.of("group4distinct2", "old-group4distinct2")))),
                new TestRowModel(fromPairs(Collections.singletonList(
                        Pair.of("group34shared", "new-group34shared")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Две простые группы, и одно и то же поле входит сразу в две группы.
     * Изменяется другое поле, которое есть только в одной группе.
     * В before должна быть оставлена только одна группа, в которой было изменение.
     * В after должно остаться только то поле, которое изменилось.
     */
    @Test
    public void inGroupWithSharedFieldChangesDistinct() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group34shared", "old-group34shared"),
                        Pair.of("group3distinct1", "old-group3distinct1"),
                        Pair.of("group3distinct2", "old-group3distinct2"),
                        Pair.of("group4distinct1", "old-group4distinct1"),
                        Pair.of("group4distinct2", "old-group4distinct2")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group34shared", "old-group34shared"),
                        Pair.of("group3distinct1", "new-group3distinct1"),  // изменение
                        Pair.of("group3distinct2", "old-group3distinct2"),
                        Pair.of("group4distinct1", "old-group4distinct1"),
                        Pair.of("group4distinct2", "old-group4distinct2")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group34shared", "old-group34shared"),
                        Pair.of("group3distinct1", "old-group3distinct1"),
                        Pair.of("group3distinct2", "old-group3distinct2")))),
                new TestRowModel(fromPairs(Collections.singletonList(
                        Pair.of("group3distinct1", "new-group3distinct1")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Сложная группа, с указанной фунцией извлечения данных.
     * В этом тесте группа содержит два поля. У каждого поля одинаковая функция для сравнения - возвращает true, если
     * значение - нечётное число.
     * <p>
     * Если хотя бы одно из полей в группе меняет чётность, то в before остаётся вся группа, а в after как обычно -
     * вырезаются поля, у которых изменились сырые необработанные значения.
     */
    @Test
    public void complexGroupKeep() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 чётный
                        // group2firstBit2 нечётный
                        Pair.of("group2firstBit1", "4"),
                        Pair.of("foo", "foo"),
                        Pair.of("group2firstBit2", "3")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBif1 нечётный (изменилось)
                        // group2firstBif2 нечётный (осталось как есть)
                        Pair.of("group2firstBit2", "55"),
                        Pair.of("group2firstBit1", "77"),
                        Pair.of("foo", "foo")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group2firstBit1", "4"),
                        Pair.of("group2firstBit2", "3")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group2firstBit2", "55"),
                        Pair.of("group2firstBit1", "77")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Сложная группа, с указанной фунцией извлечения данных.
     * В этом тесте группа содержит два поля. У каждого поля одинаковая функция для сравнения - возвращает true, если
     * значение - нечётное число.
     * <p>
     * Если все поля в группе не меняют абсолютное значение (и, как следствие, не меняют свою чётность), то они
     * вырезаются как обычно.
     */
    @Test
    public void complexGroupRemove() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 чётный
                        // group2firstBit2 нечётный
                        Pair.of("group2firstBit1", "4"),
                        Pair.of("foo", "foo"),
                        Pair.of("group2firstBit2", "3")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 чётный
                        // group2firstBit2 нечётный
                        Pair.of("group2firstBit2", "3"),
                        Pair.of("group2firstBit1", "4"),
                        Pair.of("foo", "foo")))));
        RowModelPair expected = new RowModelPair<>(new TestRowModel(), new TestRowModel());

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Сложная группа, с указанной фунцией извлечения данных.
     * В этом тесте группа содержит два поля. У каждого поля одинаковая функция для сравнения - возвращает true, если
     * значение - нечётное число.
     * <p>
     * Проверяется, что независимо от того, какие сложные группы объявлены, в after остаются только те поля,
     * у которых не изменились необработанные значения.
     */
    @Test
    public void complexGroupKeepOnlyNewInAfter() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 чётный
                        // group2firstBit2 нечётный
                        Pair.of("group2firstBit1", "4"),
                        Pair.of("foo", "foo"),
                        Pair.of("group2firstBit2", "3")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 стал нечётным
                        // group2firstBit2 не изменился
                        Pair.of("group2firstBit2", "3"),
                        Pair.of("group2firstBit1", "17"),
                        Pair.of("foo", "foo")))));
        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        Pair.of("group2firstBit1", "4"),
                        Pair.of("group2firstBit2", "3")))),
                new TestRowModel(fromPairs(Collections.singletonList(
                        // group2firstBit2 отсутствует в after так как не изменился
                        Pair.of("group2firstBit1", "17")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    /**
     * Сложная группа, с указанной фунцией извлечения данных.
     * В этом тесте группа содержит два поля. У каждого поля одинаковая функция для сравнения - возвращает true, если
     * значение - нечётное число.
     * <p>
     * Если необработанное значение у поля изменилось, то его не следует вырезать ни из before, ни из after, независимо
     * от настроек групп. Стреляло в DIRECT-75374.
     */
    @Test
    public void complexGroupKeepInBeforeAndAfterIfRawValueChanged() throws Exception {
        RowModelPair source = new RowModelPair<>(
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 чётный
                        // group2firstBit2 нечётный
                        Pair.of("group2firstBit1", "888"),
                        Pair.of("foo", "foo"),
                        Pair.of("group2firstBit2", "3")))),
                new TestRowModel(fromPairs(Arrays.asList(
                        // group2firstBit1 изменился, но остался чётным
                        // group2firstBit2 не изменился
                        Pair.of("group2firstBit1", "14"),
                        Pair.of("group2firstBit2", "3"),
                        Pair.of("foo", "foo")))));

        RowModelPair expected = new RowModelPair<>(
                new TestRowModel(fromPairs(Collections.singletonList(
                        // group2firstBit2 отсутствует в before так как не изменился и так как чётность группы не
                        // изменилась
                        Pair.of("group2firstBit1", "888")))),
                new TestRowModel(fromPairs(Collections.singletonList(
                        // group2firstBit2 отсутствует в after так как не изменился и так как чётность группы не
                        // изменилась
                        Pair.of("group2firstBit1", "14")))));

        handlerBuilder.build().handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    private FieldValueList fromPairs(List<Map.Entry<String, String>> pairs) {
        return new FieldValueList(pairs.stream().map(FieldValue::new).collect(Collectors.toList()));
    }
}
