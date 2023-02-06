package ru.yandex.autotests.innerpochta.mops;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.TimeUnits.nearFutureReplyLaterUnixTime;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.TimeUnits.nextDayUnixTime;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.stickerWrongDate;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.markMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;

@Aqua.Test
@Title("[MOPS] Создание и удаление reply_later-стикеров")
@Features(MyFeatures.MOPS)
@Stories(MyStories.REPLY_LATER)
@Issue("MAILPG-4515")
@Credentials(loginGroup = "ReplyLaterTest")
@RunWith(DataProviderRunner.class)
public class ReplyLaterTest extends MopsBaseTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

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
    @Title("Создаём стикер, проверяем метаданные письма")
    public void shouldMoveAndLabelEnvelopeWhenCreateReplyLaterSticker() {
        String mid = sendMail().firstMid();

        createReplyLaterSticker(mid, nextDayUnixTime())
                .post(shouldBe(okEmptyJson()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не в папке reply_later", envelope.getFid(), equalTo(folderList.replyLaterFID()));
        assertThat("Письмо не помечено меткой reply_later_started",
                authClient, hasMsgWithLid(mid, labels().lidBySymbol(REPLY_LATER_STARTED)));
    }

    @Test
    @Title("Создаём стикер, ждём поднятия")
    public void shouldLiftEnvelope() {
        String subject = Util.getRandomString();

        String mid = sendMail(subject).firstMid();

        createReplyLaterSticker(mid, nearFutureReplyLaterUnixTime())
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
    @Title("Проверяем папку назначения при поднимании стикера")
    @Description("Проверяем, что после удаления изначальной папки письмо перемещается в inbox, когда стикер поднимается")
    public void shouldMoveEnvelopeToInboxWhenOriginalFolderRemovedAndStickerLifted() throws Exception {
        String fid = Mops.newFolder(authClient, Util.getRandomString());

        String subject = Util.getRandomString();
        String mid = sendMail(subject).firstMid();

        Mops.complexMove(authClient, fid, new MidsSource(mid))
                .post(shouldBe(okSync()));

        createReplyLaterSticker(mid, nearFutureReplyLaterUnixTime())
                .post(shouldBe(okEmptyJson()));

        Mops.deleteFolder(authClient, fid).post(shouldBe(okSync()));

        waitWith.subj(subject).waitDeliver();

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не переместилось из папки reply_later", envelope.getFid(),
                equalTo(folderList.defaultFID()));
    }

    @Test
    @Title("Проверяем сохранение таба при поднятии письма")
    @UseDataProvider("existingTabs")
    public void shouldRestoreTabAfterLiftEnvelope(Tabs.Tab tab) throws Exception {
        String subject = Util.getRandomString();

        String mid = sendMail(subject).firstMid();
        markMessageForTab(authClient, mid, tab);

        createReplyLaterSticker(mid, nearFutureReplyLaterUnixTime())
                .post(shouldBe(okEmptyJson()));

        waitWith.subj(subject).waitDeliver();

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не вернулось в прежний таб", envelope.getTab(), equalTo(tab.getName()));
    }


    @Test
    @Title("Создаём стикер с временем поднятия 0 секунд, проверяем ошибку ответа и отсутствие стикера")
    public void shouldFailOnWrongDateWhenCreateReplyLaterSticker() {
        String mid = sendMail().firstMid();

        createReplyLaterSticker(mid, Instant.now().getEpochSecond())
                .post(shouldBe(stickerWrongDate()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо в папке reply_later", envelope.getFid(), not(equalTo(folderList.replyLaterFID())));
        assertThat("Письмо помечено меткой reply_later_started",
                authClient, not(hasMsgWithLid(mid, labels().lidBySymbol(REPLY_LATER_STARTED))));
    }
    @Test
    @Title("Создаём стикер с временем поднятия больше года, проверяем ошибку ответа и отсутствие стикера")
    public void shouldFailOnBigDateWhenCreateReplyLaterSticker() {
        String mid = sendMail().firstMid();

        createReplyLaterSticker(mid, Instant.now().plus(367, ChronoUnit.DAYS).getEpochSecond())
                .post(shouldBe(stickerWrongDate()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо в папке reply_later", envelope.getFid(), not(equalTo(folderList.replyLaterFID())));
        assertThat("Письмо помечено меткой reply_later_started",
                authClient, not(hasMsgWithLid(mid, labels().lidBySymbol(REPLY_LATER_STARTED))));
    }
}
