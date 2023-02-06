package ru.yandex.autotests.innerpochta.spaniel;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.spaniel.Search;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;

import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.FidSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.DatesWithNext;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

@Aqua.Test
@Title("[Spaniel] Получение найденных в тайничке писем")
@Credentials(loginGroup = "SpanielMessagesBySearchTest")
@Features(MyFeatures.SPANIEL)
@Stories(MyStories.HIDDING_PLACE)
public class MessagesBySearchTest extends SpanielBaseTest {
    @ClassRule
    public static HttpClientManagerRule authClient2 = HttpClientManagerRule.auth().with("SpanielMessagesBySearchTest2");

    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).allfolders();

    @Rule
    public CleanMessagesMopsRule clean2 = new CleanMessagesMopsRule(authClient2).allfolders();

    @Before
    public void prepare() throws Exception {
        pattern = getRandomString();
        final long length = 3;

        for (int i = 0; i < length; ++i) {
            sendWith(authClient).viaProd().text(pattern).send().waitDeliver();
            waitForDifferentReceivedDate();
        }
        Mops.purge(authClient, new FidSource(folderList.sentFID())).post(shouldBe(okSync()));
        waitForIndex();
    }

    private static final int MESSAGES_PER_PAGE = 2;

    private DatesWithNext getDatesFromMessagesBySearchAndUid(String searchId, String first) {
        return new DatesWithNext(getMessagesBySearchAndUid(searchId, getUid(), MESSAGES_PER_PAGE, first));
    }

    private DatesWithNext getDatesFromMessagesBySearchAndUid(String searchId) {
        return new DatesWithNext(getMessagesBySearchAndUid(searchId, getUid(), MESSAGES_PER_PAGE, null));
    }

    @Test
    @Title("Пагинация результатов")
    @Description("Результаты должны идти от новых к старым")
    public void shouldPaginateMessages() {
        String searchId = prebuildSearchCreateRequest(getUid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        DatesWithNext firstPage = getDatesFromMessagesBySearchAndUid(searchId);

        assertThat(firstPage.getDates(), hasSize(MESSAGES_PER_PAGE));
        assertThat(firstPage.getDates(), isSortedDescending());

        DatesWithNext secondPage = getDatesFromMessagesBySearchAndUid(searchId, firstPage.getNext());

        assertThat(secondPage.getDates(), hasSize(2));

        assertThat("Результат на второй странице должен иметь более ранний received_date, чем результат на первой",
                secondPage.getDates().get(1), lessThan(firstPage.getDates().get(1)));
    }

    @Test
    @Title("Архивный поиск должен быть без результатов")
    public void shouldNotFillArchivedSearch() {
        String searchId = prebuildSearchCreateRequest(getUid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        DatesWithNext page = getDatesFromMessagesBySearchAndUid(searchId);

        assertThat(page.getDates(), hasSize(MESSAGES_PER_PAGE));

        archiveSearch(searchId);

        page = getDatesFromMessagesBySearchAndUid(searchId);

        assertThat(page.getDates(), hasSize(0));

        Search search = showSearch(searchId);
        assertThat("Статус поиска не поменялся", search.getState(), equalTo("archived"));
    }

    @Test
    @Title("Должны не терять результаты при сквозной сортировке по всем юзерам")
    public void shouldNotLoseSearchResultsAfterSortByAllUids() throws Exception {
        sendTwoMessageForAnotherUserAndRemoveStuffFromSentFolder(authClient2, pattern);

        String searchId = prebuildSearchCreateRequest(getUid(), authClient2.account().uid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        final int pageSize = 4;
        final int expectedResultSize = 10;
        List<Long> receivedDates = new ArrayList<>();

        DatesWithNext resultPage1 = getDatesWithNextFromMessagesBySearch(searchId, pageSize, null);
        assertThat(resultPage1.getDates().size(), equalTo(pageSize));
        receivedDates.addAll(resultPage1.getDates());

        DatesWithNext resultPage2 = getDatesWithNextFromMessagesBySearch(searchId, pageSize, resultPage1.getNext());
        assertThat(resultPage2.getDates().size(), equalTo(pageSize));
        receivedDates.addAll(resultPage2.getDates());

        DatesWithNext resultPage3 = getDatesWithNextFromMessagesBySearch(searchId, pageSize, resultPage2.getNext());
        assertThat(resultPage3.getDates().size(), equalTo(expectedResultSize - 2 * pageSize));
        assertThat(resultPage3.getNext(), is(nullValue()));
        receivedDates.addAll(resultPage3.getDates());

        assertThat(receivedDates, hasSize(expectedResultSize));

        List<Long> expectedReceivedDates = new ArrayList<>(receivedDates);
        expectedReceivedDates.sort(Collections.reverseOrder());

        assertThat(receivedDates, equalTo(expectedReceivedDates));
    }

    private void sendTwoMessageForAnotherUserAndRemoveStuffFromSentFolder(HttpClientManagerRule otherAuthClient,
            String body) throws Exception {
        for (int i = 0; i < 2; ++i) {
            sendWith(otherAuthClient).viaProd().text(body).send().waitDeliver();
            waitForDifferentReceivedDate();
        }

        Mops.purge(otherAuthClient, new FidSource(folderList.sentFID())).post(shouldBe(okSync()));

        waitForIndex();
    }

    private static Matcher<? super List<Long>> isSortedDescending() {
        return new TypeSafeMatcher<List<Long>>() {
            @Override
            protected boolean matchesSafely(List<Long> item) {
                List<Long> sortedItem = new ArrayList<>(item);
                sortedItem.sort(Collections.reverseOrder());

                return sortedItem.equals(item);
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("should be sorted in descending order");
            }

            @Override
            protected void describeMismatchSafely(List<Long> item, org.hamcrest.Description mismatchDescription) {
                mismatchDescription.appendValueList("[", ", ", "]", item)
                        .appendText(" is not sorted in descending order");
            }
        };
    }
}
