package ru.yandex.autotests.direct.cmd.steps.performancefilters;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.*;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by aleran on 13.11.2015.
 */
public class AjaxEditPerformanceFiltersSteps extends DirectBackEndSteps {

    @Step("Редактирование перфоманс фильтра логин {1} кампании {0}")
    public AjaxEditPerformanceFiltersResponse postAjaxEditPerformanceFilters(String campaignId,
                                                                             String login,
                                                                             PerformanceFilterBannersMap filterBannersMap) {
        AjaxEditPerformanceFiltersRequest request = new AjaxEditPerformanceFiltersRequest();
        request.setCid(campaignId);
        request.setUlogin(login);
        request.setJsonAdgroupPerformanceFilters(filterBannersMap.toJson());
        return post(CMD.AJAX_EDIT_PERFORMANCE_FILTERS, request, AjaxEditPerformanceFiltersResponse.class);
    }

    @Step("изменение условий дмо через ajaxEditPerformanceFilters")
    public void performanceFiltersChangeWithAssumption(Long cid, Long groupId, PerformanceFilter filter, String uLogin) {
        AjaxEditPerformanceFiltersResponse response = postAjaxEditPerformanceFilters(cid.toString(), uLogin,
                new PerformanceFilterBannersMap()
                        .withPerformanceFilterBannerMap(groupId.toString(), new PerformanceFilterMap()
                                .withEdited(filter.getPerfFilterId(), filter)));
        assumeThat("действия над фильтром прошли успешно", response.getErrors(), nullValue());
        assumeThat("действия над фильтром прошли успешно", response.getError(), nullValue());
    }

    @Step("удаление условий дмо через ajaxEditPerformanceFilters")
    public void performanceFiltersDeleteWithAssumption(Long cid, Long groupId, String uLogin, String... perfFilterIds) {
        AjaxEditPerformanceFiltersResponse response = postAjaxEditPerformanceFilters(cid.toString(), uLogin,
                new PerformanceFilterBannersMap()
                        .withPerformanceFilterBannerMap(groupId.toString(), new PerformanceFilterMap()
                                .withDeleted(Arrays.asList(perfFilterIds))));
        assumeThat("действия над фильтром прошли успешно", response.getErrors(), nullValue());
        assumeThat("действия над фильтром прошли успешно", response.getError(), nullValue());
    }
}
