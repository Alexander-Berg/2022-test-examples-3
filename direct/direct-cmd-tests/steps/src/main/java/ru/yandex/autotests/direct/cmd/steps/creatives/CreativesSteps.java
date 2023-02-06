package ru.yandex.autotests.direct.cmd.steps.creatives;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.creatives.*;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class CreativesSteps extends DirectBackEndSteps {

    @Step("Поиск креативов по вхождению подстроки \"{0}\" в название")
    public List<Creative> searchCreatives(String text) {
        SearchCreativesResponse response = postSearchCreatives(new SearchCreativesRequest().
                withPage(1).
                withCount(10).
                withSearch(text));
        return response.getResult().getCreatives();
    }

    @Step("Поиск креативов по фильтру \"{0}\" со значением \"{1}\"")
    public List<Creative> searchCreatives(SearchCreativesFilterEnum filter, String value) {
        SearchCreativesResponse response = postSearchCreatives(new SearchCreativesRequest().
                withPage(1).
                withCount(10).
                withJsonFilter(singletonList(new SearchFilter().withFilter(filter).withValue(value)))
        );
        return response.getResult().getCreatives();
    }

    @Step("POST cmd = searchCreatives (поиск креативов по названию)")
    public SearchCreativesResponse postSearchCreatives(SearchCreativesRequest request) {
        return post(CMD.SEARCH_CREATIVES, request, SearchCreativesResponse.class);
    }

    @Step("Поиск canvas креативов по id")
    public List<Creative> searchCanvasCreatives(List<Long> creativeIds) {
        return rawSearchCanvasCreatives(creativeIds).getResult().getCreatives();
    }

    @Step("Поиск canvas креативов по id")
    public List<Creative> searchCanvasCreatives(Long... creativeIds) {
        return rawSearchCanvasCreatives(creativeIds).getResult().getCreatives();
    }

    @Step("Поиск canvas креативов по id")
    public SearchCreativesResponse rawSearchCanvasCreatives(Long... creativeIds) {
        return rawSearchCanvasCreatives(asList(creativeIds));
    }

    @Step("Поиск canvas креативов по id")
    public SearchCreativesResponse rawSearchCanvasCreatives(List<Long> creativeIds) {
        return getSearchCanvasCreatives(new SearchCreativesRequest().
                withPage(1).
                withCount(10).
                withCreative(creativeIds));
    }

    @Step("Поиск canvas креативов по id с uLogin: {0}")
    public SearchCreativesResponse rawSearchCanvasCreatives(String login, Long... creativeIds) {
        return getSearchCanvasCreatives(new SearchCreativesRequest().
                withPage(1).
                withCount(10).
                withCreative(asList(creativeIds)).
                withUlogin(login));
    }

    @Step("GET cmd = searchCanvasCreatives (поиск canvas creatives)")
    public SearchCreativesResponse getSearchCanvasCreatives(SearchCreativesRequest request) {
        return get(CMD.SEARCH_CANVAS_CREATIVES, request, SearchCreativesResponse.class);
    }
}
