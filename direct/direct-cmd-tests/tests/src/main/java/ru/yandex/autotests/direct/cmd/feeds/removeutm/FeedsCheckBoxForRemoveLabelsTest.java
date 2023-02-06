package ru.yandex.autotests.direct.cmd.feeds.removeutm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSourceType;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//DIRECT-51290
@Aqua.Test
@Description("Установка чекбокса удаления меток при создании фида")
@Stories(TestFeatures.Feeds.SAVE_FEED)
@Features(TestFeatures.FEEDS)
@Tag(CmdTag.SAVE_FEED)
@Tag(ObjectTag.FEED)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class FeedsCheckBoxForRemoveLabelsTest extends FeedsCheckBoxBaseTest {

    private final static String CLIENT = "cli-smart-feed-1";

    @Parameterized.Parameter(value = 0)
    public String deleteLabelsCheckBox;

    @Parameterized.Parameter(value = 1)
    public String expectedCheckBoxValue;

    @Parameterized.Parameter(value = 2)
    public FeedSourceType source;

    @Parameterized.Parameter(value = 3)
    public String urlFeed;

    @Parameterized.Parameter(value = 4)
    public String fileFeed;

    @Parameterized.Parameters(name = "Чекбокс {0} удаления меток из ссылки фида по ссылке {3} или из файла {4}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"1", "1", FeedSourceType.URL, FEED_URL, null},
                {"0", "0", FeedSourceType.URL, FEED_URL, null},
                {null, "0", FeedSourceType.URL, FEED_URL, null},
                {"1", "1", FeedSourceType.FILE, null, FEED_FILE},
                {"0", "0", FeedSourceType.FILE, null, FEED_FILE},
                {null, "0", FeedSourceType.FILE, null, FEED_FILE},
        });
    }

    @Test
    @Description("Проверяем установку чекбокса удаления меток при создании фида")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9745")
    public void сheckBoxForNewFeed() {
        createFeed();
        assertThat("чекбокс удаления меток выставлен правильно",
                savedFeed.getIsRemoveUtm(), equalTo(expectedCheckBoxValue));
    }

    FeedSourceType getSource() {
        return source;
    }

    String getFeedUrl() {
        return urlFeed;
    }

    String getFeedFile() {
        return fileFeed;
    }

    String getCheckBox() {
        return deleteLabelsCheckBox;
    }

    String getClient() {
        return CLIENT;
    }
}
