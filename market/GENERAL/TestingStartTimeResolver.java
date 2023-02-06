package ru.yandex.market.core.cutoff.timeouts;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import ru.yandex.market.core.cutoff.model.CutoffInfo;

/**
 * @author zoom
 */
public class TestingStartTimeResolver implements Function<CutoffInfo, Optional<Date>> {

    private List<CutoffTimeoutRule> rules;

    public TestingStartTimeResolver(List<CutoffTimeoutRule> rules) {
        this.rules = rules;
    }

    @Override
    public Optional<Date> apply(CutoffInfo cutoff) {
        for (CutoffTimeoutRule rule : rules) {
            if (rule.isApplicableTo(cutoff)) {
                return Optional.of(rule.getStartTestingDate(cutoff));
            }
        }
        return Optional.empty();
    }
}
