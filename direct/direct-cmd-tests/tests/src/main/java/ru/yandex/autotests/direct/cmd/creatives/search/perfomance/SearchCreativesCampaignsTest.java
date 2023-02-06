package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.CreativeCamp;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.beans.creatives.CreativeTemplate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Правильные id кампаний, к которым привязан креатив, " +
        "в ответе ручки searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.CAMPAIGNS_LIST)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
public class SearchCreativesCampaignsTest {

    private static final String SUPER = Logins.SUPER;
    private static final String CLIENT = "at-direct-search-creatives1";
    private static final CreativeTemplate CREATIVE_TEMPLATE =
            CreativeTemplate.MANY_OFFERS_MANY_DESCRIPTIONS_AND_CAROUSEL_240x400;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();


    private PerformanceBannersRule bannersRule1 = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    private PerformanceBannersRule bannersRule2 = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2);


    private long clientId;
    private int shard;
    private PerfCreativesRecord dbCreative;

    @Before
    public void before() {
        clientId = Long.valueOf(cmdRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID());
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);
    }

    @After
    public void after() {
        if (dbCreative != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .perfCreativesSteps().deletePerfCreatives(dbCreative.getCreativeId().longValue());
            dbCreative = null;
        }
    }

    @Test
    @Description("Список кампаний креатива, не привязанного ни к одной кампании, пуст " +
            "(в ответе ручки searchCreatives)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9674")
    public void testSearchCreativesCampaignIdsEmpty() {
        createCreative();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));

        Creative creative = getCreative(dbCreative.getName(), dbCreative.getCreativeId().longValue());
        List<CreativeCamp> usedInCampsActual = creative.getUsedInCamps();

        assertThat("список кампаний креатива на ручке searchCreatives пуст",
                usedInCampsActual, emptyIterable());
    }

    @Test
    @Description("Список кампаний креатива, привязанного к 1 кампании, содержит эту 1 кампанию " +
            "(в ответе ручки searchCreatives)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9675")
    public void testSearchCreativesCampaignIdsOne() {
        PerfCreativesRecord dbCreative = getDbCreative(bannersRule1.getCreativeId());

        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));

        Creative creative = getCreative(dbCreative.getCreativeId().toString(), dbCreative.getCreativeId());
        List<CreativeCamp> usedInCampsActual = creative.getUsedInCamps();
        List<CreativeCamp> usedInCampsExpected = new ArrayList<>(
                Collections.singletonList(new CreativeCamp().withCampaignId(bannersRule1.getCampaignId())));

        assertThat("список кампаний креатива на ручке searchCreatives содержит одну кампанию",
                usedInCampsActual,
                BeanDifferMatcher.
                        beanDiffer(usedInCampsExpected).
                        useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("Список кампаний креатива, привязанного к 2 кампаниям, содержит эти 2 кампании " +
            "(в ответе ручки searchCreatives)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9676")
    public void testSearchCreativesCampaignIdsMany() {
        long creativeId = bannersRule1.getCreativeId();
        PerfCreativesRecord dbCreative = getDbCreative(creativeId);

        // привязываем баннер одной кампании к креативу другой
        connectBannerWithCreative(bannersRule2.getBannerId(), creativeId);

        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));

        Creative creative = getCreative(dbCreative.getCreativeId().toString(), dbCreative.getCreativeId());
        List<CreativeCamp> usedInCampsActual = creative.getUsedInCamps();
        List<CreativeCamp> usedInCampsExpected = new ArrayList<>(Arrays.asList(
                new CreativeCamp().withCampaignId(bannersRule1.getCampaignId()),
                new CreativeCamp().withCampaignId(bannersRule2.getCampaignId())));

        assertThat("список кампаний креатива на ручке searchCreatives содержит 2 кампании",
                usedInCampsActual,
                BeanDifferMatcher.
                        beanDiffer(usedInCampsExpected).
                        useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    private void createCreative() {
        String creativeName = randomAlphabetic(5);
        long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().
                saveDefaultPerfCreativesForClient(creativeName, CREATIVE_TEMPLATE, clientId);
        dbCreative = getDbCreative(creativeId);
    }

    private PerfCreativesRecord getDbCreative(long creativeId) {
        return TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().getPerfCreatives(creativeId);
    }

    private Creative getCreative(String name, long id) {
        List<Creative> creatives = cmdRule.cmdSteps().creativesSteps().searchCreatives(name);
        return creatives.
                stream().
                filter(c -> c.getCreativeId() == id).
                findFirst().
                orElseThrow(() -> new AssumptionException("в ответе ручки searchCreatives отсутствует созданный креатив"));
    }

    private void connectBannerWithCreative(long bid, long creativeId) {
        BannersPerformanceRecord bannersPerformanceRecord = TestEnvironment.newDbSteps().useShard(shard).bannersPerformanceSteps()
                .findBannersPerformance(bid).get(0);
        bannersPerformanceRecord.setCreativeId(creativeId);
        TestEnvironment.newDbSteps().useShard(shard).bannersPerformanceSteps().updateBannersPerformanceById(bannersPerformanceRecord);
    }
}
