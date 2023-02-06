package ru.yandex.autotests.direct.web.cleandata;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.AdGetItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.FeedsRecord;
import ru.yandex.autotests.direct.web.TestEnvironment;
import ru.yandex.autotests.direct.web.steps.UserSteps;
import ru.yandex.autotests.direct.web.util.WebRuleFactory;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.adgroups.AdGroupsSelectionCriteriaMap;
import ru.yandex.autotests.directapi.model.api5.adgroups.GetRequestMap;
import ru.yandex.autotests.directapi.model.api5.ads.AdsSelectionCriteriaMap;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.webdriver.rules.WebDriverConfiguration;

import static ru.yandex.autotests.direct.web.TestEnvironment.getWebDriverConfiguration;
import static ru.yandex.autotests.direct.web.TestEnvironment.newDbSteps;

@Aqua.Test
@Title("Удаление кампаний и объектов на клиентах, которые используются для асессоров")
@Stories("Удаление кампаний и объектов")
@Features("Очистка тестовых данных")
// -----------------------------
// Обязательно нужно учитывать, что в данных для асессоров могут быть сохранены какие-то группы, баннеры, кампании
// которые нельзя удалять, так как на них асессоры что-то смотрят. Поэтому во многих очищалках захардкожены id объектов,
// которые нужно пропускать при удалении
//-----------------------------
public class CleanTestDataForAssessorsTest {

    public WebDriverConfiguration config = getWebDriverConfiguration();
    @ClassRule
    public static RuleChain defaultClassRuleChain = WebRuleFactory.defaultClassRuleChain();
    @Rule
    public RuleChain defaultRuleChain = WebRuleFactory.defaultRuleChain(config);

    public UserSteps user;

    @Before
    public void before() {
        user = UserSteps.getInstance(UserSteps.class, config);
    }


    @Test
    @Title("Удаление фидов клиента yndx-canvas-assessors-test")
    @Description("Удаление фидов клиента yndx-canvas-assessors-test")
    public void delFeedsYndxCanvasAssessors() {
        String clientLogin = "yndx-canvas-assessors-test";
        int saveLastFeedsNumber = 10;
        int saveFirstFeedsNumber = 5;
//        Long savedFeedId = 558493L;

        List<FeedsRecord> feeds = newDbSteps().useShardForLogin(clientLogin).feedsSteps()
                .getFeeds(Long.parseLong(User.get(clientLogin).getClientID()));
        if (feeds.size() > saveFirstFeedsNumber + saveLastFeedsNumber) {
            for (int i = saveFirstFeedsNumber; i < feeds.size() - saveLastFeedsNumber; i++) {
                if (feeds.get(i).getFeedId() != 558493L) {
                    newDbSteps().useShardForLogin(clientLogin).feedsSteps().deleteFeedById(feeds.get(i).getFeedId());
                }
            }
        }
    }

    @Test
    @Title("Удаление групп кампании клиента yndx-canvas-assessors-test")
    @Description("Удаление групп кампании клиента yndx-canvas-assessors-test")
    public void delAdGroupsYndxCanvasAssessors() {
        String clientLogin = "yndx-canvas-assessors-test";
        Long campaignId = 289042008L;
        int saveLastGroupsNumber = 10;
        // Long savedGroupId = 4863397103L;

        List<AdGroupGetItem> expectedGroups = TestEnvironment.getApiUserSteps().adGroupsSteps().adGroupsGet(
                new GetRequestMap()
                        .withSelectionCriteria(new AdGroupsSelectionCriteriaMap().withCampaignIds(campaignId))
                        .withFieldNames(AdGroupFieldEnum.values()),
                clientLogin).getAdGroups();

        for (int i = 0; i < expectedGroups.size() - saveLastGroupsNumber; i++) {
            if (expectedGroups.get(i).getId() != 4863397103L) {
                TestEnvironment.newDbSteps().useShardForLogin(clientLogin).adgroupsPerformanceSteps()
                        .deleteAdgroupsPerformance(expectedGroups.get(i).getId());
                TestEnvironment.newDbSteps().useShardForLogin(clientLogin).adGroupsSteps().deleteAdGroup(expectedGroups.get(i).getId());
            }
        }
    }

    @Test
    @Title("Удаление кампаний клиента yndx-canvas-assessors-test")
    @Description("Удаление кампаний клиента yndx-canvas-assessors-test")
    public void delCampsCanvasAssessors() {
        List<Long> savedCampaigns =  new ArrayList<>();
        // Кампании с базы test
        Collections.addAll(savedCampaigns, 253562608L, 253998608L, 254015938L, 254030658L, 262224488L, 303960738L,
                275637723L, 275949428L, 277542348L, 277719328L, 282074753L, 285501573L, 286950773L, 289042008L, 254040533L,
                264955653L, 264991393L, 268730143L, 272636153L, 268054383L, 295870863L);
        //Кампании с продакшена
        Collections.addAll(savedCampaigns, 63165773L, 63166146L, 63168125L, 63173728L, 63174042L, 63178225L,
                63432013L, 63432164L, 63639772L, 63640531L, 63640585L, 63665165L, 63904278L, 63904399L, 64649260L, 65160805L,
                63172750L,  63664988L, 63927885L, 63928022L, 63928698L, 63932576L, 63961636L, 63961642L, 63997339L,
                63997416L, 65375013L);
        user.byUsingBackend().deleteCampaignsWithDefendedCampaigns("yndx-canvas-assessors-test", 100, 100, savedCampaigns);
    }

    @Test
    @Title("Удаление кампаний клиента yndx-assessors-direct")
    @Description("Удаление кампаний клиента yndx-assessors-direct")
    public void delCampsAssessorsDirect() {
        List<Long> savedCampaigns =  new ArrayList<>();
        // Кампании с базы test
        Collections.addAll(savedCampaigns, 269080628L, 282087828L, 282671668L, 282819458L, 286803848L, 286815438L,
                286835108L, 286901973L, 286950793L, 286950928L, 286951253L, 286981993L, 290534098L, 290535328L, 290568358L,
                290661633L, 290688878L, 291882638L, 294021263L, 294319298L, 281693698L, 286995323L, 282985498L, 283072343L,
                287872398L, 280626653L, 292713758L, 281693198L, 294204218L, 282509388L, 272636118L, 295969443L, 297864173L, 298684968L,
                298940098L, 303716913L, 303717173L, 303684583L, 303719218L, 303819108L, 303958493L);
        //Кампании с продакшена
        Collections.addAll(savedCampaigns, 63633233L, 63633306L, 63998259L, 63998341L, 64007999L, 64008574L,
                64009044L, 66255307L, 63245473L, 63245655L, 63403780L, 63404961L, 63431599L, 63431656L, 63431806L,
                63518324L, 63518324L, 63631283L, 63632796L, 63633734L, 63637295L, 63640856L, 63641210L, 63641402L, 63643132L,
                63744946L, 63745068L, 63745158L, 63745244L, 66211741L, 66245238L, 66248260L, 66253499L, 66271404L, 66504721L,
                66505104L, 71841328L);
        user.byUsingBackend().deleteCampaignsWithDefendedCampaigns("yndx-assessors-direct", 100, 100, savedCampaigns);
    }
}
