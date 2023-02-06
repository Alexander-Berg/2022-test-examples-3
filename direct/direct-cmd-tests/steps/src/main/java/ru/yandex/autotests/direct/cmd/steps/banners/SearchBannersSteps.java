package ru.yandex.autotests.direct.cmd.steps.banners;

import org.apache.commons.lang3.StringUtils;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersRequest;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersResponse;
import ru.yandex.autotests.direct.cmd.data.banners.SearchWhere;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.sort.SortBy;
import ru.yandex.autotests.direct.cmd.data.sort.SortOrder;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;
import java.util.List;


public class SearchBannersSteps extends DirectBackEndSteps {

    @Step
    public List<Banner> searchBanners(String what, String... textSearch) {
        SearchBannersResponse response = postSearchBanners(what, textSearch);
        List<Banner> result = response.getBanners();
        if (result == null) {
            throw new DirectCmdStepsException("Баннеры не найдены по " + what + " " + Arrays.toString(textSearch));
        }
        return result;
    }

    @Step("Поиск баннеров (cmd_searchBanners, what = {1}, text_search = {2}")
    public SearchBannersResponse postSearchBanners(String what, String... textSearch) {
        SearchBannersRequest request = new SearchBannersRequest();
        request.setWhat(what);
        request.setTextSearch(StringUtils.join(textSearch, ","));
        request.setSort(SortBy.PRICE.getName());
        request.setReverse(SortOrder.ASCENDING.getOrder());
        request.setWhere(SearchWhere.DIRECT.getName());
        return postSearchBanners(request);
    }

    @Step("Поиск баннеров (cmd_searchBanners")
    public SearchBannersResponse postSearchBanners(SearchBannersRequest searchBannersRequest) {
        return post(CMD.SEARCH_BANNERS, searchBannersRequest, SearchBannersResponse.class);
    }
}
