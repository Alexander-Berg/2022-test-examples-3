package ru.yandex.market.replenishment.autoorder.utils;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

public class CompoziteResultMatcher implements ResultMatcher {
    ResultMatcher[] matchers;

    public CompoziteResultMatcher(ResultMatcher... matchers) {
        this.matchers = matchers;
    }

    @Override
    public void match(MvcResult result) throws Exception {
        for (ResultMatcher matcher : matchers) {
            matcher.match(result);
        }
    }
}
