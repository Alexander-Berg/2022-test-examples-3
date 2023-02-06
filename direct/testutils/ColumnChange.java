package ru.yandex.direct.ess.router.testutils;

public class ColumnChange {
    public Object before;
    public Object after;

    public ColumnChange(Object before, Object after) {
        this.before = before;
        this.after = after;
    }
}
