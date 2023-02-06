package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.data.groups.DynamicGroupSource;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
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
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Невозможность изменения фида дто группы")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SaveDynamicGroupFeedValidationTest {

    protected static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private DynamicBannersRule bannersRule;
    private Group savingGroup;

    public SaveDynamicGroupFeedValidationTest(DynamicGroupSource source) {
        bannersRule = new DynamicBannersRule().withSource(source).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Невозможность изменения фида у дто группы с {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {DynamicGroupSource.DOMAIN},
                {DynamicGroupSource.FEED}
        });
    }

    @Before
    public void before() {
        String feedId = String.valueOf(createDefaultFeed());
        savingGroup = bannersRule.getCurrentGroup()
                .withHrefParams("")
                .withTags(emptyList())
                .withMainDomain("")
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()))
                .withFeedId(feedId)
                .withHasFeedId("1");
        savingGroup.getBanners().get(0).withImage("");
        savingGroup.getDynamicConditions().get(0).setConditions(BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class).getConditions());
    }

    @Test(expected = HttpClientLiteException.class)
    @Description("Невозможность изменения фида дто группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9795")
    public void checkChangeDynamicFeedGroup() {
        ErrorResponse errorResponse = cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroupsInvalidData(
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savingGroup));
        assertThat("ошибка совпадает с ожидаемой", errorResponse.getError(), equalTo("error"));
    }

    private Long createDefaultFeed() {
        FeedsRecord defaultFeed = FeedSaveRequest.getDefaultFeed(User.get(CLIENT).getClientID());
        defaultFeed.setUpdateStatus(FeedsUpdateStatus.Done);
        defaultFeed.setOffersCount(2L);
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).feedsSteps()
                .createFeed(defaultFeed, User.get(CLIENT).getClientID());
    }


}
