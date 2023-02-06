package ru.yandex.autotests.directintapi.tests.smoke;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by pavryabov on 27.04.15.
 * https://st.yandex-team.ru/TESTIRT-4861
 * https://st.yandex-team.ru/TESTIRT-10356
 */
@Aqua.Test()
@Features(FeatureNames.INCREMENT_OFFSET_MONITORING)
@Description("Проверка инкремента и оффсета в базах Директа")
@RunWith(Parameterized.class)
public class DevtestDBIncrementTest extends TCDBIncrementBaseTest {

    public DevtestDBIncrementTest(String dbName, String host, long offset) {
        super(dbName, host, offset);
    }

    @Parameterized.Parameters(name = "db={0}")
    public static Collection data() {
        Object[][] data = new Object[][]{
                {"devtest", "http://8998.beta1.direct.yandex.ru", 1},
                {"dev7", "http://8996.beta1.direct.yandex.ru", 2}
        };
        return Arrays.asList(data);
    }

}
