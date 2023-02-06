package ru.yandex.autotests.direct.httpclient.showstat;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.exceptions.HttpClientIOException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Aqua.Test
@Description("Проверка кодов ответов БК при запросе статистики через мастер отчетов, часть 2")
@Features(TestFeatures.SHOW_STAT_RESPONSE_CHECKER)
@RunWith(Parameterized.class)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class MasterOfReportsResponseChecker2Test extends MasterOfReportsBaseTest {
    @Parameterized.Parameters(name = "url = {0}")
    public static List testData() {
        try {
            List<String> it = IOUtils.readLines(MasterOfReportsBaseTest.class.getClassLoader().getResourceAsStream("url_part2.list"));
            List<String[]> testUrls = new ArrayList<>();

            for (String s : it) {
                testUrls.add(new String[]{s});
            }
            return testUrls;
        } catch (IOException e) {
            throw new HttpClientIOException("Не удалось прочитать список тестовых урлов из ресурсов", e);
        }
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10574")
    public void testHTTPResponseCodesInMasterOfReports() {
        super.testHTTPResponseCodesInMasterOfReports();
    }
}
