package ru.yandex.direct.useractionlog.writer.generator;

import java.util.Arrays;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.useractionlog.dict.DictResponsesAccessor;
import ru.yandex.direct.useractionlog.model.RowModel;
import ru.yandex.direct.useractionlog.model.RowModelPair;
import ru.yandex.direct.useractionlog.model.TestRowModel;

@ParametersAreNonnullByDefault
public class FilterFieldsStrategyTest {
    private static final DictResponsesAccessor EMPTY_ACCESSOR =
            new DictResponsesAccessor(Collections.emptyMap());
    private final FilterFieldsStrategy handler = new FilterFieldsStrategy("bad1", "bad2", "bad3");
    private FieldValueList allGood = FieldValueList.zip(
            Arrays.asList("good1", "good2", "good3"),
            Arrays.asList("xxx", "", null));
    private FieldValueList allBad = FieldValueList.zip(
            Arrays.asList("bad1", "bad2", "bad3"),
            Arrays.asList("xxx", "", null));
    private FieldValueList mixed = FieldValueList.zip(
            Arrays.asList("good1", "bad1", "good2", "bad2", "good3", "bad3"),
            Arrays.asList("xxx", "xxx", "", "", null, null));

    @Test
    public void handleInsertMixed() throws Exception {
        RowModel source = new TestRowModel(mixed);
        RowModel expected = new TestRowModel(allGood);
        handler.handleInsert(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleUpdateMixed() throws Exception {
        RowModelPair source = new RowModelPair<>(new TestRowModel(mixed), new TestRowModel(mixed));
        RowModelPair expected = new RowModelPair<>(new TestRowModel(allGood), new TestRowModel(allGood));
        handler.handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleDeleteMixed() throws Exception {
        RowModel source = new TestRowModel(mixed);
        RowModel expected = new TestRowModel(allGood);
        handler.handleDelete(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleInsertAllGood() throws Exception {
        RowModel source = new TestRowModel(allGood);
        RowModel expected = new TestRowModel(allGood);
        handler.handleInsert(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleUpdateAllGood() throws Exception {
        RowModelPair source = new RowModelPair<>(new TestRowModel(allGood), new TestRowModel(allGood));
        RowModelPair expected = new RowModelPair<>(new TestRowModel(allGood), new TestRowModel(allGood));
        handler.handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleDeleteAllGood() throws Exception {
        RowModel source = new TestRowModel(allGood);
        RowModel expected = new TestRowModel(allGood);
        handler.handleDelete(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleInsertAllBad() throws Exception {
        RowModel source = new TestRowModel(allBad);
        RowModel expected = new TestRowModel();
        handler.handleInsert(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleUpdateAllBad() throws Exception {
        RowModelPair source = new RowModelPair<>(new TestRowModel(allBad), new TestRowModel(allBad));
        RowModelPair expected = new RowModelPair<>(new TestRowModel(), new TestRowModel());
        handler.handleUpdate(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void handleDeleteAllBad() throws Exception {
        RowModel source = new TestRowModel(allBad);
        RowModel expected = new TestRowModel();
        handler.handleDelete(source, EMPTY_ACCESSOR);
        Assertions.assertThat(source).isEqualToComparingFieldByField(expected);
    }
}
