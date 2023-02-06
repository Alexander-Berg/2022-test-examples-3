package ru.yandex.direct.web.entity.inventori.service;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.asynchttp.AsyncHttpExecuteException;
import ru.yandex.direct.web.core.entity.inventori.model.ReachOutdoorResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Ignore //todo выключил в рамках рефакторинга, переделать или удалить в рамках DIRECT-104384
public class InventoriWebServiceReachOutdoorTest extends ReachOutdoorBaseTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void getReachOutdoorForecast_inventoriOkResponse() {
        inventoriSuccessResponse();

        ReachOutdoorResult actualResult = inventoriWebService.getReachOutdoorForecast(defaultRequest());
        ReachOutdoorResult expectedResult = (ReachOutdoorResult) new ReachOutdoorResult()
                .withReach(1000L)
                .withOtsCapacity(2000L);

        assertThat(actualResult, beanDiffer(expectedResult).useCompareStrategy(REACH_OUTDOOR_RESULT_STRATEGY));
    }

    @Test
    public void getReachOutdoorForecast_inventoriLessThanResponse() {
        inventoriSuccessLessThanResponse();

        ReachOutdoorResult actualResult = inventoriWebService.getReachOutdoorForecast(defaultRequest());
        ReachOutdoorResult expectedResult = (ReachOutdoorResult) new ReachOutdoorResult()
                .withReachLessThan(3000L);

        assertThat(actualResult, beanDiffer(expectedResult).useCompareStrategy(REACH_OUTDOOR_RESULT_STRATEGY));
    }

    @Test(expected = IllegalStateException.class)
    public void getReachOutdoorForecast_inventoriBadResponse() {
        inventoriBadResponse();
        inventoriWebService.getReachOutdoorForecast(defaultRequest());
    }

    @Test(expected = AsyncHttpExecuteException.class)
    public void getReachOutdoorForecast_inventoriExceptionResponse() {
        inventoriExceptionResponse();

        inventoriWebService.getReachOutdoorForecast(defaultRequest());
    }
}
