package ru.yandex.autotests.innerpochta.spaniel;

import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;
import ru.yandex.autotests.innerpochta.beans.yplatform.FilterSearch;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.mbody.BodyMatchers.withTransformerResult;
import static ru.yandex.autotests.innerpochta.beans.mbody.MbodyMatchers.withBodies;
import static ru.yandex.autotests.innerpochta.beans.mbody.TextTransformerResultMatchers.withContent;
import static ru.yandex.autotests.innerpochta.beans.mbody.TransformerResultMatchers.withTextTransformerResult;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;

@Aqua.Test
@Title("[Spaniel] Проверяю что нельзя взаимодействовать с письмами вне поисков")
@Credentials(loginGroup = "SpanielScenario")
@Features(MyFeatures.SPANIEL)
@Stories(MyStories.HIDDING_PLACE)
public class AccessToMessagesTest extends SpanielBaseTest {
    private static String searchId;
    private static String mid;
    private static String anotherMid;

    @BeforeClass
    public static void prepare() throws InterruptedException {
        mid = sendAndIndex(authClient, pattern);
        searchId = prebuildSearchCreateRequest(getUid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        anotherMid = sendAndIndex(authClient, pattern);
    }

    @Test
    @Title("Получение меты письма, которое было в поиске")
    public void shouldAllowSearchingWithUidAndMidInSearch() {
        FilterSearch spanielEnvelopes = apiSpaniel().filterSearch()
                .withSearchId(searchId)
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withMids(mid)
                .get(shouldBe(ok200()))
                .as(FilterSearch.class);

        assertThat(spanielEnvelopes.getEnvelopes(), iterableWithSize(1));
        assertThat(spanielEnvelopes.getEnvelopes().get(0).getMid(), equalTo(mid));
    }

    @Test
    @Title("Получение тела письма, которое было в поиске")
    public void shouldAllowReadingMessageWithUidAndMidInSearch() {
        Mbody message = apiSpaniel().message()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withMid(mid)
                .withSearchId(searchId)
                .get(shouldBe(ok200()))
                .as(Mbody.class);

        assertThat(message.getBodies().size(), equalTo(1));
        assertThat(message, withBodies(hasItem((Matcher)
                withTransformerResult(withTextTransformerResult(withContent(containsString(pattern)))))));
    }

    @Test
    @Title("Форвард письма, которое было в поиске")
    public void shouldAllowSharingWithUidAndMidInSearch() {
        apiSpaniel().sendShare()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withMid(mid)
                .withSearchId(searchId)
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()));
    }

    @Test
    @Title("Отвечаем ошибкой если uid + mid не были найдены в поиске")
    public void shouldReturnErrorPerformWithUidAndMidNotInSearch() {
        apiSpaniel().filterSearch()
                .withSearchId(searchId)
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withMids(anotherMid)
                .get(shouldBe(accessDenied400()));

        apiSpaniel().message()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withMid(anotherMid)
                .withSearchId(searchId)
                .get(shouldBe(accessDenied400()));

        apiSpaniel().sendShare()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withMid(anotherMid)
                .withSearchId(searchId)
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(accessDenied400()));
    }
}
