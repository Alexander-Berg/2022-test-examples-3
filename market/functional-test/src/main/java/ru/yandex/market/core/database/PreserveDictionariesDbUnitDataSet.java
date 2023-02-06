package ru.yandex.market.core.database;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@DbUnitDataSet(
        dataSource = "vendorDataSource",
        nonTruncatedTables = {
                // lqb
                "public.databasechangelog",
                "public.databasechangeloglock",
                // app
                "java_sec.domain",
                "java_sec.authority",
                "java_sec.authority_checker",
                "java_sec.domain",
                "java_sec.op_desc",
                "java_sec.op_perm",
                "java_sec.op_parent",
                "java_sec.perm_auth",
                "java_sec.auth_link"
        })
public @interface PreserveDictionariesDbUnitDataSet {
}
