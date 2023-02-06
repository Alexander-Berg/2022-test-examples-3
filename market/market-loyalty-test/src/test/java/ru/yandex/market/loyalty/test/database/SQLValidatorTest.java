package ru.yandex.market.loyalty.test.database;

import com.google.common.collect.ImmutableMap;
import org.intellij.lang.annotations.Language;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class SQLValidatorTest {
    @Test
    public void shouldAllowSelectIfMultiIndexFirstColumnUsed() {
        DatabaseMetadata metaData = new DatabaseMetadata(new HashMap<>(), ImmutableMap.of("coin", Arrays.asList("uid"
                , "status", "id", "start_date", "end_date")));

        IndexSQLValidator validator = new IndexSQLValidator(metaData, emptyList(), emptyList(), emptyList());
        @Language("PostgreSQL")
        String sql = "SELECT  c.* FROM coin AS c WHERE C.UID = ?";


        validator.startTest();
        validator.validate(sql);
        assertThat(validator.finishTest(), not(is(empty())));

        metaData.addIndex(new TableIndex("coin", false, false, new String[]{"uid", "status"}, null));
        validator.startTest();
        validator.validate(sql);
        assertThat(validator.finishTest(), is(empty()));
    }

    @Test
    public void shouldRejectSelectIfMultiIndexFirstColumnNotUsed() {
        DatabaseMetadata metaData = new DatabaseMetadata(new HashMap<>(), ImmutableMap.of("coin", Arrays.asList("uid"
                , "status", "id", "start_date", "end_date")));

        IndexSQLValidator validator = new IndexSQLValidator(metaData, emptyList(), emptyList(), emptyList());
        @Language("PostgreSQL")
        String sql = "SELECT  c.* FROM coin AS c WHERE C.UID = ?";

        metaData.addIndex(new TableIndex("coin", false, false, new String[]{"status", "uid"}, null));
        validator.startTest();
        validator.validate(sql);
        assertThat(validator.finishTest(), not(is(empty())));
    }

    @Test
    public void shouldAllowUpdateIfTheOnlyIndexColumnUsed() {
        DatabaseMetadata metaData = new DatabaseMetadata(new HashMap<>(), ImmutableMap.of("coin", Arrays.asList("uid"
                , "status", "id", "start_date", "end_date")));

        IndexSQLValidator validator = new IndexSQLValidator(metaData, emptyList(), emptyList(), emptyList());
        @Language("PostgreSQL")
        String sql = "UPDATE coin AS c SET C.status = ? WHERE C.UID = ?";

        metaData.addIndex(new TableIndex("coin", false, false, new String[]{"uid"}, null));
        validator.startTest();
        validator.validate(sql);
        assertThat(validator.finishTest(), is(empty()));
    }

    @Test
    public void shouldRejectUpdateIfNoIndexFound() {
        DatabaseMetadata metaData = new DatabaseMetadata(new HashMap<>(), ImmutableMap.of("coin", Arrays.asList("uid"
                , "status", "id", "start_date", "end_date")));


        IndexSQLValidator validator = new IndexSQLValidator(metaData, emptyList(), emptyList(), emptyList());
        @Language("PostgreSQL")
        String sql = "UPDATE coin AS c SET C.status = ? WHERE C.UID = ?";

        validator.startTest();
        validator.validate(sql);
        assertThat(validator.finishTest(), not(is(empty())));
    }

}
