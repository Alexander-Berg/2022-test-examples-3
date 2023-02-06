package ru.yandex.autotests.innerpochta.hound;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.hound.data.HoundB2BDataGenerator;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraint;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.function.Function.identity;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MbodyApi.apiMbody;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BeanConstraints.ignore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.10.13
 * Time: 15:36
 * <p/>
 * Для ручек folder и label запросы без кук
 */
@Aqua.Test
@Title("[HOUND2][B2B] Тестирование сервиса hound на 9091 порту")
@Description("Тесты на различные ручки hound-а")
@Features(MyFeatures.HOUND)
@Stories(MyStories.B2B)
@RunWith(Parameterized.class)
@Credentials(loginGroup = "Hound2B2B")
@IgnoreForPg("MAILPG-2767")
public class Hound2B2BTest extends BaseHoundTest {
    private static final BeanConstraint IGNORE_DEFAULT = ignore("age", "birthdayDate", "httpOnly", "timestamp",
            "timer_logic", "timer_db", "tscn", "ckey", "jane", "account_information/age", "account_information/ticket",
            "threads_by_folder/threadLabels[0]/tscn", "threads_by_folder/threadLabels[1]/tscn",
            //for api_,mobile
            "account_information/account-information/ckey", "account_information/account-information/timer_logic",
            "get_user_parameters/body/timer_logic", "yamail_status/yamail_status/timer_logic",
            "settings_setup/body/timer_logic", "files[0]/url", "files[0]/preview");

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        return HoundB2BDataGenerator.apiHandles();
    }

    @BeforeClass
    public static void fillParams() throws Exception {
        CleanMessagesMopsRule.with(authClient).allfolders().call();
        new DeleteLabelsMopsRule(authClient).call();
        DeleteFoldersRule.with(authClient).all().call();

        params = newHashMap();
        params.put("non_exist_uid", "10001");
        params.put("non_exist_param", "21463124314");
        params.put("inbox_fid", folderList.defaultFID());

        String userFid = Mops.newFolder(authClient, Util.getRandomString());
        String userLid = Mops.newLabelByName(authClient, Util.getRandomString());
        params.put("lid", userLid);

        List<ru.yandex.autotests.innerpochta.beans.Envelope> envelopesTid0 = sendWith(authClient).count(2).send()
                .waitDeliver().getEnvelopes();
        List<ru.yandex.autotests.innerpochta.beans.Envelope> envelopesTid1 = sendWith(authClient).count(2).send()
                .waitDeliver().getEnvelopes();
        params.put("tid", envelopesTid0.get(0).getThreadId());
        params.put("tid2", envelopesTid1.get(0).getThreadId());

        String mid0 = envelopesTid0.get(0).getMid();
        params.put("mid0", mid0);
        params.put("mid1", envelopesTid1.get(0).getMid());

        Mops.label(authClient, new MidsSource(envelopesTid0.stream().map(e -> e.getMid()).collect(Collectors.toList())),
                asList(userLid)).post(shouldBe(okSync()));

        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();
        sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();

        String pinnedLabel = Hound.getLidBySymbolTitle(authClient, PINNED);
        Mops.label(authClient, new MidsSource(envelopesTid0.stream().map(e -> e.getMid()).collect(Collectors.toList())),
                asList(pinnedLabel)).post(shouldBe(okSync()));

        String msgid = apiMbody(authClient.account().userTicket()).message()
                .withMid(mid0)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class).getInfo()
                .getMessageId();

        sendWith(authClient).text(Util.getLongString()).inReplyTo(msgid).saveDraft().waitDeliver();
        sendWith(authClient).text(Util.getLongString()).inReplyTo(msgid).send().waitDeliver();

        msgid = URLEncoder.encode(msgid, StandardCharsets.UTF_8.name());
        params.put("msgid", msgid);
    }

    public Hound2B2BTest(HoundB2BDataGenerator.TestMethod oper, String opername) {
        this.oper = oper;
    }

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    @Test
    @Title("Сравние yplatform ручек с продакшеном (GET)")
    @Description("Сравниваем с продакшеном выдачу GET методов (с заголовками) Yplatform на 9090 порту.")
    public void compareMethodsResponseWithHeaders() {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());

        Oper respNew = oper.call(uid(), params)
                .setHost(props().houndUri())
                .get().via(authClient);

        Oper respBase = oper.call(uid(), params)
                .setHost(b2bUri)
                .get().via(authClient);

        Map expectedMap = JsonUtils.getObject(respBase.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respNew.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(IGNORE_DEFAULT));
    }

    @Test
    @Title("Сравние yplatform ручек с продакшеном (POST и без заголовков)")
    @Issue("DARIA-35785")
    @Description("Сравниваем с продакшеном выдачу POST методов Yplatform на 9090 порту.\n" +
            "Делаем тоже самое, но POST запросы\n" +
            "DARIA-35785]")
    public void compareMethodsResponsePostRequest() {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());

        Map<String, String> params = newHashMap();
        params.put("post", "true");
        params.putAll(this.params);

        Oper respNew = oper.call(uid(), params)
                .setHost(props().houndUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post().via(authClient);

        Oper respBase = oper.call(uid(), params)
                .setHost(b2bUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post().via(authClient);


        Map expectedMap = JsonUtils.getObject(respBase.toString(), Map.class);
        Map actualMap = JsonUtils.getObject(respNew.toString(), Map.class);

        assertThat(actualMap, beanDiffer(expectedMap).fields(IGNORE_DEFAULT));
    }

    private static Map<String, String> params;
    private static String b2bUri = props().houndB2bUri();
    private HoundB2BDataGenerator.TestMethod oper;
}