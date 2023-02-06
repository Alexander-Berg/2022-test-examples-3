package ru.yandex.market.checkout.helpers.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * @author Nikolai Iusiumbeli
 * date: 18/07/2017
 */
public class ResultActionsContainer implements ResultActions {

    private final List<ResultMatcher> matchers = new ArrayList<>();
    private final List<ResultHandler> handlers = new ArrayList<>();

    @Override
    public ResultActionsContainer andExpect(ResultMatcher matcher) {
        matchers.add(matcher);
        return this;
    }

    @Override
    public ResultActionsContainer andDo(ResultHandler handler) {
        handlers.add(handler);
        return this;
    }

    @Override
    public MvcResult andReturn() {
        return null;
    }

    public List<ResultMatcher> getMatchers() {
        return matchers;
    }

    public List<ResultHandler> getHandlers() {
        return handlers;
    }

    public void clear() {
        matchers.clear();
        handlers.clear();
    }

    public ResultActions propagateResultActions(ResultActions resultActions) throws
            Exception {
        for (ResultMatcher resultMatcher : this.getMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
        for (ResultHandler resultHandler : this.getHandlers()) {
            resultActions.andDo(resultHandler);
        }
        return resultActions;
    }
}
