package ru.yandex.autotests.innerpochta.mbody;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.val;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.Validate;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.DateResult;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.matchers.RegexMatcher;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.beans.mbody.BodyMatchers.withTransformerResult;
import static ru.yandex.autotests.innerpochta.beans.mbody.DateResultMatchers.withTimestamp;
import static ru.yandex.autotests.innerpochta.beans.mbody.DateResultMatchers.withUserTimestamp;
import static ru.yandex.autotests.innerpochta.beans.mbody.MbodyMatchers.withBodies;
import static ru.yandex.autotests.innerpochta.beans.mbody.TextTransformerResultMatchers.withAfterTykva;
import static ru.yandex.autotests.innerpochta.beans.mbody.TextTransformerResultMatchers.withContent;
import static ru.yandex.autotests.innerpochta.beans.mbody.TextTransformerResultMatchers.withDivLimitExceeded;
import static ru.yandex.autotests.innerpochta.beans.mbody.TransformerResultMatchers.withTextTransformerResult;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.NO_VDIRECT_LINKS_WRAP;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.error500WithString;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.missingParam400;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.badRequest400;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getSmileHtml;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 16.09.14
 * Time: 17:07
 * <p/>
 * https://wiki.yandex-team.ru/users/dskut/message-body
 * SC_INTERNAL_SERVER_ERROR 500
 * SC_BAD_REQUEST 400
 */
@Aqua.Test
@Title("[MBODY] Сервис mbody на 8888 порту")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "MbodyTest")
@Issue("AUTOTESTPERS-142")
public class MbodyTest extends MbodyBaseTest {
    public static final String NOT_EXISTENT_MID =  "0";

    public static final Integer divLimit = 170;
    public static final String MESSAGE_NOT_FOUND_ERROR_PATTERN = "exception: error in forming message: getMessageAccessParams error: unknown mid=%s";
    public static final String NO_MID_ERROR = "mid must not be empty";

    private static String stid;
    private static String mid;

    public static final String TZ_MOSCOW = "Europe/Moscow";
    public static final String TZ_BERLIN = "Europe/Berlin";

    @BeforeClass
    public static void prepare() throws Exception {
        CleanMessagesMopsRule.with(authClient).allfolders();
        Envelope envelope = sendWith(authClient)
                .viaProd()
                .text(getSmileHtml(Util.getRandomShortInt()) + "\nhttp://ya.ru")
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getEnvelope().orElse(null);

        assertTrue("Не нашли отправленное письмо", envelope != null);

        mid = envelope.getMid();
        stid = envelope.getStid();
    }

    @Before
    public void setUp() throws Exception {
        Validate.notBlank(stid, "STID не был получен из mdbdir");
    }

    @Test
    @Issue("MAILDEV-462")
    @Title("Должны вернуть 400 на несуществующий мид")
    @Description("Проверяем реакцию mbody на письмо с мид 0\n" +
            "Ожидаемый результат: 400 и " + MESSAGE_NOT_FOUND_ERROR_PATTERN)
    public void shouldReturn400OnWrongMid() throws IOException {
        String midError = String.format(MESSAGE_NOT_FOUND_ERROR_PATTERN, NOT_EXISTENT_MID);
        apiMbody().message()
            .withMid(NOT_EXISTENT_MID)
            .withUid(uid())
            .get(shouldBe(badRequest400(midError)));
    }

    @Test
    @Issue("MAILDEV-462")
    @Title("Должны вернуть 400 на отсутствующий мид")
    @Description("Проверяем реакцию mbody на запрос со стидом без мида")
    public void shouldReturn400OnNoMid() throws IOException {
        apiMbody().message()
            .withStid(stid)
            .withUid(uid())
            .get(shouldBe(missingParam400(NO_MID_ERROR)));
    }

    @Test
    @Issue("DARIA-47387")
    @Title("Не должны оборачивать ссылки в вдирект при соответствующем флаге")
    public void shouldNotWrapLinksWithVdirectWithFlag() throws Exception {
        final Mbody res = apiMbody().message()
            .withFlags(NO_VDIRECT_LINKS_WRAP.toString())
            .withMid(mid)
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class);
        assertThat(res, withBodies(hasItem((Matcher)
            withTransformerResult(withTextTransformerResult(withContent(not(containsString("re.jsx"))))))));
    }

    @Test
    @Title("Должны отдавать московскую таймзону по дефолту")
    @Description("Тест mbody с правильным stid и различными tz\n" +
            "tz - паспортная тайм-зона юзера. Если нет параметра, берется дефолтная \"Europe/Moscow\"")
    @Stories("Проверка таймзоны")
    public void shouldReturnMoscowTzByDefault() throws Exception {
        DateResult date = apiMbody().message()
            .withMid(mid)
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class).getInfo().getDateResult();

        assertThat("<timestamp> должен совпадать с <user_timestamp>", date,
                withTimestamp(equalTo(date.getUserTimestamp())));

        DateResult dateMoscow = apiMbody().message()
            .withMid(mid)
            .withUid(uid())
            .withTz(TZ_MOSCOW)
            .get(identity()).peek().as(Mbody.class).getInfo().getDateResult();

        assertThat("<timestamp> и <user_timestamp> должны совпадать со значениями по умолчанию",
                dateMoscow, allOf(withTimestamp(equalTo(date.getTimestamp())),
                        withUserTimestamp(equalTo(date.getUserTimestamp()))));
    }

    @Test
    @Title("Должны менять таймзону по параметру TZ")
    @Description("Тест mbody с правильным stid и различными tz\n" +
            "tz - паспортная тайм-зона юзера. Если нет параметра, берется дефолтная \"Europe/Moscow\"")
    @Stories("Проверка таймзоны")
    public void shouldChangeTZWithChangeOfTzParam() throws Exception {
        DateResult date = apiMbody().message()
            .withMid(mid)
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class).getInfo().getDateResult();

        DateResult dateBerlin = apiMbody().message()
            .withMid(mid)
            .withUid(uid())
            .withTz(TZ_BERLIN)
            .get(identity()).peek().as(Mbody.class).getInfo().getDateResult();

        assertThat("<timestamp> должны совпадать со значениями по умолчанию, <user_timestamp> должен быть Берлинским",
                dateBerlin, allOf(
                        withTimestamp(equalTo(date.getTimestamp())),
                        withUserTimestamp(equalTo(date.getUserTimestamp() + tzOffset(TZ_BERLIN) - tzOffset(TZ_MOSCOW)))
                ));
    }

    private static Long tzOffset(String tz) {
        TimeZone timeZone = TimeZone.getTimeZone(tz);
        return Long.valueOf(timeZone.getOffset(new Date().getTime()));
    }

    public String formDivMessage(int depth) {
        String msg = "<html>";
        for (int i = 0; i < depth; i++) {
            msg += "<div><p>";
        }
        for (int i = 0; i < depth; i++) {
            msg += "</p></div>";
        }
        msg += "</html>";
        return msg;
    }

    @Test
    @Issue("MAILDEV-721")
    @Title("Проверяем divLimit для письма с нормальным уровнем вложенности div")
    public void divLimitOk() throws Exception {
        mid = sendWith(authClient)
                .viaProd()
                .text(formDivMessage(divLimit))
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getMid();

        final Mbody res = apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class);
        assertThat(res, withBodies(hasItem((Matcher)
                withTransformerResult(withTextTransformerResult(withDivLimitExceeded(equalTo(false)))))));
    }

    @Test
    @Issue("MAILDEV-721")
    @Title("Проверяем divLimit для письма с уровнем вложненности превышающим лимит")
    public void divLimitNotOk() throws Exception {
        mid = sendWith(authClient)
                .viaProd()
                .text(formDivMessage(divLimit + 1))
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getMid();

        final Mbody res = apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class);
        assertThat(res, withBodies(hasItem((Matcher)
                withTransformerResult(withTextTransformerResult(withDivLimitExceeded(equalTo(true)))))));
    }

    @Test
    @Issue("MAILDEV-726")
    @Title("добавление лимита размера адреса в mbody")
    public void shouldNotReturnLongAddress() throws Exception {
        final String LONG_ADDRESS = Util.getLongString().toLowerCase() + "@" + authClient.acc().getDomain();
        mid = sendWith(authClient)
                .viaProd()
                .to(LONG_ADDRESS + ", " + authClient.acc().getSelfEmail())
                .saveDraft()
                .waitDeliver()
                .getMid();

        Mbody res = apiMbody().message()
               .withMid(mid)
               .withUid(uid())
               .get(identity()).peek().as(Mbody.class);

        assertThat("Тело письма не дожно содержать этот адрес",
                res.getInfo().getAddressesResult().get(0).getEmail(), RegexMatcher.isContain(LONG_ADDRESS));

        mid = sendWith(authClient)
                .viaProd()
                .to(Util.getLongString().substring(1,102) + LONG_ADDRESS + ", " + authClient.acc().getSelfEmail())
                .saveDraft()
                .waitDeliver()
                .getMid();

        res = apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class);

        assertThat("Тело письма не дожно содержать этот адрес",
                res.getInfo().getAddressesResult().get(0).getEmail(), not(RegexMatcher.isContain(LONG_ADDRESS)));
    }

    @Test
    @Issues({@Issue("MAILPG-2349"), @Issue("MAILPG-2508")})
    @Title("Проверяем возвращаемые значения в случае стратегии 'тыква'")
    public void shouldSetSpecialResponseValuesInCaseOfTykvaResponse() throws Exception {
        final String before = "aaa<script>steal cookies!</script><br/>bbb";
        final String after = "aaa<br>bbb\r\n";


        mid = sendWith(authClient)
                .viaProd()
                .text(before)
                .html(ApiSendMessage.HtmlParam.YES.value())
                .send()
                .waitDeliver()
                .getMid();


        final Mbody res = apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .withSanitizerStrategy("tykva")
                .get(shouldBe(ok200())).peek().as(Mbody.class);


        assertThat(res, withBodies(hasItem((Matcher)
                withTransformerResult(withTextTransformerResult(withContent(equalTo(after)))))));

        assertThat(res, withBodies(hasItem((Matcher)
                withTransformerResult(withTextTransformerResult(withAfterTykva(is(true)))))));
    }

    @Test
    @Issue("MAILPG-2425")
    @Title("Размер аттачей в выдачах хаунда и mbody должен совпадать")
    public void sizesOfAttachesInMbodyResponseShouldBeEqualSizesOfAttachesInHoundResponse() throws IOException {
        File attach = AttachUtils.genFile(1025);
        attach.deleteOnExit();
        File attach2 = AttachUtils.genFile(1);
        attach2.deleteOnExit();
        final String mid = sendWith(authClient).addAttaches(attach, attach2).send().waitDeliver().getMid();

        FolderList folderList = new FolderList(authClient);

        final FilterSearchCommand houndResponse = filterSearch(empty().setUid(uid())
                .setFids(folderList.defaultFID())
                .setMids(mid)).get().via(authClient).statusCodeShouldBe(HttpStatus.SC_OK);

        final Mbody mbodyMessageResponse = apiMbody().message()
                .withMid(mid)
                .withUid(uid())
                .get(shouldBe(ok200())).peek().as(Mbody.class);

        val envelopes = houndResponse.parsed().getEnvelopes();
        assertThat("Неожиданное кол-во писем в выдаче хаунда", envelopes, hasSize(1));
        Map<String, Double> houndHidSize = envelopes.get(0).getAttachments().stream()
                .map(m -> (Map<String, Object>)m)
                .collect(Collectors.toMap(
                        m -> (String)m.get("m_hid"),
                        m -> (Double)(m.get("m_size"))
                ));

        Map<String, Double> mbodyMessageHidSize = mbodyMessageResponse.getAttachments().stream()
                .collect(Collectors.toMap(
                        a -> a.getBinaryTransformerResult().getHid(),
                        a -> Double.valueOf(a.getBinaryTransformerResult().getTypeInfo().getLength())
                ));

        assertThat("Размеры Аттачей в хаунде и mbody/message не совпадают", mbodyMessageHidSize, equalTo(houndHidSize));
    }
}