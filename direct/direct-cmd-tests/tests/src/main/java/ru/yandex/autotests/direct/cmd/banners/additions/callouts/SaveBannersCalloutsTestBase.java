package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public abstract class SaveBannersCalloutsTestBase {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public CampaignRule campaignRule = getCampaignRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    protected CalloutsTestHelper helper;

    public abstract void saveCallouts(String... callouts);

    public abstract void saveCalloutsForExistingGroup(String... callouts);

    public abstract CampaignRule getCampaignRule();

    public abstract String getUlogin();

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(getUlogin(), cmdRule.cmdSteps(),
                campaignRule.getCampaignId().toString());
        helper.clearCalloutsForClient();
    }

    @Description("Сохранение текстовых дополнений")
    public void saveCalloutsForBanner() {
        String callout1 = "callout1";
        String callout2 = "callout2";

        saveCallouts(callout1, callout2);

        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        List<String> callouts = helper.getCalloutsList(showCamp);

        assertThat("в баннере сохранились дополнения", callouts, containsInAnyOrder(callout1, callout2));
    }

    @Description("Обновление дополнений клиента")
    public void updateCalloutsForBannerSavedForClient() {
        String callout1 = "callout1";
        String callout2 = "callout2";
        String callout3 = "callout3";

        saveCallouts(callout1, callout2);

        saveCallouts(callout3);

        List<String> callouts = cmdRule.cmdSteps().bannersAdditionsSteps().getCalloutsList(getUlogin());

        assertThat("на клиенте сохранились все дополнения", callouts, containsInAnyOrder(callout1, callout2, callout3));
    }

    @Description("Обновление текстовых дополнений на баннере")
    public void updateCalloutsForBanner() {
        String callout1 = "callout1";
        String callout2 = "callout2";
        String callout3 = "callout3";

        saveCallouts(callout1, callout2);

        saveCalloutsForExistingGroup(callout3);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        List<String> callouts = helper.getCalloutsList(response);

        assertThat("в баннере сохранились дополнения", callouts, containsInAnyOrder(callout3));
    }

    @Description("Отвязка текстовых дополнений от баннера")
    public void removeCalloutsFromBanner() {
        String callout1 = "callout1";
        String callout2 = "callout2";

        saveCallouts(callout1, callout2);

        saveCalloutsForExistingGroup();

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        List<Callout> callouts = response.getGroups()
                .stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("В ответе нет групп"))
                .getCallouts();

        assertThat("в баннере отсутствуют дополнения", callouts, hasSize(0));
    }

    @Description("Сохранение максимального числа текстовых дополнений на баннер")
    public void saveMaxCalloutsForBanner() {
        String[] expectedCallouts = IntStream.range(0, CalloutsTestHelper.MAX_CALLOUTS_FOR_BANNER)
                .boxed()
                .map(String::valueOf)
                .toArray(String[]::new);

        saveCallouts(expectedCallouts);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        List<String> callouts = helper.getCalloutsList(response);

        assertThat("в баннере сохранились дополнения", callouts, containsInAnyOrder(expectedCallouts));
    }

    @Description("Сохранение порядка тестовых дополнений")
    public void calloutsOrder() {
        String[] expectedCallouts = {"callout1", "callout2", "callout3"};
        saveCallouts(expectedCallouts);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        List<String> callouts = helper.getCalloutsList(response);

        assertThat("в баннере сохранились дополнения", callouts, beanDiffer(Arrays.asList(expectedCallouts)));
    }

    @Description("Изменение порядка тестовых дополнений")
    public void canChangeCalloutsOrder() {
        String[] expectedCallouts = {"callout1", "callout2", "callout3"};
        saveCallouts(expectedCallouts);

        expectedCallouts = new String[]{"callout3", "callout2", "callout1"};
        saveCalloutsForExistingGroup(expectedCallouts);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        List<String> callouts = helper.getCalloutsList(response);

        assertThat("в баннере сохранились дополнения", callouts, beanDiffer(Arrays.asList(expectedCallouts)));
    }

    @Description("Сохранение разных тестовых дополнений для разных баннеров")
    public void canSaveDifferentCalloutsOrderForDifferentBanners() {
        String[] expectedCallouts1 = {"callout1", "callout2", "callout3"};
        saveCallouts(expectedCallouts1);

        String[] expectedCallouts2 = new String[]{"callout3", "callout2", "callout1"};
        saveCallouts(expectedCallouts2);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(getUlogin(),
                campaignRule.getCampaignId().toString());

        assumeThat("Сохранилось 2 группы", response.getGroups(), hasSize(2));

        List<String> callouts1 = Optional.ofNullable(response.getGroups().get(0).getCallouts())
                .orElseThrow(() -> new DirectCmdStepsException("В первой группе отсутствуют дополнения"))
                .stream()
                .map(Callout::getCalloutText)
                .collect(Collectors.toList());

        List<String> callouts2 = Optional.ofNullable(response.getGroups().get(1).getCallouts())
                .orElseThrow(() -> new DirectCmdStepsException("Во второй группе отсутствуют дополнения"))
                .stream()
                .map(Callout::getCalloutText)
                .collect(Collectors.toList());


        assertThat("в баннере 1 сохранились дополнения в нужном порядке",
                callouts1, beanDiffer(Arrays.asList(expectedCallouts1)));
        assertThat("в баннере 2 сохранились дополнения в нужном порядке",
                callouts2, beanDiffer(Arrays.asList(expectedCallouts2)));
    }
}
