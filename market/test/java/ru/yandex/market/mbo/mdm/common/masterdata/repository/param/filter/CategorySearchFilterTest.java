package ru.yandex.market.mbo.mdm.common.masterdata.repository.param.filter;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CategorySearchFilterTest {

    @Test
    public void shouldCreateCorrectQueryForNumericsIn() {
        // given
        var filter = CategorySearchFilter.forNumerics(1L,
            CategorySearchFilter.SearchCondition.EQ,
            List.of(BigDecimal.valueOf(100), BigDecimal.valueOf(101)));

        // when
        var where = filter.toWhereCondition();

        // then
        Assertions.assertThat(where).isEqualTo(" where 1 = 1  and mdm_param_id = 1 and numerics[1]  in (100,101)");
    }

    @Test
    public void shouldCreateCorrectQueryForNumericsLte() {
        // given
        var filter = CategorySearchFilter.forNumerics(1L,
            CategorySearchFilter.SearchCondition.LTE,
            List.of(BigDecimal.valueOf(100)));

        // when
        var where = filter.toWhereCondition();

        // then
        Assertions.assertThat(where).isEqualTo(" where 1 = 1  and mdm_param_id = 1 and numerics[1]  <= (100)");
    }

    @Test
    public void shouldCreateCorrectQueryForStringsLike() {
        // given
        var filter = CategorySearchFilter.forStrings(1L,
            CategorySearchFilter.SearchCondition.LIKE,
            List.of("test"));

        // when
        var where = filter.toWhereCondition();

        // then
        Assertions.assertThat(where).isEqualTo(" where 1 = 1  and mdm_param_id = 1 and strings[1]  like ('%test%')");
    }

    @Test
    public void shouldCreateCorrectQueryForStringsNe() {
        // given
        var filter = CategorySearchFilter.forStrings(1L,
            CategorySearchFilter.SearchCondition.NE,
            List.of("test"));

        // when
        var where = filter.toWhereCondition();

        // then
        Assertions.assertThat(where).isEqualTo(" where 1 = 1  and mdm_param_id = 1 and strings[1]  not in ('test')");
    }

    @Test
    public void shouldCreateCorrectQueryForOptionEq() {
        // given
        var filter = CategorySearchFilter.forOptionsIds(1L,
            CategorySearchFilter.SearchCondition.EQ,
            List.of(15L));

        // when
        var where = filter.toWhereCondition();

        // then
        Assertions.assertThat(where).isEqualTo(" where 1 = 1  and mdm_param_id = 1 and (options->0->>'id')::int  in " +
            "(15)");
    }

}
