package ru.yandex.market.common.test.jdbc.functions;

import com.google.common.base.Joiner;
import org.h2.api.AggregateFunction;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Агрегатная функция для конкатенации строк.
 *
 * @author otedikova
 */
public class StringAggregateFunction implements AggregateFunction {
    private final List<String> values = new ArrayList<>();

    @Override
    public void init(Connection conn) {

    }

    @Override
    public int getType(int[] inputTypes) {
        return Types.VARCHAR;
    }

    @Override
    public void add(Object value) {
        if (value != null) {
            values.add(String.valueOf(value));
        }
    }

    @Override
    public Object getResult() {
        return Joiner.on("").skipNulls().join(values);
    }
}
