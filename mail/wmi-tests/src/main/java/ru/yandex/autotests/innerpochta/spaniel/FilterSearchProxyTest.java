package ru.yandex.autotests.innerpochta.spaniel;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.FilterSearch;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;

@Aqua.Test
@Title("[Spaniel] Проксирование ручки /filter_search из хаунда")
@Credentials(loginGroup = "SpanielFilterSearchTest")
@Features(MyFeatures.SPANIEL)
@Stories(MyStories.HIDDING_PLACE)
public class FilterSearchProxyTest extends SpanielBaseTest {
    String makeSearch() {
        String searchId = prebuildSearchCreateRequest(getUid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        return searchId;
    }

    @Test
    @Title("Получение envelop'а")
    public void shouldReceiveIdenticalEnvelopes() throws InterruptedException {
        String mid = sendAndIndex(authClient, pattern);
        String searchId = makeSearch();

        FilterSearch spanielEnvelopes = apiSpaniel().filterSearch()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withSearchId(searchId)
                .withMids(mid)
                .get(shouldBe(ok200()))
                .as(FilterSearch.class);
        assertThat(spanielEnvelopes.getEnvelopes(), iterableWithSize(1));

        FilterSearchCommand search = filterSearch(empty().setUid(getUid())
                .setMids(mid)).get().via(authClient);
        List<Envelope> houndEnvelopes = search.parsed().getEnvelopes();

        assertThat(houndEnvelopes, iterableWithSize(1));

        assertThat("Энвелопы из хаунда и спаниеля различаются", spanielEnvelopes.getEnvelopes().get(0),
                equalTo(houndEnvelopes.get(0)));
    }

    @Test
    @Title("Запрос от юзера без прав администратора")
    @Description("Запрос без прав администратора должен завершаться ошибкой")
    public void shouldReceive400WithoutAdminPermissions() throws InterruptedException {
        String mid = sendAndIndex(authClient, pattern);
        String searchId = makeSearch();

        apiSpaniel().filterSearch()
                .withAdminUid(notAdminUid)
                .withOrgId(orgId)
                .withUid(getUid())
                .withMids(mid)
                .withSearchId(searchId)
                .get(shouldBe(accessDenied400()));
    }

}
