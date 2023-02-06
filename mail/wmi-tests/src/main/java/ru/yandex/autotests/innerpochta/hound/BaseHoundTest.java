package ru.yandex.autotests.innerpochta.hound;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.specification.ResponseSpecification;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.MultipleFailureException;

import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.hound.ApiHound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.UpdateHCFieldRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.UtilsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreRule.newIgnoreRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.IgnoreSshTestRule.newIgnoreSshTestRule;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;

public abstract class BaseHoundTest {
    public static AccLockRule lock = new AccLockRule();

    public static HttpClientManagerRule authClient = auth().withAnnotation().lock(lock);

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
    public UtilsRule utils = new UtilsRule(authClient);

    public static FolderList folderList = new FolderList(authClient);

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            //эксперимент
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS).times(1);

    public static ApiHound apiHound() {
        return WmiApis.apiHound(authClient.account().userTicket());
    }

    public static ru.yandex.autotests.innerpochta.wmi.core.hound.v2.ApiHound apiHoundV2() {
        return WmiApis.apiHoundV2(authClient.account().userTicket());
    }

    Integer mailboxRevision() {
        return apiHound().mailboxRevision()
                .withUid(uid())
                .get(identity()).then()
                .assertThat().statusCode(HttpStatus.OK_200).contentType("application/json")
                .extract().body().jsonPath().getInt("mailbox_revision");
    }

    public static FolderList updatedFolderList() {
        return new FolderList(authClient);
    }

    void resetFreshCounter() {
        apiHound().resetFreshCounter()
                .withUid(uid())
                .get(identity()).then()
                .assertThat().statusCode(HttpStatus.OK_200).contentType("application/json");
    }

    List<Envelope> nearestMessages(String mid, String deviation) {
        String resp = apiHound().nearestMessages()
                .withUid(uid())
                .withMid(mid)
                .withDeviation(deviation)
                .get(identity()).then()
                .assertThat().statusCode(HttpStatus.OK_200).contentType("application/json")
                .extract().body().asString();

        return getEnvelopes(resp);
    }

    protected List<Envelope> getEnvelopes(String json) {
        JsonElement envelopes = new JsonParser().parse(json).getAsJsonObject().get("envelopes");
        return new Gson().fromJson(envelopes, new TypeToken<List<Envelope>>(){}.getType());
    }

    protected DefaultHttpClient hc;

    protected final Logger logger = LogManager.getLogger(this.getClass());

    protected static String uid() {
        return authClient.account().uid();
    }

    @DataProvider
    public static Object[][] existingTabs() {
        return Tabs.existingTabs();
    }
}
