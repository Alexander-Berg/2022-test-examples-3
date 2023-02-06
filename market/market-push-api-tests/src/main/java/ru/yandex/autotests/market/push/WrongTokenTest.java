package ru.yandex.autotests.market.push;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.pushapi.steps.PushApiCompareSteps;
import ru.yandex.autotests.market.pushapi.request.PushApiRequestData;
import ru.yandex.autotests.market.pushapi.utils.TestDataUtils;

import java.util.Collection;

import static ru.yandex.autotests.market.pushapi.data.WrongTokenRequestData.validWrongTokenRequestWithUrlAuthorization;

/**
 * User: jkt
 * Date: 05.06.13
 * Time: 12:34
 */
@Feature("cart/wrong_token resource")
@Aqua.Test(title = "BackToBack тесты сравнения с сохраненным результатом")
@RunWith(Parameterized.class)
public class WrongTokenTest {

    private PushApiCompareSteps tester = new PushApiCompareSteps();

    private PushApiRequestData requestData;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestDataUtils.asListOfArrays(
                validWrongTokenRequestWithUrlAuthorization()

        );
    }

    public WrongTokenTest(PushApiRequestData requestData) {
        this.requestData = requestData;
    }

    @Before
    public void saveExpected() {
//        tester.saveExpectedToStorage(requestData);   // сохраналка  результатов на элиптикс
    }

    @Test
    public void testCompareWithCachedResponse() {
        tester.compareWithStoredResult(requestData);
    }
}
