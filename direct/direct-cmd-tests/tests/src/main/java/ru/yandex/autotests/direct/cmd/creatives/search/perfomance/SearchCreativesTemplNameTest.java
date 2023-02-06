package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.beans.creatives.CreativeTemplate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Правильное название шаблона креатива в ответе ручки searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.TEMPLATE_NAME)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SearchCreativesTemplNameTest {

    private static final String CLIENT = "at-direct-search-creatives1";

    private static PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule().useAuth(true).withRules(bannersRule);
    private static long clientId;
    private static int shard;
    @Parameterized.Parameter
    public CreativeTemplate template;
    private String creativeName;
    private long creativeId;

    @Parameterized.Parameters
    public static Collection<Object[]> searchTexts() {
        return Arrays.asList(CreativeTemplate.values()).
                stream().
                map(ct -> new CreativeTemplate[]{ct}).
                collect(Collectors.toList());
    }

    @BeforeClass
    public static void beforeClass() {
        clientId = Long.valueOf(cmdRule.darkSideSteps().getClientFakeSteps().getClientData(CLIENT).getClientID());
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByClientID(clientId);
    }

    @After
    public void after() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps().deletePerfCreatives(creativeId);
    }

    @Test
    @Description("Правильное название шаблона креатива в ответе ручки searchCreative (страница креативов)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9677")
    public void testSearchCreativesTemplateName() {
        createCreative();
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        Creative creative = getCreative();

        String expectedBsTemplateName = template.getDescription();
        String actualBsTemplateName = creative.getBsTemplateName();
        assertThat("название шаблона креатива в ответе ручки searchCreatives соответствует ожидаемому",
                actualBsTemplateName, equalTo(expectedBsTemplateName));
    }

    private void createCreative() {
        creativeName = RandomStringUtils.randomAlphabetic(5);
        creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .perfCreativesSteps().saveDefaultPerfCreativesForClient(
                        creativeName, template, clientId);
    }

    private Creative getCreative() {
        List<Creative> creatives = cmdRule.cmdSteps().creativesSteps().searchCreatives(creativeName);
        return creatives.
                stream().
                filter(c -> c.getCreativeId() == creativeId).
                findFirst().
                orElseThrow(() -> new AssumptionException("в ответе ручки searchCreatives отсутствует созданный креатив"));
    }

}
