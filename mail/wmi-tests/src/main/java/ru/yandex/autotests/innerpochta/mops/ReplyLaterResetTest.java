package ru.yandex.autotests.innerpochta.mops;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.beans.hound.Sticker;
import ru.yandex.autotests.innerpochta.beans.hound.V2Stickers;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.stickers.ApiStickers;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol.PINNED_LABEL;
import static ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol.REPLY_LATER_FINISHED;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MopsApi.apiMops;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.TimeUnits.nearFutureReplyLaterUnixTime;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.TimeUnits.nextDayUnixTime;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.stickerDoesNotExist;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;

@Aqua.Test
@Title("[MOPS] Переустановка времени reply_later-стикера после поднятия")
@Features(MyFeatures.MOPS)
@Stories(MyStories.REPLY_LATER)
@Credentials(loginGroup = "ReplyLaterResetTest")
public class ReplyLaterResetTest extends MopsBaseTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders().before(true).after(false);

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).all();

    @Before
    public void createReplyLaterFolderIfNotExists() {
        if (folderList.replyLaterFID() == null) {
            Mops.newFolder(authClient, "reply_later", Symbol.REPLY_LATER);
            folderList = updatedFolderList();
        }
    }

    @Test
    @Title("Проверяем reset стикера без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldResponseBadRequestWhenUidNotPassed() {
        apiMops(authClient.account().userTicket())
                .replyLater().reset()
                .withMid(NOT_EXIST_MID)
                .post(shouldBe(missingExpectedParam()));
    }

    @Test
    @Title("Проверяем reset стикера без даты")
    public void shouldResponseBadRequestWhenDateNotPassed() {
        apiMops(authClient.account().userTicket())
                .replyLater().reset()
                .withUid(authClient.account().uid())
                .withMid(NOT_EXIST_MID)
                .post(shouldBe(missingExpectedParam()));
    }

    @Test
    @Title("Проверяем reset стикера с некорректной датой")
    public void shouldResponseBadRequestWhenDateIsInvalid() {
        apiMops(authClient.account().userTicket())
                .replyLater().reset()
                .withUid(authClient.account().uid())
                .withMid(INVALID_MID)
                .withDate(INVALID_DATE)
                .post(shouldBe(missingExpectedParam()));
    }

    @Test
    @Title("Проверяем reset стикера без мида")
    public void shouldResponseBadRequestWhenMidNotPassed() {
        apiMops(authClient.account().userTicket())
                .replyLater().reset()
                .withUid(authClient.account().uid())
                .withDate(String.valueOf(Instant.now().getEpochSecond()))
                .post(shouldBe(missingExpectedParam()));
    }

    @Test
    @Title("Проверяем reset стикера с несуществующим mid'ом")
    public void shouldResponseBadRequestWhenMidNotExists() {
        apiMops(authClient.account().userTicket())
                .replyLater().reset()
                .withUid(authClient.account().uid())
                .withDate(String.valueOf(Instant.now().getEpochSecond()))
                .withMid(NOT_EXIST_MID)
                .post(shouldBe(stickerDoesNotExist()));
    }

    @Test
    @Title("Проверяем reset стикера с некорректным mid'ом")
    public void shouldResponseBadRequestWhenMidIsInvalid() {
        apiMops(authClient.account().userTicket())
                .replyLater().reset()
                .withUid(authClient.account().uid())
                .withMid(INVALID_MID)
                .withDate(String.valueOf(Instant.now().getEpochSecond()))
                .post(shouldBe(stickerDoesNotExist()));
    }

    @Test
    @Title("Создаём стикер, ждем поднятия, изменяем дату, ждем поднятия")
    public void shouldLiftEnvelopeAfterReset() {
        String subject = Util.getRandomString();

        String mid = sendMail(subject).firstMid();

        createReplyLaterSticker(mid, nearFutureReplyLaterUnixTime())
                .post(shouldBe(okEmptyJson()));

        waitWith.subj(subject).waitDeliver();

        Long newDate = Instant.now().plus(5, ChronoUnit.SECONDS).getEpochSecond();
        resetReplyLaterSticker(mid, newDate)
                .post(shouldBe(okEmptyJson()));

        waitWith.subj(subject).waitDeliver();

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не вернулось в inbox", envelope.getFid(), equalTo(folderList.defaultFID()));
        assertThat("Письмо не помечено меткой reply_later_finished", envelope.getLabels(),
                hasItems(labels().lidBySymbol(REPLY_LATER_FINISHED)));
        assertThat("Письмо не помечено меткой pinned", envelope.getLabels(),
                hasItems(labels().lidBySymbol(PINNED_LABEL)));
    }

    @Test
    @Title("Создаём стикер, ждем поднимания, изменяем дату, проверяем корректность измененной даты")
    public void shouldResetReplyLaterSticker() {
        String subject = Util.getRandomString();

        String mid = sendMail(subject).firstMid();

        createReplyLaterSticker(mid, nearFutureReplyLaterUnixTime())
                .post(shouldBe(okEmptyJson()));

        waitWith.subj(subject).waitDeliver();

        Long newDate = nextDayUnixTime();
        resetReplyLaterSticker(mid, newDate)
                .post(shouldBe(okEmptyJson()));

        List<Sticker> stickers = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();

        assertThat("Должны получить один новый стикер", stickers, hasSize(1));
        assertThat("Должны новый стикер на тот же mid", stickers.get(0).getMid(), equalTo(mid));
        assertThat("Дата не изменилась", stickers.get(0).getDate(), equalTo(newDate));
    }
}
