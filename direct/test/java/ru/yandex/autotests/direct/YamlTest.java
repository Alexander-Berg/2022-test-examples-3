package ru.yandex.autotests.direct;

import org.junit.Test;
import ru.yandex.autotests.direct.objects.MonthlyHist;
import ru.yandex.autotests.direct.utils.AdvqError;
import ru.yandex.autotests.direct.utils.YamlHelper;

/**
 * Author xy6er
 * Date 23.05.14
 */
public class YamlTest {

    @Test(expected = AdvqError.class)
    public void canGetWordsData() throws Exception {
        YamlHelper.convertYamlToBean("Not bean", MonthlyHist.class);
    }

}
