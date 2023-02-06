package ru.yandex.market.security.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.security.data.kampfer.impl.simple.sql.NextValuable;
import ru.yandex.market.security.data.kampfer.impl.simple.sql.SqlDialect;

import static ru.yandex.market.security.data.kampfer.impl.simple.sql.JavaSecConstants.SEQUENCE;

/**
 * Unit tests for {@link NextValuable}.
 *
 * @author fbokovikov
 */
class NextValuableTest {

    private static final NextValuable NEXT_VALUABLE = new NextValuable() { };

    @Test
    void oracle() {
        String nextval = NEXT_VALUABLE.nextval(SqlDialect.ORACLE, SEQUENCE);
        Assertions.assertEquals(
               "java_sec.s_id.nextval",
               nextval
        );
    }

    @Test
    void postgres() {
        String nextval = NEXT_VALUABLE.nextval(SqlDialect.POSTGRES, SEQUENCE);
        Assertions.assertEquals(
                "nextval('java_sec.s_id')",
                nextval
        );
    }
}
