package ru.yandex.direct.dbutil.wrapper;

import org.jooq.impl.DSL;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.dbutil.testing.DbUtilTest;

import static org.assertj.core.api.Assertions.assertThat;

@DbUtilTest
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
public class QueryTaggerIntegrationTest {
    @Autowired
    DatabaseWrapperProvider dbProvider;

    @Test
    public void singleQueryContainsComment() throws Exception {
        String query = dbProvider.get(SimpleDb.PPCDICT).getDslContext()
                .select(DSL.field("info", String.class))
                .from("INFORMATION_SCHEMA.PROCESSLIST")
                .where("id = CONNECTION_ID()")
                .fetchOne()
                .value1();

        assertThat(query)
                .contains("/* reqid:");
    }
}
