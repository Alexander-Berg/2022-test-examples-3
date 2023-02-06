package ru.yandex.market.core.util.filters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.util.db.jdbc.TNumberTbl;
import ru.yandex.market.mbi.util.db.jdbc.TStringTbl;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Created by bfattahov on 05.06.14.
 */
public class FiltersTest {
    private static final String COL_NAME = "COL_NAME";
    private static final String COL_NAME2 = "COL_NAME2";
    public static final String PARAM_NAME = "PARAM_NAME";
    private static final String PARAM_NAME2 = "PARAM_NAME2";
    public static final String VALUE = "VALUE";
    private static final String VALUE2 = "VALUE2";

    private Filter eqFilter = Filters.eq(COL_NAME, PARAM_NAME, VALUE);
    private Filter eqFilter2 = Filters.eq(COL_NAME2, PARAM_NAME2, VALUE2);
    private Filter ltFilter = Filters.lt(COL_NAME, PARAM_NAME, VALUE);
    private Filter leFilter = Filters.le(COL_NAME, PARAM_NAME, VALUE);
    private Filter gtFilter = Filters.gt(COL_NAME, PARAM_NAME, VALUE);
    private Filter geFilter = Filters.ge(COL_NAME, PARAM_NAME, VALUE);
    private Filter notEqFilter = Filters.nonEq(COL_NAME, PARAM_NAME, VALUE);

    private Filter emptyEqFilter = Filters.eq(COL_NAME, PARAM_NAME, null);
    private Filter emptyEqFilter2 = Filters.eq(COL_NAME2, PARAM_NAME2, null);

    private Filter orFilter = Filters.or(eqFilter, eqFilter2);
    private Filter andFilter = Filters.and(eqFilter, eqFilter2);

    private String eqString = COL_NAME + " = :" + PARAM_NAME;
    private String eqString2 = COL_NAME2 + " = :" + PARAM_NAME2;

    private Map<String, Object> paramMap = Map.of(PARAM_NAME, VALUE);
    private Map<String, Object> paramAll = Map.of(
            PARAM_NAME, VALUE,
            PARAM_NAME2, VALUE2
    );

    @Test
    void testSimpleFilter() {
        assertThat(eqFilter).hasToString(eqString);
        assertThat(ltFilter).hasToString(COL_NAME + " < :" + PARAM_NAME);
        assertThat(leFilter).hasToString(COL_NAME + " <= :" + PARAM_NAME);
        assertThat(gtFilter).hasToString(COL_NAME + " > :" + PARAM_NAME);
        assertThat(geFilter).hasToString(COL_NAME + " >= :" + PARAM_NAME);
        assertThat(notEqFilter).hasToString(COL_NAME + " <> :" + PARAM_NAME);
        assertThat(eqFilter.toParameters()).isEqualTo(paramMap);
    }


    @Test
    void testEmptyFilter() {
        assertThat(Filters.EMPTY.isEmpty()).isTrue();
        assertThat(emptyEqFilter.isEmpty()).isTrue();
        assertThat(emptyEqFilter).hasToString("");
        assertThat(emptyEqFilter.toParameters()).isEmpty();
    }

    @Test
    void testCompoundFilter() {
        var halfEmptyOrFilter = Filters.or(eqFilter, emptyEqFilter);
        var emptyOrFilter = Filters.or(emptyEqFilter, emptyEqFilter2);
        assertThat(orFilter).hasToString("(" + eqString + " OR " + eqString2 + ")");
        assertThat(andFilter).hasToString("(" + eqString + " AND " + eqString2 + ")");
        assertThat(halfEmptyOrFilter).hasToString(eqString);
        assertThat(Filters.and(eqFilter, emptyEqFilter)).hasToString(eqString);
        assertThat(Filters.or(emptyEqFilter, eqFilter)).hasToString(eqString);
        assertThat(Filters.and(emptyEqFilter, eqFilter)).hasToString(eqString);
        assertThat(emptyOrFilter).hasToString("");

        assertThat(orFilter.toParameters()).isEqualTo(paramAll);
        assertThat(halfEmptyOrFilter.toParameters()).isEqualTo(paramMap);
        assertThat(emptyOrFilter.toParameters()).isEqualTo(Map.of());

        assertThat(orFilter.isEmpty()).isFalse();
        assertThat(halfEmptyOrFilter.isEmpty()).isFalse();
        assertThat(emptyOrFilter.isEmpty()).isTrue();
        assertThat(
                Filters.TRUE.and(Filters.TRUE.and(Filters.FALSE))
                        .or(Filters.FALSE.and(Filters.TRUE))
                        .and(Filters.TRUE)
                        .and(Filters.TRUE)
        ).hasToString("((((1 = 1 AND (1 = 1 AND 1 = 2)) OR (1 = 2 AND 1 = 1)) AND 1 = 1) AND 1 = 1)");
    }

    @Test
    void testDifficultFilter() {
        var filter = Filters.or(Filters.and(orFilter, andFilter), Filters.or(emptyEqFilter, andFilter));
        assertThat(filter).hasToString(
                "(((COL_NAME = :PARAM_NAME OR COL_NAME2 = :PARAM_NAME2) AND (COL_NAME = :PARAM_NAME AND COL_NAME2 = " +
                        ":PARAM_NAME2)) " +
                        "OR (COL_NAME = :PARAM_NAME AND COL_NAME2 = :PARAM_NAME2))"
        );
    }

    @Test
    void testInFilterEmpty() {
        var filter = Filters.inLongs(COL_NAME, List.of());
        assertThat(filter).hasToString("1 = 2");
        assertThat(Filters.TRUE.and(filter)).hasToString("(1 = 1 AND 1 = 2)");
    }

    @Test
    void testInFilterNullSingle() {
        var values = Collections.<Long>singleton(null);
        var filter = Filters.inLongs(COL_NAME, values);
        assertThat(filter).hasToString("COL_NAME IS NULL");
        assertThat(filter.toParameters()).isEqualTo(Map.of());
    }

    @Test
    void testInFilterIntsSingle() {
        var values = List.of(42);
        var filter = Filters.inInts(COL_NAME, values);
        assertThat(filter).hasToString("COL_NAME = :COL_NAME_array_0");
        assertThat(filter.toParameters()).isEqualTo(Map.of(
                "COL_NAME_array_0", 42
        ));
    }

    @Test
    void testInFilterIntsLarge() {
        var values = makeRangeInts(0, 2100);
        var filter = Filters.inInts(COL_NAME, values);
        assertThat(filter).hasToString(
                " (COL_NAME IN (select /*+ cardinality(t 1)*/ value(t)" +
                        " FROM TABLE (cast(:COL_NAME_array_0 AS shops_web.t_number_tbl)) t))"
        );
        assertThat(filter.toParameters()).isEqualTo(Map.of(
                "COL_NAME_array_0", new TNumberTbl(values.stream()
                        .map(Integer::longValue)
                        .collect(Collectors.toList())
                )
        ));
    }

    @Test
    void testInFilterLongs() {
        var values = makeRangeLongs(0, 2100);
        var filter = Filters.inLongs(COL_NAME, values);
        assertThat(filter).hasToString(
                " (COL_NAME IN (select /*+ cardinality(t 1)*/ value(t)" +
                        " FROM TABLE (cast(:COL_NAME_array_0 AS shops_web.t_number_tbl)) t))"
        );
        assertThat(filter.toParameters()).isEqualTo(Map.of(
                "COL_NAME_array_0", new TNumberTbl(values)
        ));
    }

    @Test
    void testInFilterStrings() {
        var values = makeRangeInts(0, 2100).stream()
                .map(Objects::toString)
                .collect(Collectors.toList());
        var filter = Filters.inStrings(COL_NAME, values);
        assertThat(filter).hasToString(
                " (COL_NAME IN (select /*+ cardinality(t 1)*/ value(t)" +
                        " FROM TABLE (cast(:COL_NAME_array_0 AS shops_web.ntt_varchar2)) t))"
        );
        assertThat(filter.toParameters()).isEqualTo(Map.of(
                "COL_NAME_array_0", new TStringTbl(values)
        ));
    }

    @Test
    void testInFilterArbitrary() {
        var valueDummy = new Object();
        var values = makeRangeInts(0, 2100).stream()
                .map(i -> valueDummy)
                .collect(Collectors.toList());
        var filter = InFilter.of(COL_NAME, values, Object.class);
        assertThat(filter).hasToString(
                "(( (COL_NAME IN (:COL_NAME_array_0))" +
                        " OR  (COL_NAME IN (:COL_NAME_array_1)))" +
                        " OR  (COL_NAME IN (:COL_NAME_array_2)))"
        );
        assertThat(filter.toParameters()).isEqualTo(Map.of(
                "COL_NAME_array_0", values.subList(0, 1000),
                "COL_NAME_array_1", values.subList(1000, 2000),
                "COL_NAME_array_2", values.subList(2000, 2100)
        ));
    }

    private static List<Integer> makeRangeInts(int startInclusive, int endExclusive) {
        return IntStream.range(startInclusive, endExclusive).boxed().collect(Collectors.toList());
    }

    private static List<Long> makeRangeLongs(long startInclusive, long endExclusive) {
        return LongStream.range(startInclusive, endExclusive).boxed().collect(Collectors.toList());
    }

    @Test
    void testInFilter2() {
        var inFilter = Filters.in(COL_NAME, "SELECT 1 FROM DUAL");
        assertThat(inFilter).hasToString(" (" + COL_NAME + " IN (SELECT 1 FROM DUAL))");
    }

    @Test
    void testEmptyFilter2() {
        var emptyFilter = Filters.EMPTY;
        assertThat(emptyFilter.and(emptyFilter).and(emptyFilter).and(Filters.TRUE)).hasToString("1 = 1");
        assertThat(Filters.TRUE.and(emptyFilter).and(emptyFilter).and(Filters.FALSE)).hasToString("(1 = 1 AND 1 = 2)");
        assertThat(emptyFilter).hasToString("");
        assertThat(Filters.TRUE.and(emptyFilter)).hasToString("1 = 1");
        assertThat(emptyFilter.and(Filters.TRUE)).hasToString("1 = 1");
        assertThat(
                Filters.EMPTY
                        .and(Filters.EMPTY)
                        .and(Filters.EMPTY)
                        .and(Filters.EMPTY)
                        .and(Filters.EMPTY)
                        .and(Filters.EMPTY)
                        .and(Filters.TRUE)
        ).hasToString("1 = 1");
    }

    @Test
    void between() {
        var filter = Filters.between(COL_NAME, VALUE, VALUE2);
        assertThat(filter).hasToString("COL_NAME BETWEEN :COL_NAME_From AND :COL_NAME_To");
        assertThat(filter.toParameters()).isEqualTo(Map.of(
                "COL_NAME_From", VALUE,
                "COL_NAME_To", VALUE2
        ));
    }
}
