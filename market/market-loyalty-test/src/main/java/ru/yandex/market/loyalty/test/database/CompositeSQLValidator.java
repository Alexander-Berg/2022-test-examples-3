package ru.yandex.market.loyalty.test.database;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeSQLValidator implements SQLValidator {
    private final List<SQLValidator> delegates;
    private volatile boolean enabled;

    public CompositeSQLValidator(SQLValidator... delegates) {
        this.delegates = ImmutableList.copyOf(delegates);
        this.enabled = true;
    }

    @Override
    public void validate(String sql) {
        if (enabled) {
            delegates.forEach(sqlValidator -> sqlValidator.validate(sql));
        }
    }

    public void startTest() {
        delegates.forEach(SQLValidator::startTest);
    }

    public List<String> finishTest() {
        return delegates.stream()
                .map(SQLValidator::finishTest)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
