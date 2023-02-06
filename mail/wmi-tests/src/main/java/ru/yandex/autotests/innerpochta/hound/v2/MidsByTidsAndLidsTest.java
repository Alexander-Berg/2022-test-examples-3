package ru.yandex.autotests.innerpochta.hound.v2;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.response.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.V2MidsByTidsAndLidsResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.midsbytidsandlids.ApiMidsByTidsAndLids;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitedMailbox;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgumentWithCode;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] v2/mids_by_tids_and_lids")
@Description("Тесты на v2/mids_by_tids_and_lids")
@Features(MyFeatures.HOUND)
@Stories(MyStories.MID_LIST)
@Credentials(loginGroup = "HoundV2MidsByTidsAndLidsTest")
public class MidsByTidsAndLidsTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).before(true).allfolders();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient).before(true);

    @Test
    @Title("Проверка вызова с отсутствующим UID")
    @IgnoreForPg("MAILPG-2767")
    public void mustReturn400ForAbsentUid() {
        apiHoundV2().midsByTidsAndLids().get(getInvalidArgumentHandler("uid parameter is required"));
    }

    @Test
    @Title("Проверка вызова с пустым UID")
    @IgnoreForPg("MAILPG-2767")
    public void mustReturn400ForEmptyUid() {
        apiHoundV2().midsByTidsAndLids().withUid("").get(getInvalidArgumentHandler(
                "uid parameter is required"));
    }

    @Test
    @Title("Проверка вызова с неизвестным UID")
    @IgnoreForPg("MAILPG-2767")
    public void mustReturn400ForUnknownUid() {
        apiHoundV2().midsByTidsAndLids().withUid(UNEXISTING_UID).withTid("0").withLid("0")
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным UID")
    @IgnoreForPg("MAILPG-2767")
    public void mustReturn400ForIncorrectUid() {
        apiHoundV2().midsByTidsAndLids().withUid("incorrect").withTid("0").withLid("0")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова с отсутствующими TID")
    public void mustReturn400ForAbsentTid() {
        apiHoundV2().midsByTidsAndLids().withUid(uid()).withLid("0").get(
                getInvalidArgumentHandler("TID parameters are required"));
    }

    @Test
    @Title("Проверка вызова с отсутствующими LID")
    public void mustReturn400ForAbsentLid() {
        apiHoundV2().midsByTidsAndLids().withUid(uid()).withTid("0").get(
                getInvalidArgumentHandler("LID parameters are required"));
    }

    @Test
    @Title("Проверка вызова с пустым TID")
    public void mustReturn400ForEmptyTid() {
        apiHoundV2().midsByTidsAndLids().withUid(uid()).withTid("0").withTid("").withTid("1").withLid("0").
                get(getInvalidArgumentHandler("TID must not be empty"));
    }

    @Test
    @Title("Проверка вызова с пустым LID")
    public void mustReturn400ForEmptyLid() {
        apiHoundV2().midsByTidsAndLids().withUid(uid()).withTid("0").withLid("0").withLid("").withLid("1").
                get(getInvalidArgumentHandler("LID must not be empty"));
    }

    @Test
    @Title("Проверка вызова с корректными параметрами")
    @Description("Отправляем три письма в три разных thread`а; отправляем ещё два письма в четвёртый " +
            "thread; сохраняем идентификаторы последних трёх thread`ов; помечаем метками письма во всех " +
            "thread`ах, кроме первого; помечаем дополнительной меткой одно письмо в последнем thread`е. " +
            "Проверяем, что возвращаются MID всех помеченных метками писем")
    public void mustReturnMidsForCorrectParameters() throws Exception {
        sendMessage();

        List<String> tids = new ArrayList<String>();
        tids.add(sendMessage().getTid());

        WaitedMailbox mailbox = sendMessage();
        tids.add(mailbox.getTid());
        List<String> expectedMids = new ArrayList<String>();
        expectedMids.addAll(mailbox.getMids());

        int count = 2;
        mailbox = sendMessage(count);
        tids.add(mailbox.getTid());
        expectedMids.addAll(mailbox.getMids());

        String commonLid = Mops.newLabelByName(authClient, Util.getRandomString());
        Mops.label(authClient, new MidsSource(expectedMids), Collections.singletonList(commonLid)).post(
                shouldBe(okSync()));

        String extraLid = Mops.newLabelByName(authClient, Util.getRandomString());
        Mops.label(authClient, new MidsSource(expectedMids.get(expectedMids.size() - 1)), Collections.
                singletonList(extraLid)).post(shouldBe(okSync()));

        List<String> actualMids = getMidsByTidsAndLids(tids, asList(commonLid, extraLid));

        assertEquals("Размер списка полученных MID не соответствуют ожидаемому", expectedMids.size(),
                actualMids.size());
        assertTrue("Полученные MID не соответствуют ожидаемым", actualMids.containsAll(expectedMids));
    }

    @NotNull
    private Function<Response, Response> getInvalidArgumentHandler(String reason) {
        return shouldBe(invalidArgument(equalTo(reason)));
    }

    @NotNull
    private Function<Response, Response> getHandler(int status) {
        return shouldBe(new ResponseSpecBuilder().expectStatusCode(status).build());
    }

    private WaitedMailbox sendMessage() {
        int count = 1;
        return sendMessage(count);
    }

    private WaitedMailbox sendMessage(int count) {
        return sendWith(authClient).viaProd().subj(Util.getRandomString()).count(count).send().waitDeliver();
    }

    private List<String> getMidsByTidsAndLids(@NotNull List<String> tids, List<String> lids) {
        ApiMidsByTidsAndLids api = apiHoundV2().midsByTidsAndLids().withUid(uid());
        for (String tid : tids) {
            api.withTid(tid);
        }

        for (String lid : lids) {
            api.withLid(lid);
        }

        return api.get(shouldBe(ok200())).body().as(V2MidsByTidsAndLidsResponse.class).getMids();
    }
}
