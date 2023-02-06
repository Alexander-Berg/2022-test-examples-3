package ru.yandex.autotests.innerpochta.spaniel;

import com.google.common.collect.Iterables;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.spaniel.LastId;
import ru.yandex.autotests.innerpochta.beans.spaniel.Search;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.FidSource;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.SearchListResponse;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[Spaniel] Поиск в тайничке")
@Credentials(loginGroup = "SpanielSearchTest")
@Features(MyFeatures.SPANIEL)
@Stories(MyStories.HIDDING_PLACE)
public class SearchTest extends SpanielBaseTest {
    @BeforeClass
    public static void prepare() {
        hiddenTrashFid = Mops.createHiddenTrash(authClient)
                .post(shouldBe(okFid()))
                .then().extract().body().path("fid");
    }

    @Test
    @Title("Создание и просмотр поиска")
    public void shouldCreateSearch() throws InterruptedException {
        String testDepartmentId = "2";
        String testGroupId = "2";
        String fakeExcludeId = "100500";

        sendAndIndex(authClient, pattern);

        String searchId = apiSpaniel().search().create()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withDateFrom(dateFrom)
                .withDateTo(dateTo)
                .withIncludeUids(getUid())
                .withIncludeDepartments(testDepartmentId)
                .withIncludeGroups(testGroupId)
                .withExcludeUids(fakeExcludeId)
                .withExcludeGroups(fakeExcludeId)
                .withExcludeDepartments(fakeExcludeId)
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        Search search = showSearch(searchId);

        assertThat(search.getFoundUids(), hasItem(getUid()));
        assertThat(search.getRequestedUids(), hasItem(getUid()));
        assertThat(String.valueOf(search.getDateFrom()), equalTo(dateFrom));
        assertThat(String.valueOf(search.getDateTo()), equalTo(dateTo));

        assertThat(search.getQuery().getExcludeGroups(), hasItem(fakeExcludeId));
        assertThat(search.getQuery().getExcludeDepartments(), hasItem(fakeExcludeId));
        assertThat(search.getQuery().getExcludeUids(), hasItem(fakeExcludeId));

        assertThat(search.getQuery().getIncludeGroups(), hasItem(testGroupId));
        assertThat(search.getQuery().getIncludeDepartments(), hasItem(testDepartmentId));
        assertThat(search.getQuery().getIncludeUids(), hasItem(getUid()));

        SearchListResponse searchList = apiSpaniel().search().list()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withCount("20")
                .get(shouldBe(ok200()))
                .as(SearchListResponse.class);

        List<String> searchIds = searchList.getData().stream()
                .map(Search::getSearchId)
                .collect(Collectors.toList());

        assertThat(searchIds, hasItem(searchId));
    }

    @Test
    @Title("Создание поиска только с датами")
    public void shouldCreateSearchWithDatesAndEmptyQuery() {
        String searchId = apiSpaniel().search().create()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withDateFrom(dateFrom)
                .withDateTo(dateTo)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));
    }

    @Test
    @Title("Пагинация списка поисков")
    @Description("Результаты должны идити от новых к старым")
    public void shouldPaginateSearchList() throws InterruptedException {
        sendAndIndex(authClient, pattern);

        List<String> searchIds = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            String searchId = prebuildSearchCreateRequest(getUid())
                    .withText(pattern)
                    .post(shouldBe(ok200()))
                    .as(SearchCreateResponse.class)
                    .getSearchId();
            searchIds.add(searchId);
        }

        for (String searchId : searchIds) {
            assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));
        }

        SearchListResponse searchList = apiSpaniel().search().list()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withCount("2")
                .get(shouldBe(ok200()))
                .as(SearchListResponse.class);

        List<String> firstPage = searchList.getData().stream()
                .map(Search::getSearchId)
                .collect(Collectors.toList());

        assertThat(firstPage, hasItem(Iterables.getLast(searchIds)));

        SearchListResponse searchList2 = apiSpaniel().search().list()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withCount("2")
                .withFirst(searchList.getNext())
                .get(shouldBe(ok200()))
                .as(SearchListResponse.class);

        List<String> searchIds2 = searchList2.getData().stream()
                .map(Search::getSearchId)
                .collect(Collectors.toList());

        assertThat(searchIds2, hasItem(searchIds.iterator().next()));
    }

    @Test
    @Title("Поиск по полю from")
    public void shouldSearchByFrom() throws InterruptedException {
        String from = authClient.acc().getLogin();

        String mid = sendWith(authClient).viaProd()
                .fromName(from)
                .send()
                .waitDeliver()
                .getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withFrom(from)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск по полю to")
    public void shouldSearchByTo() throws InterruptedException {
        String to = authClient.acc().getSelfEmail();
        String mid = sendWith(authClient).viaProd().to(to).send().waitDeliver().getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withTo(to)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск по полю cc")
    public void shouldSearchByCc() throws InterruptedException {
        String cc = authClientNotAdmin.acc().getSelfEmail();
        String mid = sendWith(authClient).viaProd().cc(cc).send().waitDeliver().getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withCc(cc)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск по полю bcc")
    public void shouldSearchByBcc() throws InterruptedException {
        String bcc = authClientNotAdmin.acc().getSelfEmail();
        String mid = sendWith(authClient).viaProd().bcc(bcc).send().waitDeliver().getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withBcc(bcc)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск по полям to, cc, bcc")
    @Description("Письмо должно найтись, если искомый шаблон содержит хотя бы одно из полей")
    public void shouldSearchByToCcBcc() throws InterruptedException {
        String bcc = authClientNotAdmin.acc().getSelfEmail();
        String cc = authClientNotAdmin.acc().getSelfEmail();

        String bccMid = sendWith(authClient).viaProd().bcc(bcc).send().waitDeliver().getMid();
        String ccMid = sendWith(authClient).viaProd().cc(cc).send().waitDeliver().getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withToCcBcc(bcc)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItems(bccMid, ccMid));
    }

    @Test
    @Title("Поиск по полю subject")
    public void shouldSearchBySubject() throws InterruptedException {
        String subject = "abacaba100500";

        String mid = sendWith(authClient).viaProd()
                .subj(subject)
                .send()
                .waitDeliver()
                .getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withSubject(subject)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск писем с аттачами")
    public void shouldSearchWithAttaches() throws InterruptedException, IOException {
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();
        String mid = sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withHasAttachments("yes")
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск по теме или телу одновременно")
    public void shouldSearchByBodyAndSubject() throws Exception {
        String pattern = Util.getRandomString();

        String midWithSubject = sendWith(authClient).viaProd()
                .subj(pattern)
                .send()
                .waitDeliver()
                .getMid();

        String midWithBody = sendWith(authClient).viaProd()
                .text(pattern)
                .send()
                .waitDeliver()
                .getMid();

        String midWithSubjectAndBody = sendWith(authClient).viaProd()
                .text(pattern)
                .subj(pattern)
                .send()
                .count(2)
                .waitDeliver()
                .getFirstMidIf(e -> e.getSubject().equals(pattern) && !e.getMid().equals(midWithSubject));

        String midWithoutPattern = sendWith(authClient).viaProd()
                .text(Util.getRandomString())
                .subj(Util.getRandomString())
                .send()
                .waitDeliver()
                .getMid();
        Mops.purge(authClient, new FidSource(folderList.sentFID())).post(shouldBe(okSync()));
        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withRequest(pattern)
                .withScope("hdr_subject")
                .withScope("body_text")
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat("Не нашли письмо с паттерном в теме", mids, hasItem(midWithSubject));
        assertThat("Не нашли письмо с паттерном в теле", mids, hasItem(midWithBody));
        assertThat("Не нашли письмо с паттерном в теле и теме", mids, hasItem(midWithSubjectAndBody));
        assertThat("Нашли письмо без паттерна", mids, not(hasItem(midWithoutPattern)));
    }

    @Test
    @Title("Поиск по всем возможным полям сразу")
    public void shouldSearchByAllFields() throws InterruptedException, IOException {
        String bcc = authClientNotAdmin.acc().getSelfEmail();
        String cc = bccAuthClient.acc().getSelfEmail();
        String subject = Util.getRandomString();
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();
        String mid = sendWith(authClient).viaProd()
                .addAttaches(attach)
                .cc(cc)
                .bcc(bcc)
                .subj(subject)
                .text(pattern)
                .send()
                .waitDeliver()
                .getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withHasAttachments("yes")
                .withFrom(authClient.acc().getSelfEmail())
                .withTo(authClient.acc().getSelfEmail())
                .withBcc(bcc)
                .withCc(cc)
                .withToCcBcc(cc)
                .withSubject(subject)
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск писем в корзине")
    public void shouldSearchInTrashFolder() throws Exception {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).send().waitDeliver().getMid();

        Mops.remove(authClient, new MidsSource(mid))
                .post(shouldBe(okSync()));

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withSubject(subject)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск писем в спаме")
    public void shouldSearchInSpamFolder() throws Exception {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).send().waitDeliver().getMid();

        Mops.spam(authClient, new MidsSource(mid))
                .post(shouldBe(okSync()));

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withSubject(subject)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск писем в скрытой корзине")
    public void shouldSearchInHiddenTrashFolder() throws Exception {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).send().waitDeliver().getMid();

        Mops.complexMove(authClient, hiddenTrashFid, new MidsSource(mid))
                .post(shouldBe(okSync()));

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withSubject(subject)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, hasItem(mid));
    }

    @Test
    @Title("Поиск писем в черновиках")
    public void shouldNotSearchInDraftFolder() throws Exception {
        String subject = Util.getRandomString();
        String mid = sendWith(authClient).viaProd().subj(subject).saveDraft().waitDeliver().getMid();

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withSubject(subject)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        List<String> mids = getMidsFromMessagesBySearchAndUid(searchId, getUid());
        assertThat(mids, not(hasItem(mid)));
    }

    @Test
    @Title("Архивирование поиска")
    public void shouldArchiveTest() {
        String searchId = prebuildSearchCreateRequest(getUid())
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        archiveSearch(searchId);

        Search search = showSearch(searchId);

        assertThat("Поиск не заархивировался", search.getState(), equalTo("archived"));
    }

    @Test
    @Title("Переименование поиска")
    public void shouldChangeSearchName() throws Exception {
        String subject = Util.getRandomString();
        String name = subject;

        waitForIndex();

        String searchId = prebuildSearchCreateRequest(getUid())
                .withSubject(subject)
                .withName(name)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        Search search = showSearch(searchId);

        assertThat("Неправильное имя поиска", search.getName(), equalTo(name));

        String newName = Util.getRandomString();
        apiSpaniel().search().rename()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withSearchId(searchId)
                .withName(newName)
                .post(shouldBe(ok200()));

        search = showSearch(searchId);

        assertThat("Имя поиска не поменялось", search.getName(), equalTo(newName));
    }

    @Test
    @Title("Возвращаем последний id поиска")
    public void shouldReturnLastSearchId() throws Exception {
        Search search = apiSpaniel().search().list()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withCount("1")
                .get(shouldBe(ok200()))
                .as(SearchListResponse.class)
                .getData()
                .get(0);

        Long searchId = apiSpaniel().search().lastId()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .get(shouldBe(ok200()))
                .as(LastId.class)
                .getSearchId();

        assertThat("Не совпадают идентификаторы со страницы поисков и из ручки последнего поиска",
                search.getSearchId(), equalTo(searchId.toString()));
    }

    @ClassRule
    public static HttpClientManagerRule bccAuthClient = HttpClientManagerRule.auth().with("SpanielBccTest");

    @ClassRule
    public static CleanMessagesMopsRule cleanBcc = new CleanMessagesMopsRule(bccAuthClient).allfolders();

    private static String hiddenTrashFid;
}
