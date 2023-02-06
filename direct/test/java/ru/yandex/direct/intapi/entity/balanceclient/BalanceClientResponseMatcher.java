package ru.yandex.direct.intapi.entity.balanceclient;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.intapi.entity.balanceclient.model.BalanceClientResult;
import ru.yandex.direct.utils.JsonUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class BalanceClientResponseMatcher implements ResultMatcher {
    private final int expectedCode;
    private final String expectedMessage;
    private final String reason;
    private final boolean matchCode;
    private final boolean matchMessage;

    public BalanceClientResponseMatcher(String reason, int expectedCode, String expectedMessage, boolean matchCode,
                                        boolean matchMessage) {
        this.reason = reason;
        this.expectedCode = expectedCode;
        this.expectedMessage = expectedMessage;
        this.matchCode = matchCode;
        this.matchMessage = matchMessage;
    }

    public BalanceClientResponseMatcher(String reason, int expectedCode, String expectedMessage) {
        this(reason, expectedCode, expectedMessage, true, true);
    }

    public BalanceClientResponseMatcher(String reason, int expectedCode) {
        this(reason, expectedCode, null, true, false);
    }

    public static BalanceClientResponseMatcher ncAnswerOk() {
        return new BalanceClientResponseMatcher("Ручка ответила Ок", 0, "");
    }

    public static BalanceClientResponseMatcher ncAnswerWarning(String message) {
        return new BalanceClientResponseMatcher("Ручка ответила предупреждением", 0, message);
    }

    public static BalanceClientResponseMatcher ncAnswerCriticalError() {
        return new BalanceClientResponseMatcher("Ручка ответила критической ошибкой",
                BalanceClientServiceConstants.CRITICAL_ERROR);
    }

    @Override
    public void match(MvcResult result) throws Exception {
        String actualTextContent = result.getResponse().getContentAsString();
        BalanceClientResult actual =
                JsonUtils.fromJson(actualTextContent, BalanceClientResult.class);
        List<BeanFieldPath> names = new ArrayList<>();
        if (matchCode) {
            names.add(BeanFieldPath.newPath(BalanceClientResult.RESPONSE_CODE_FIELD_NAME));
        }
        if (matchMessage) {
            names.add(BeanFieldPath.newPath(BalanceClientResult.RESPONSE_MESSAGE_FIELD_NAME));
        }
        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(names.toArray(new BeanFieldPath[names.size()]));
        assertThat(reason, actual, beanDiffer(new BalanceClientResult(expectedCode, expectedMessage))
                .useCompareStrategy(strategy));
    }
}
