package ru.yandex.market.common.excel.out.impl;

import java.time.Instant;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class TestDataObject {
    private final String f1;
    private final String f2;
    private final String f3;
    private final String f4;
    private final Integer f5;
    private final Instant f6;

    public TestDataObject(String f1, String f2, String f3, String f4, Integer f5, Instant f6) {
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;
    }

    public String getF1() {
        return f1;
    }

    public String getF2() {
        return f2;
    }

    public String getF3() {
        return f3;
    }

    public String getF4() {
        return f4;
    }

    public Integer getF5() {
        return f5;
    }

    public Instant getF6() {
        return f6;
    }
}
