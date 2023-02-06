package ru.yandex.market.mbi.msapi.clicks;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbi.msapi.grouping.universal.ApiVersion;
import ru.yandex.market.mbi.msapi.handler.fields.TransId;

import static org.mockito.Mockito.when;

/**
 * @author vbudnev
 */
public class JdbcEntitySaverTest {

    /**
     * Smoke тест на то, что в случае наличия "null" строки среди значений, она будет корректно преобразована к null,
     * и NullBinder, если есть, отработает корректно, не пропуская обработку до типизированного.
     *
     * @throws SQLException
     */
    @Test
    public void test_smokeTestOnNullValueBehavior() throws SQLException {
        JdbcEntitySaver saver = new JdbcEntitySaver();
        saver.setBinders(
                ImmutableMap.of(
                        "valWithoutNull",
                        new NullableBinder<>(new IntegerBinder()),
                        "valWithNull",
                        new NullableBinder<>(new IntegerBinder()),
                        "valCantHoldNull",
                        new IntegerBinder()
                )
        );

        MarketstatApiLine apiLine = new MarketstatApiLine(
                new String[] {"123", "", "789123"},
                ImmutableMap.of(
                        "valWithoutNull", 0,
                        "valWithNull", 1,
                        "valCantHoldNull", 2
                )
        );

        PreparedStatement ps = Mockito.mock(PreparedStatement.class);
        TransId transId = Mockito.mock(TransId.class);
        when(transId.getId()).thenReturn(123L);
        when(transId.getApiVersion()).thenReturn(ApiVersion.LOGBROKER_API);

        apiLine.setTransId(transId);
        apiLine.setTrantime(1521210664L);

        saver.init();

        saver.saveLine(apiLine, ps);
    }

}
