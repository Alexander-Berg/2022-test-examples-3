package ru.yandex.market.vendors.analytics.core.utils.dbunit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.TestDatasource;

/**
 * @author antipov93.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@DbUnitDataSet(dataSource = TestDatasource.CLICKHOUSE)
public @interface ClickhouseDbUnitDataSet {

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "schema")
    String schema() default "";

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "before")
    String[] before() default "";

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "after")
    String[] after() default "";

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "type")
    DataSetType type() default DataSetType.SINGLE_CSV;

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "truncateAllTables")
    boolean truncateAllTables() default true;

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "nonTruncatedTables")
    String[] nonTruncatedTables() default {};

    @AliasFor(annotation = DbUnitDataSet.class, attribute = "nonRestartedSequences")
    String[] nonRestartedSequences() default {};

}
