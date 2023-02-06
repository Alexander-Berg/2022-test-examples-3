package ru.yandex.market.clickhouse;

import java.math.BigDecimal;

import org.h2.api.AggregateFunction;

public class SumIf implements AggregateFunction {
    private BigDecimal result;

    @Override
    public void init(java.sql.Connection cnctn) {
        this.result = BigDecimal.ZERO;
    }

    @Override
    public int getType(int[] ints) throws java.sql.SQLException {
        if (ints.length != 2) {
            throw new java.sql.SQLException("The aggregate function sumIf must have 2 arguments.");
        }
        return ints[0];
    }

    @Override
    public void add(Object o) {
        Object[] objects = (Object[]) o;
        BigDecimal value = (BigDecimal) objects[0];
        boolean condition;
        if (objects[1] instanceof BigDecimal) {
            condition = ((BigDecimal) objects[1]).intValue() > 0;
        } else if (objects[1] instanceof Boolean) {
            condition = (boolean) objects[1];
        } else {
            throw new IllegalArgumentException();
        }

        if (condition) {
            result = result.add(value);
        }
    }

    @Override
    public Object getResult() {
        return result;
    }
}
