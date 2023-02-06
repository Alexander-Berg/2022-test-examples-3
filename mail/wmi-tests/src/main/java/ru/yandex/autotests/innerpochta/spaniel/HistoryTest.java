package ru.yandex.autotests.innerpochta.spaniel;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.spaniel.HistoryAction;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.HistoryResponse;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;

@Aqua.Test
@Title("[Spaniel] Тестирование истории действий")
@Credentials(loginGroup = "SpanielHistoryTest")
@Features(MyFeatures.SPANIEL)
@Stories(MyStories.HIDDING_PLACE)
public class HistoryTest extends SpanielBaseTest {
    private List<HistoryAction> getLatestHistoryAction() {
        List<HistoryAction> actions = apiSpaniel()
                .history()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withCount("2")
                .get(shouldBe(ok200()))
                .as(HistoryResponse.class)
                .getData();

        assertThat("Пустая история", actions.size(), not(equalTo(0)));
        return actions;
    }

    @Test
    @Title("Логгирование создания поиска и пересылки сообщений")
    public void shouldLogCreateSearchAndSendShare() throws InterruptedException {
        String mid = sendAndIndex(authClient, pattern);
        String to = authClient.acc().getSelfEmail();

        String searchId = apiSpaniel().search().create()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withDateFrom(dateFrom)
                .withDateTo(dateTo)
                .withIncludeUids(getUid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();
        String searchIdFromHistory = getLatestHistoryAction().get(0).getInfo().getCreateSearch().getSearchId();
        assertThat("Неверное значение searchId в истории", searchId, equalTo(searchIdFromHistory));

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        sendShare(to, mid, searchId, "/send_share");

        String toFromHistory = getLatestHistoryAction().get(0).getInfo().getSendShare().getTo();
        assertThat("Неверное значение to в истории", to, equalTo(toFromHistory));
    }
}
