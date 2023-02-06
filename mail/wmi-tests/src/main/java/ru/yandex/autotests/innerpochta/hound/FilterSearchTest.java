package ru.yandex.autotests.innerpochta.hound;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.yplatform.FilterSearch;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.beans.folderlist.Symbol.INBOX;
import static ru.yandex.autotests.innerpochta.beans.folderlist.Symbol.SPAM;
import static ru.yandex.autotests.innerpochta.beans.yplatform.ErrorMatchers.withReason;
import static ru.yandex.autotests.innerpochta.beans.yplatform.FilterSearchMatchers.withEnvelopes;
import static ru.yandex.autotests.innerpochta.beans.yplatform.FilterSearchMatchers.withError;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops.complexMove;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.10.13
 * Time: 15:36
 * <p/>
 * DARIA-41118
 */
@Aqua.Test
@Title("[HOUND] Вывод ручки /filter_search")
@Description("Проверяем нормализацию выдачи")
@Features(MyFeatures.HOUND)
@Stories(MyStories.SEARCH)
@Credentials(loginGroup = "FilterEditingTest")
@Issues({@Issue("DARIA-41118"), @Issue("MAILPG-628")})
public class FilterSearchTest extends BaseHoundTest {

    private static final String YES = "yes";
    private static final String FILTER_SEARCH_EMPTY_HEADER = "X-Yandex-Hound-Filter-Search-Empty";
    private static final String FILTER_SEARCH_ERROR_HEADER = "X-Yandex-Hound-Filter-Search-Error";

    private static final String NAME_WITH_BACKSLASHES = "Alex \"Big\" Smith";

    @Rule
    public CleanMessagesMopsRule clean = with(authClient).inbox().outbox().allfolders();

    @Test
    @Issue("DARIA-41118")
    @Title("Должны увидеть в выдаче ненормализованный displayName, который передали через параметры")
    public void shouldSeeNormalizedNameFromArgument() {
        String mid = sendWith(authClient).viaProd().fromName(NAME_WITH_BACKSLASHES).send().strict().waitDeliver().getMid();

        FilterSearchCommand search = filterSearch(empty().setUid(uid())
                .setOrder("date1").setFids(folderList.defaultFID())
                .setMids(mid)).get().via(authClient);

        assertThat("Дисплейнейм не должен быть нормализован", search.displayNameFrom(), is(NAME_WITH_BACKSLASHES));
    }

    @Test
    @Issue("DARIA-41118")
    @Title("Должны увидеть ненормализованный displayName в выдаче")
    public void shouldSeeNormalizedName() {
        String mid = sendWith(authClient).viaProd().send().strict().waitDeliver().getMid();

        FilterSearchCommand search = filterSearch(empty().setUid(uid())
                .setOrder("date1").setFids(folderList.defaultFID())
                .setMids(mid)).get().via(authClient);

        assertThat("Дисплейнейм не должен быть нормализован", search.displayNameFrom(), is(NAME_WITH_BACKSLASHES));
    }

    @Test
    @Title("Не должны удалять «FWD:» префикс из темы письма")
    public void shouldNotRemoveFwdPrefixFromSubject() throws Exception {
        String subj = "another recent for FWD" + Util.getRandomString();
        String subjFwd = "Fwd: " + subj;

        String mid = sendWith(authClient).viaProd().subj(subjFwd).send().strict().waitDeliver()
                .getFirstMidIf(e -> e.getSubject().equals(subjFwd));

        FilterSearchCommand search = filterSearch(empty().setUid(uid())
                .setOrder("date1").setFids(folderList.defaultFID())
                .setMids(mid)).get().via(authClient);

        assertThat("Из темы не должны убирать «Fwd» префикс", search.subject(), equalTo(subjFwd));
    }

    @Test
    @Title("Не должны удалять «RE:» префикс из темы письма")
    public void shouldNotRemoveRePrefixFromSubject() throws Exception {
        String subj = "another recent for RE" + Util.getRandomString();
        String subjRe = "Re: " + subj;

        String mid = sendWith(authClient).viaProd().subj(subjRe).send().strict().waitDeliver()
                .getFirstMidIf(e -> e.getSubject().equals(subjRe));

        FilterSearchCommand search = filterSearch(empty().setUid(uid())
                .setOrder("date1").setFids(folderList.defaultFID())
                .setMids(mid)).get().via(authClient);

        assertThat("Из темы не должны убирать «Re» префикс", search.subject(), equalTo(subjRe));
    }

    @Test
    @Issue("DARIA-41118")
    @Title("Должны пуникодить кириллический домен в адресатах в filter_search")
    public void shouldCyrillicDomain() {
        final String RF_DOMAIN_PDD = "админкапдд.рф";
        String subj = Util.getRandomString();
        String subjFwd = "Re: " + subj;

        String mid = sendWith(authClient).viaProd().to("vicdev@" + RF_DOMAIN_PDD).subj(subjFwd).saveDraft()
                .strict().waitDeliver()
                .getFirstMidIf(e -> e.getSubject().equals(subjFwd));

        FilterSearchCommand search = filterSearch(empty().setUid(uid())
                .setOrder("date1").setFids(folderList.draftFID())
                .setMids(mid)).get().via(authClient).withDebugPrint();

        assertThat("Должны пуникодить домен", search.domainTo(), equalTo(RF_DOMAIN_PDD));
    }

    @Test
    @Issue("PS-1360")
    @Title("Должны исключать из результатов указанную папку")
    public void shouldFilterInboxFolderBySymbol() throws Exception {
        String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        FilterSearchObj params = empty().setUid(uid()).setMids(mid);

        assumeThat("Должны найти письмо", filterSearch(params)
                        .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectNoHeader(FILTER_SEARCH_ERROR_HEADER)
                        .get().via(authClient)
                        .parsed(),
                withEnvelopes((Matcher) hasSize(1)));

        assertThat("Не должны ничего найти", filterSearch(params.withExclFolders(INBOX, SPAM))
                        .expectNoHeader(FILTER_SEARCH_ERROR_HEADER).expectHeader(FILTER_SEARCH_EMPTY_HEADER, equalTo(YES))
                        .get().via(authClient)
                        .parsed(),
                withEnvelopes((Matcher) hasSize(0)));
    }

    @Test
    @Issue("PS-1360")
    @Title("Должны включать в выдачу результаты из указанньй папки")
    @Description("Два письма. Одно во входящих, другое в спаме. Ищем только в спаме, ожидая одно найденное")
    public void shouldIncludeSpamWhenFilterFolderBySymbol() throws Exception {
        String spamMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String nospamMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        Mops.spam(authClient, new MidsSource(spamMid))
                .post(shouldBe(okSync()));
        FilterSearchObj params = empty().setUid(uid()).setMids(spamMid, nospamMid);

        assumeThat("Должны найти оба письма", filterSearch(params)
                        .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectNoHeader(FILTER_SEARCH_ERROR_HEADER)
                        .get().via(authClient).parsed(),
                withEnvelopes((Matcher) hasSize(2)));

        assertThat("Должны найти только письмо в спаме",
                filterSearch(params.withInclFolders(SPAM))
                        .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectNoHeader(FILTER_SEARCH_ERROR_HEADER)
                        .get().via(authClient)
                        .parsed(),
                withEnvelopes((Matcher) hasSize(1)));
    }

    @Test
    @Issue("PS-1360")
    @Title("Не должны применять одновременно инклуд и экслуд")
    public void shouldReturnErrOnFilterBySymbolBothExcludeAndInclude() {
        FilterSearch parsed = filterSearch(empty().setUid(uid())
                .withExclFolders(SPAM)
                .withInclFolders(INBOX))
                .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectHeader(FILTER_SEARCH_ERROR_HEADER, equalTo(YES))
                .get().via(authClient).parsed();

        assertThat("Должны вернуть ошибку",
                parsed,
                both(withError(notNullValue(ru.yandex.autotests.innerpochta.beans.yplatform.Error.class)))
                        .and(withError(withReason(containsString("excl_folders and incl_folders")))));
    }

    @Test
    @Issue("PS-1360")
    @Title("Не должны применять одновременно only_useful и include")
    public void shouldReturnErrOnFilterBySymbolBothOnlyUsefulAndInclude() {
        FilterSearch parsed = filterSearch(empty().setUid(uid())
                .withOnlyUseful()
                .withInclFolders(INBOX))
                .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectHeader(FILTER_SEARCH_ERROR_HEADER, equalTo(YES))
                .get().via(authClient).parsed();

        assertThat("Должны вернуть ошибку",
                parsed,
                both(withError(notNullValue(ru.yandex.autotests.innerpochta.beans.yplatform.Error.class)))
                        .and(withError(withReason(containsString("only_useful and incl_folders")))));
    }

    @Test
    @Issue("PS-1360")
    @Title("Не должны применять одновременно only_useful и инклуд")
    public void shouldReturnErrOnFilterBySymbolBothOnlyUsefulAndExclude() {
        FilterSearch parsed = filterSearch(empty().setUid(uid())
                .withOnlyUseful()
                .withExclFolders(INBOX))
                .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectHeader(FILTER_SEARCH_ERROR_HEADER, equalTo(YES))
                .get().via(authClient).parsed();

        assertThat("Должны вернуть ошибку",
                parsed,
                both(withError(notNullValue(ru.yandex.autotests.innerpochta.beans.yplatform.Error.class)))
                        .and(withError(withReason(containsString("only_useful and excl_folders")))));
    }

    @Test
    @Issue("MAILPG-992")
    @Title("Должны выдавать хедер об ошибке, если не передан юид.")
    @Description("Раньше ошибка выдавалась только при непосредственно исполнении метода." +
            " Из-за этого при ошибках создания MailMetadata ошибка в хедере не отдавалась." +
            " Теперь хедер выдается на любые исключения execute, в том числе на getMetadata." +
            " Проверять удобнее на отсутствующих параметрах, необходимых для getMetadata.")
    @IgnoreForPg("MAILPG-2767")
    public void noUidShouldSeeErrorHeader() {
        filterSearch(empty()
                .setMids("123"))
                .expectNoHeader(FILTER_SEARCH_EMPTY_HEADER).expectHeader(FILTER_SEARCH_ERROR_HEADER, equalTo(YES))
                .get().via(authClient);
    }

    @Test
    @Issue("MAILPG-2007")
    @Title("Проверка поля tab в выдаче")
    public void hasTabField() throws Exception {
        String mid = sendWith(authClient).viaProd().send().strict().waitDeliver().getMid();
        FilterSearchCommand search = filterSearch(empty().setUid(uid()).setMids(mid)).get().via(authClient);
        assertThat("tab должен быть default", search.tab(), is(Tab.defaultTab));

        complexMove(authClient, folderList.inboxFID(), Tab.NEWS.getName(), new MidsSource(mid)).post(shouldBe(okSync()));
        search = filterSearch(empty().setUid(uid()).setMids(mid)).get().via(authClient);
        assertThat("tab должен быть news", search.tab(), is(Tab.NEWS.getName()));

        complexMove(authClient, folderList.inboxFID(), Tab.SOCIAL.getName(), new MidsSource(mid)).post(shouldBe(okSync()));
        search = filterSearch(empty().setUid(uid()).setMids(mid)).get().via(authClient);
        assertThat("tab должен быть social", search.tab(), is(Tab.SOCIAL.getName()));
    }

    @Test
    @Issue("MAILPG-2007")
    @Title("Проверка поля tab в выдаче с параметром full_folders_and_labels (там другая рефлексия)")
    public void hasTabFieldFull() throws Exception {
        String mid = sendWith(authClient).viaProd().send().strict().waitDeliver().getMid();
        FilterSearchCommand search = filterSearch(empty().setUid(uid()).setMids(mid).setFullFoldersAndLabels("1")).get().via(authClient);
        assertThat("tab должен быть default", search.tab(), is(Tab.defaultTab));
    }
}