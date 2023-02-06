package ru.yandex.autotests.innerpochta.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.filter.Filter;
import ru.yandex.autotests.innerpochta.steps.beans.unsubscribeFilter.UnsubscribeFilter;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.api.filters.CreateNewsletterFiltersHandler.doCreateNewsletterFiltersHadler;
import static ru.yandex.autotests.innerpochta.api.filters.DoFiltersAddHandler.doFiltersAddHandler;
import static ru.yandex.autotests.innerpochta.api.filters.DoFiltersBlackListRemoveHandler.doFiltersBlackListRemove;
import static ru.yandex.autotests.innerpochta.api.filters.DoFiltersBlacklistAdd.doFiltersBlacklistAdd;
import static ru.yandex.autotests.innerpochta.api.filters.DoFiltersDeleteHandler.doFiltersDeleteHandler;
import static ru.yandex.autotests.innerpochta.api.filters.DoFiltersWhiteListRemoveHandler.doFiltersWhiteListRemove;
import static ru.yandex.autotests.innerpochta.api.filters.DoFiltersWhitelistAdd.doFiltersWhitelistAdd;
import static ru.yandex.autotests.innerpochta.api.filters.DoUnsubscribeFuritaFiltersDelete.doUnsubscribeFuritaFiltersDelete;
import static ru.yandex.autotests.innerpochta.api.filters.FiltersBlacklistHandler.filtersBlacklistHandler;
import static ru.yandex.autotests.innerpochta.api.filters.FiltersHandler.filtersHandler;
import static ru.yandex.autotests.innerpochta.api.filters.FiltersWhitelistHandler.filtersWhitelistHandler;
import static ru.yandex.autotests.innerpochta.api.filters.UnsubscribeFuritaFiltersHandler.unsubscribeFuritaFiltersHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_ALL;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_FIELD1_FROM;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_FIELD1_SUBJECT;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_FIELD2_CONTAINS;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_LOGIC_AND;

/**
 * @author mabelpines
 */
public class ApiFiltersSteps {

    private AllureStepStorage user;
    private RestAssuredAuthRule auth;

    public ApiFiltersSteps(AllureStepStorage user) {
        this.user = user;
    }

    public ApiFiltersSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: filters. Получаем список всех фильтров пользователя.")
    public List<Filter> getAllFilters() {
        return Arrays.asList(filtersHandler().withAuth(auth).callFiltersHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.action", Filter[].class));
    }

    @Step("Вызов api-метода: filters. Получаем список всех фильтров пользователя.")
    public List<UnsubscribeFilter> getAllUnsubscribeFilters() {
        return Arrays.asList(unsubscribeFuritaFiltersHandler().withAuth(auth).callUnsubscribeFuritaFiltersHandler()
            .then().extract().jsonPath(getJsonPathConfig())
            .getObject("result.newsletterFilters", UnsubscribeFilter[].class));
    }

    @Step("Вызов api-метода: do-filters-add. Добавляем новый фильтр.")
    public Filter createFilterForFolderOrLabel(String pattern1, String pattern2, String elementType,
                                               String elementId, String clicker, boolean containsAttachment) {
        String attachmentParam = containsAttachment ? "1" : "";
        Response filter = doFiltersAddHandler().withAuth(auth).withAttachment(attachmentParam).withClicker(clicker)
            .withField1Params(FILTERS_ADD_PARAM_FIELD1_FROM, FILTERS_ADD_PARAM_FIELD1_SUBJECT)
            .withField2Params(FILTERS_ADD_PARAM_FIELD2_CONTAINS, FILTERS_ADD_PARAM_FIELD2_CONTAINS)
            .withField3Params(pattern1, pattern2).withLetter(FILTERS_ADD_PARAM_ALL)
            .withLogicParam(FILTERS_ADD_PARAM_LOGIC_AND).withMove(elementType, elementId).withDefaultName()
            .callDoFiltersAddHandler();
        return select(getAllFilters(), having(on(Filter.class).getFilid(), equalTo(filter.then().extract()
            .jsonPath().getString("models[0].data.id")))).get(0);
    }

    @Step("Вызов api-метода: do-filter-remove. Удаляем все пользовательские фильтры")
    public ApiFiltersSteps deleteAllUserFilters() {
        getAllFilters().forEach(this::deleteFilter);
        return this;
    }

    @Step("Удаляем фильтр")
    public ApiFiltersSteps deleteFilter(Filter filter) {
        if (filter.getFilid() != null)
            doFiltersDeleteHandler().withAuth(auth).withId(filter.getFilid()).callFiltersDeleteHandler();
        return this;
    }

    @Step("Вызов api-метода: do-filter-remove. Удаляем все пользовательские фильтры")
    public ApiFiltersSteps deleteAllUnsubscribeFilters() {
        getAllUnsubscribeFilters().forEach(this::deleteUnsubscribeFilters);
        return this;
    }

    @Step("Удаляем фильтр")
    public ApiFiltersSteps deleteUnsubscribeFilters(UnsubscribeFilter... filters) {
        ArrayList<String> ids = new ArrayList<>();
        for (UnsubscribeFilter filter : filters) {
            ids.add(filter.getId());
        }
        if (!ids.isEmpty())
            doUnsubscribeFuritaFiltersDelete().withAuth(auth).withId(ids, auth).callUnsubscribeFuritaFiltersDelete();
        return this;
    }

    @Step("Создаём фильтр скрытых рассылок")
    public ApiFiltersSteps createUnsubscribeFilters(String... params) {
        for (String param : params) {
            doCreateNewsletterFiltersHadler().withAuth(auth)
                .withParam(param, auth)
                .callCreateNewsletterFiltersHadler();
        }
        return this;
    }

    @Step("Вызов api-метода: filters-blacklist. Получаем все адреса из черного списка.")
    public List<String> getAllFiltersBlacklist() {
        return Arrays.asList(filtersBlacklistHandler().withAuth(auth).callFiltersBlacklistHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.addresses", String[].class));
    }

    @Step("Вызов api-метода: filters-whitelist. Получаем все адреса из белого списка.")
    public List<String> getAllFiltersWhiteList() {
        return Arrays.asList(filtersWhitelistHandler().withAuth(auth).callFiltersWhitelistHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.addresses", String[].class));
    }

    @Step("Адрес “{0}“ должен быть в Черном списке")
    public ApiFiltersSteps shouldContainAdressInBlackList(String... adresses) {
        assertThat("Адрес не содержится в черном списке.", getAllFiltersBlacklist(), hasItems(adresses));
        return this;
    }

    @Step("Удаляем адрес:{0} из черного списка.")
    public ApiFiltersSteps removeAdressFromBlackList(String email) {
        if (email != null)
            doFiltersBlackListRemove().withAuth(auth).withEmail(email).callDoFiltersBlackListRemove();
        return this;
    }

    @Step("Удаляем адрес:{0} из белого списка.")
    public ApiFiltersSteps removeAdressFromWhiteList(String email) {
        if (email != null)
            doFiltersWhiteListRemove().withAuth(auth).withEmail(email).callDoFiltersWhiteListRemove();
        return this;
    }

    @Step("Удаляем все адреса из черного списка.")
    public ApiFiltersSteps removeAllEmailsFromBlackList() {
        getAllFiltersBlacklist().forEach(this::removeAdressFromBlackList);
        return this;
    }

    @Step("Удаляем все адреса из белого списка.")
    public ApiFiltersSteps removeAllEmailsFromWhiteList() {
        getAllFiltersWhiteList().forEach(this::removeAdressFromWhiteList);
        return this;
    }

    @Step("Вызов api-метода: do-filters-blacklist-add. Добавляем адрес в черный список.")
    public ApiFiltersSteps addAddressToBlacklist(String email) {
        if (email != null)
            doFiltersBlacklistAdd().withAuth(auth).withEmail(email).callDoFiltersBlacklistAdd();
        return this;
    }

    @Step("Вызов api-метода: do-filters-whitelist-add. Добавляем адрес в белый список.")
    public ApiFiltersSteps addAddressToWhitelist(String email) {
        if (email != null)
            doFiltersWhitelistAdd().withAuth(auth).withEmail(email).callDoFiltersWhitelistAdd();
        return this;
    }

    public ApiFiltersSteps createsFilterAndGoesToRefactorPage(String address, String subject, String targetElement,
                                                              String elementId, String clicker) {
        String filterId = createFilterForFolderOrLabel(address, subject, targetElement, elementId, clicker, true)
            .getFilid();
        user.defaultSteps().opensDefaultUrlWithPostFix("#setup/filters-create/id=" + filterId);
        return this;
    }
}
