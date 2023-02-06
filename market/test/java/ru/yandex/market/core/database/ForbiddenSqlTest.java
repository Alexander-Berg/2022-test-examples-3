package ru.yandex.market.core.database;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ForbiddenSqlTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "select nvl(1,2) from dual",
            "select 1,nvl(1,2),2 from dual",
            "select t1.*,t2.* where t1.id=t2.id(+)",
            "select t1.*,t2.* where t1.id(+)=t2.id",
    })
    void detectsForbiddenPatterns(String sql) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ForbiddenSql.INSTANCE.transform(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "select 1 as nvl from dual", // this is valid sql
            "select 1 as nvlnvl from dual",
            "select custom_nvl(1,2) from dual",
            "select nvl2(1,'1','2') from dual",
    })
    void returnsValidSqlAsIs(String sql) {
        assertThat(ForbiddenSql.INSTANCE.transform(sql))
                .isEqualTo(sql);
    }
}
