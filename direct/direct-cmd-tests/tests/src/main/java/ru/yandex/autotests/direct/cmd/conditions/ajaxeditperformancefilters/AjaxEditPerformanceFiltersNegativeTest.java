package ru.yandex.autotests.direct.cmd.conditions.ajaxeditperformancefilters;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.AjaxEditPerformanceFiltersResponse;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilterBannersMap;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFiltersErrors;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.httpclientlite.HttpClientLiteParserException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter.resource;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка получения ошибок при сохранении ДМО фильтра контроллером ajaxEditPerformanceFilters")
@Stories(TestFeatures.Conditions.AJAX_EDIT_PERFORMANCE_FILTERS)
@Features(TestFeatures.CONDITIONS)
@Tag(CmdTag.AJAX_EDIT_PERFORMANCE_FILTERS)
@Tag(ObjectTag.PERFORMANCE_FILTER)
@Tag(CampTypeTag.PERFORMANCE)
public class AjaxEditPerformanceFiltersNegativeTest {

    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-back-perf-filters";
    private static final String WRONG_ID = "111";
    private static final String WRONG_TEXT = "wrong";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected String campaignId;
    protected Long adgroupId;
    protected String filterId;
    protected PerformanceFilter expectedFilter;
    protected PerformanceFilterBannersMap expectedPerformanceFilterBannersMap;
    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        expectedPerformanceFilterBannersMap = new PerformanceFilterBannersMap();

        expectedFilter = getExpectedPerformanceFilter();
        campaignId = bannersRule.getCampaignId().toString();
        adgroupId = bannersRule.getGroupId();

        filterId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId)
                .getGroups().get(0).getPerformanceFilters().get(0).getPerfFilterId();

        expectedPerformanceFilterBannersMap = PerformanceFilterBannersMap
                .forPerformanceFilter(String.valueOf(adgroupId), filterId, expectedFilter);
    }

    protected PerformanceFilter getExpectedPerformanceFilter() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PERFORMANCE_FILTER_DEFAULT, PerformanceFilter.class)
                .withPerfFilterId(null);
    }

    @Test
    @Description("Проверяем сохранение фильтра с некорректным from_tab")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9635")
    public void checkSaveNullTargetFunnel() {
        expectedFilter.setFromTab(WRONG_TEXT);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверяем сохранение фильтра с некорректным target_funnel")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9625")
    public void checkSaveWrongTargetFunnel() {
        expectedFilter.setTargetFunnel(WRONG_TEXT);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверяем сохранение фильтра с пустым value")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9626")
    @Ignore("https://st.yandex-team.ru/TESTIRT-11232")
    public void checkSaveNullFilterValue() {
        expectedFilter.getConditions().get(0).setValue(null);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверяем сохранение фильтра с пустым field")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9627")
    @Ignore("https://st.yandex-team.ru/TESTIRT-11232")
    public void checkSaveNullFilterField() {
        expectedFilter.getConditions().get(0).setField(null);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test(expected = HttpClientLiteParserException.class)
    @Description("Проверяем сохранение фильтра с неверным field")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9628")
    public void checkSaveWrongFilterField() {
        expectedFilter.getConditions().get(0).setField(WRONG_TEXT);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверяем сохранение фильтра с пустым relation")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9629")
    @Ignore("https://st.yandex-team.ru/TESTIRT-11232")
    public void checkSaveNullFilterRelation() {
        expectedFilter.getConditions().get(0).setRelation(null);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test(expected = HttpClientLiteParserException.class)
    @Description("Проверяем сохранение фильтра с неверным relation")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9630")
    public void checkSaveWrongFilterRelation() {
        expectedFilter.getConditions().get(0).setRelation(WRONG_TEXT);
        checkError(CommonErrorsResource.WRONG_INPUT_DATA.toString());
    }

    @Test
    @Description("Проверяем сохранение фильтра с пустым ид группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9631")
    public void checkSaveNullAdgroupId() {
        expectedPerformanceFilterBannersMap = PerformanceFilterBannersMap
                .forPerformanceFilter(null, filterId, expectedFilter);
        checkError(PerformanceFiltersErrors.WRONG_FORMAT.getErrorText());
    }

    @Test
    @Description("Проверяем сохранение фильтра с неверным ид группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9632")
    public void checkSaveWrongAdgroupId() {
        expectedPerformanceFilterBannersMap = PerformanceFilterBannersMap
                .forPerformanceFilter(WRONG_ID, filterId, expectedFilter);
        checkErrors(resource(PerformanceFiltersErrors.GROUP_NOT_FOUND).args(WRONG_ID, campaignId).toString());
    }

    @Test
    @Description("Проверяем сохранение фильтра с пустым ид фильтра")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9633")
    public void checkSaveNullFilterId() {
        expectedPerformanceFilterBannersMap = PerformanceFilterBannersMap
                .forPerformanceFilter(String.valueOf(adgroupId), null, expectedFilter);
        checkError(PerformanceFiltersErrors.WRONG_FORMAT.getErrorText());
    }

    @Test
    @Description("Проверяем сохранение фильтра с неверным ид фильтра")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9634")
    public void checkSaveWrongFilterId() {
        expectedPerformanceFilterBannersMap = PerformanceFilterBannersMap
                .forPerformanceFilter(String.valueOf(adgroupId), WRONG_ID, expectedFilter);
        checkErrors(resource(PerformanceFiltersErrors.FILTER_NOT_FOUND).args(WRONG_ID, adgroupId).toString());
    }

    private void checkError(String errorText) {
        AjaxEditPerformanceFiltersResponse response = cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .postAjaxEditPerformanceFilters(campaignId, CLIENT, expectedPerformanceFilterBannersMap);
        assertThat("Ошибка соответствует ожидаемой", response.getError(),
                equalTo(errorText));
    }

    private void checkErrors(String errorText) {
        AjaxEditPerformanceFiltersResponse response = cmdRule.cmdSteps().ajaxEditPerformanceFiltersSteps()
                .postAjaxEditPerformanceFilters(campaignId, CLIENT, expectedPerformanceFilterBannersMap);
        assertThat("Ошибка соответствует ожидаемой", response.getErrors(),
                equalTo(errorText));
    }

}
