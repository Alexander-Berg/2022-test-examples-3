package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.FeedsUpdateStatus;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Сохранение дто группы с фидом")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
public class SaveDynamicGroupFeedTest extends DtoBaseTest {

    private String feedId;

    @Before
    @Override
    public void before() {
        feedId = String.valueOf(createDefaultFeed());
        super.before();
    }

    @Test
    @Description("Создание дто группы с фидом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9794")
    public void checkCreateDynamicFeedGroup() {
        Group actualGroup = getCreatedGroup().getCampaign().getGroups().get(0);
        assertThat("параметры ссылки соотвествуют ожидаемым", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    @Override
    protected Group getDynamicGroup() {
        Group group = super.getDynamicGroup()
                .withMainDomain("")
                .withFeedId(feedId)
                .withHasFeedId("1");
        group.getDynamicConditions().get(0).withConditions(BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class)
                .getConditions());
        return group;
    }

    private Group getExpectedGroup() {
        List<DynamicCondition> dynamicConditions = getDynamicGroup().getDynamicConditions();
        dynamicConditions.get(0).setDynId(null);
        dynamicConditions.get(0).setAutobudgetPriority(null);
        return new Group()
                .withFeedId(feedId)
                .withDynamicConditions(dynamicConditions);
    }

    private Long createDefaultFeed() {
        FeedsRecord defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        defaultFeed.setUpdateStatus(FeedsUpdateStatus.Done);
        defaultFeed.setOffersCount(2L);
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());
    }
}
