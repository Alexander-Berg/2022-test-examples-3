package ru.yandex.market.clickhouse;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.h2.api.AggregateFunction;

public class UniqExactDistinctIf implements AggregateFunction {
    private Set<Object> result;

    @Override
    public void init(Connection conn) throws SQLException {
        this.result = new HashSet<>();
    }

    @Override
    public int getType(int[] inputTypes) throws SQLException {
        if (inputTypes.length != 2) {
            throw new java.sql.SQLException("The aggregate function uniqExactDistinctIf must have 2 arguments.");
        }
        return inputTypes[0];
    }

    @Override
    public void add(Object o) throws SQLException {
        Object[] objects = (Object[]) o;
        Object value = objects[0];
        boolean condition;
        if (objects[1] instanceof BigDecimal) {
            condition = ((BigDecimal) objects[1]).intValue() > 0;
        } else if (objects[1] instanceof Boolean) {
            condition = (boolean) objects[1];
        } else {
            throw new IllegalArgumentException();
        }

        if (condition) {
            result.add(value);
        }
    }

    @Override
    public Object getResult() throws SQLException {
        return result.size();
    }
}
