package ru.yandex.autotests.innerpochta.spaniel;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.ResponseSpecification;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.beans.spaniel.Search;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.UpdateHCFieldRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.ApiSpaniel;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.DatesWithNext;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.MessagesBySearchResponse;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.MessagesForUser;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.v1.messagesbysearch.ApiMessagesBySearch;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.v1.messagesbysearchanduid.ApiMessagesBySearchAndUid;
import ru.yandex.autotests.innerpochta.wmi.core.spaniel.v1.search.create.ApiSearchCreate;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitForMessage;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

public class SpanielBaseTest {
    public static AccLockRule lock = new AccLockRule();

    public static HttpClientManagerRule authClient = HttpClientManagerRule.auth().withAnnotation().lock(lock);

    @ClassRule
    public static RuleChain chainAuth = RuleChain.outerRule(lock).around(authClient);

    @ClassRule
    public static IgnoreRule beforeTestClass = newIgnoreRule();

    @Rule
    public IgnoreRule beforeTest = newIgnoreRule();

    @ClassRule
    public static IgnoreSshTestRule beforeSshTestClass = newIgnoreSshTestRule();

    @Rule
    public IgnoreSshTestRule beforeSshTest = newIgnoreSshTestRule();

    @Rule
    public TestRule chainRule = RuleChain
            .outerRule(new LogConfigRule())
            .around(new UpdateHCFieldRule(authClient, this, "hc"));

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    public static FolderList folderList = new FolderList(authClient);

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            //эксперимент
            .ifException(AssertionError.class)
            .every(3, TimeUnit.SECONDS).times(2);

    @ClassRule
    public static CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).allfolders();

    protected DefaultHttpClient hc;

    public WaitForMessage waitWith = new WaitForMessage(authClient);

    static private String userTicket = null;

    @BeforeClass
    public static void setUp() {
        apiSpaniel().organization().enable()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .post(identity());
    }

    static String getUserTicket() {
        if (userTicket == null) {
            userTicket = authClient.account().userTicket();
        }
        return userTicket;
    }

    static ApiSpaniel apiSpaniel() {
        return ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiSpaniel(getUserTicket());
    }

    static String getUid() {
        return authClient.account().uid();
    }

    static void waitForIndex() throws InterruptedException {
        long moreTimeForMsearchIndexing = 1000;
        Thread.sleep(moreTimeForMsearchIndexing);
    }

    static void waitForDifferentReceivedDate() throws InterruptedException {
        long moreTimeForMsearchIndexing = 1000;
        Thread.sleep(moreTimeForMsearchIndexing);
    }

    static String sendAndIndex(HttpClientManagerRule client, String body) throws InterruptedException {
        String mid = sendWith(client).viaProd().text(body).send().waitDeliver().getMid();

        waitForIndex();

        return mid;
    }

    static String sendAndIndexWithAttach(HttpClientManagerRule client, String body, File attach) throws InterruptedException {
        String mid = sendWith(client)
                .viaProd()
                .text(body)
                .addAttaches(attach)
                .send()
                .waitDeliver()
                .getMid();

        waitForIndex();

        return mid;
    }

    void sendShare(String to, String mid, String searchId, String subj) {
        apiSpaniel().sendShare()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withForwardMids(mid)
                .withMid(mid)
                .withSearchId(searchId)
                .withTo(to)
                .withText("there is forwarded message sent from spaniel")
                .withSubj(subj)
                .post(shouldBe(ok200()));
    }

    void archiveSearch(String searchId) {
        apiSpaniel().search().archive()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withSearchId(searchId)
                .post(shouldBe(ok200()));
    }

    MessagesBySearchResponse getMessagesBySearchAndUid(String searchId, String uid, long count, String first) {
        ApiMessagesBySearchAndUid builder = apiSpaniel().messagesBySearchAndUid()
                .withAdminUid(uid)
                .withOrgId(orgId)
                .withSearchId(searchId)
                .withCount(String.valueOf(count))
                .withUid(uid);

        if (first != null) {
            builder.withFirst(first);
        }

        MessagesBySearchResponse response = builder
                .get(shouldBe(ok200()))
                .as(MessagesBySearchResponse.class);

        return response;
    }

    DatesWithNext getDatesWithNextFromMessagesBySearch(String searchId, long count, String first) {
        ApiMessagesBySearch builder = apiSpaniel().messagesBySearch()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withSearchId(searchId)
                .withCount(String.valueOf(count));

        if (first != null) {
            builder.withFirst(first);
        }

        Response netResponse = builder.get(shouldBe(ok200()));

        SearchCreateResponse running = netResponse.as(SearchCreateResponse.class);
        if (running.getSearchId() != null) {
            assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), running.getSearchId())));
            return getDatesWithNextFromMessagesBySearch(running.getSearchId(), count, first);
        }

        MessagesBySearchResponse response = netResponse.as(MessagesBySearchResponse.class);

        return new DatesWithNext(response);
    }

    List<String> getMidsFromMessagesBySearchAndUid(String searchId, String uid) {
        Map<String, MessagesForUser> data = getMessagesBySearchAndUid(searchId, uid, 30, null).getData();

        if (data.containsKey(uid)) {
            return data.get(uid)
                    .getEnvelopes().stream()
                    .map(Envelope::getMid)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    static ApiSearchCreate prebuildSearchCreateRequest(String... uids) {
        return apiSpaniel().search().create()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withDateFrom(dateFrom)
                .withDateTo(dateTo)
                .withIncludeUids(uids)
        ;
    }

    Search showSearch(String searchId) {
        return apiSpaniel().search().show()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withSearchId(searchId)
                .get(shouldBe(ok200()))
                .as(Search.class);
    }

    static final String orgId = "107908";
    static final String notAdminLoginGroup = "SpanielNotAdminUser";
    static String pattern = getRandomString();

    @ClassRule
    public static HttpClientManagerRule authClientNotAdmin = HttpClientManagerRule.auth().with(notAdminLoginGroup);

    static String notAdminUid;
    static String dateFrom;
    static String dateTo;

    @BeforeClass
    public static void prepareData() {
        dateFrom = String.valueOf(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());
        dateTo = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        notAdminUid = authClientNotAdmin.account().uid();
    }

    public static ResponseSpecification ok200() {
        return new ResponseSpecBuilder()
                .expectStatusCode(org.apache.http.HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification accessDenied400() {
        return new ResponseSpecBuilder()
                .expectStatusCode(org.apache.http.HttpStatus.SC_BAD_REQUEST)
                .expectBody("category", equalTo("access"))
                .expectBody("message", equalTo("access denied"))
                .build();
    }
}
