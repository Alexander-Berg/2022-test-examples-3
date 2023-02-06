package ru.yandex.autotests.mbi.api.tests;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.market.common.http.response.BackendResponse;
import ru.yandex.autotests.mbi.api.steps.ParamSteps;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by ivmelnik on 05.09.16.
 */
public class ParamTest {

    private static final Long SHOP_ID = 224380L;

    private static final int CPA_REGION_CHECK_STATUS_CODE = 8;

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";

    private ParamSteps paramSteps = new ParamSteps();

    @Ignore
    @Test
    public void test() {
        BackendResponse backendResponse = paramSteps.setParam(SHOP_ID, CPA_REGION_CHECK_STATUS_CODE, STATUS_SUCCESS);
        assertThat(backendResponse.getStatusCode(), is(equalTo(200)));
    }
}
