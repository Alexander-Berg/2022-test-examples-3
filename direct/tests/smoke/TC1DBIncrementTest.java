package ru.yandex.autotests.directintapi.tests.smoke;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

/**
 * Created by pashkus 03.10.2016
 * https://st.yandex-team.ru/TESTIRT-10356
 */

@Aqua.Test()
@Features(FeatureNames.INCREMENT_OFFSET_MONITORING)
@Description("Проверка инкремента и оффсета в базах Директа")
public class TC1DBIncrementTest extends TCDBIncrementBaseTest {

    public TC1DBIncrementTest() {
        super("TC1", "http://test-direct.yandex.ru", 3);
    }
}
