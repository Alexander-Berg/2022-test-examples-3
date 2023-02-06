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
import ru.yandex.autotests.innerpochta.beans.hound.Sticker;
import ru.yandex.autotests.innerpochta.beans.hound.V2Stickers;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.stickers.ApiStickers;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol.*;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.INVALID_MID;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.NOT_EXIST_MID;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MopsApi.apiMops;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.notHasMsgsWithLids;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.TimeUnits.nearFutureReplyLaterUnixTime;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.TimeUnits.nextDayUnixTime;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.markMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;

@Aqua.Test
@Title("[MOPS] Удаление reply_later-стикеров")
@Features(MyFeatures.MOPS)
@Stories(MyStories.REPLY_LATER)
@Credentials(loginGroup = "ReplyLaterRemoveTest")
@RunWith(DataProviderRunner.class)
public class ReplyLaterRemoveTest extends MopsBaseTest {
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
    @Title("Проверяем удаление стикера без uid'а")
    public void shouldResponseBadRequestWhenUidNotPassed() {
        apiMops(authClient.account().userTicket())
                .replyLater().remove()
                .withMid("123")
                .post(shouldBe(missingExpectedParam()));
    }

    @Test
    @Title("Проверяем удаление стикера без мида")
    public void shouldResponseBadRequestWhenMidNotPassed() {
        apiMops(authClient.account().userTicket())
                .replyLater().remove()
                .withUid(authClient.account().uid())
                .post(shouldBe(missingExpectedParam()));
    }

    @Test
    @Title("Проверяем удаление стикера с несуществующим mid'ом")
    public void shouldResponseBadRequestWhenMidNotExists() {
        apiMops(authClient.account().userTicket())
                .replyLater().remove()
                .withUid(authClient.account().uid())
                .withMid(NOT_EXIST_MID)
                .post(shouldBe(stickerDoesNotExist()));
    }

    @Test
    @Title("Проверяем удаление стикера с некорректным mid'ом")
    public void shouldResponseBadRequestWhenMidIsInvalid() {
        apiMops(authClient.account().userTicket())
                .replyLater().remove()
                .withUid(authClient.account().uid())
                .withMid(INVALID_MID)
                .post(shouldBe(stickerDoesNotExist()));
    }

    @Test
    @Title("Проверяем удаление стикера до поднятия")
    public void shouldRemoveReplyLaterStickerBeforeLift() {
        String mid = sendMail().firstMid();

        createReplyLaterSticker(mid, nextDayUnixTime())
                .post(shouldBe(okEmptyJson()));

        List<Sticker> stickers = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();
        assertThat("Должны получить один стикер", stickers, hasSize(1));

        removeReplyLaterSticker(mid)
                .post(shouldBe(okEmptyJson()));

        List<Sticker> stickersAfterRemove = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();
        assertThat("Стикер не удалён", stickersAfterRemove, hasSize(0));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо помечено меткой reply_later_finished после удаления стикера", envelope.getLabels(),
                not(hasItems(labels().lidBySymbol(REPLY_LATER_FINISHED))));
        assertThat("Письмо помечено меткой reply_later_started после удаления стикера", envelope.getLabels(),
                not(hasItems(labels().lidBySymbol(REPLY_LATER_STARTED))));
        assertThat("Письмо помечено меткой pinned_label после удаления стикера", envelope.getLabels(),
                not(hasItems(labels().lidBySymbol(PINNED_LABEL))));
    }

    @Test
    @Title("Проверяем удаление стикера после поднятия")
    public void shouldRemoveReplyLaterStickerAfterLift() {
        String subject = Util.getRandomString();

        String mid = sendMail(subject).firstMid();

        createReplyLaterSticker(mid, nearFutureReplyLaterUnixTime())
                .post(shouldBe(okEmptyJson()));

        waitWith.subj(subject).waitDeliver();

        List<Sticker> stickers = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();
        assertThat("Должны получить один стикер", stickers, hasSize(1));

        removeReplyLaterSticker(mid)
                .post(shouldBe(okEmptyJson()));

        List<Sticker> stickersAfterRemove = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();
        assertThat("Стикер не удалён", stickersAfterRemove, hasSize(0));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо помечено меткой reply_later_finished после удаления стикера", envelope.getLabels(),
                not(hasItems(labels().lidBySymbol(REPLY_LATER_FINISHED))));
        assertThat("Письмо помечено меткой reply_later_started после удаления стикера", envelope.getLabels(),
                not(hasItems(labels().lidBySymbol(REPLY_LATER_STARTED))));
        assertThat("Письмо помечено меткой pinned_label после удаления стикера", envelope.getLabels(),
                not(hasItems(labels().lidBySymbol(PINNED_LABEL))));
    }

    @Test
    @Title("Проверяем метаданные письма после удаления с него стикера")
    public void shouldMoveAndUnlabelEnvelopeWhenRemoveReplyLaterSticker() {
        String mid = sendMail().firstMid();

        createReplyLaterSticker(mid, nextDayUnixTime())
                .post(shouldBe(okEmptyJson()));

        removeReplyLaterSticker(mid)
                .post(shouldBe(okEmptyJson()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не переместилось из папки reply_later", envelope.getFid(),
                equalTo(folderList.defaultFID()));
        assertThat("С письма не снята метка reply_later_started",
                authClient, notHasMsgsWithLids(asList(mid), asList(labels().lidBySymbol(REPLY_LATER_STARTED))));
    }

    @Test
    @Title("Проверяем папку назначения при удалении стикера")
    @Description("Проверяем, что после удаления изначальной папки письмо перемещается в inbox, когда стикер удаляется")
    public void shouldMoveEnvelopeToInboxWhenOriginalFolderRemovedAndStickerRemoved() throws Exception {
        String fid = Mops.newFolder(authClient, Util.getRandomString());

        String mid = sendMail().firstMid();

        Mops.complexMove(authClient, fid, new MidsSource(mid))
                .post(shouldBe(okSync()));

        createReplyLaterSticker(mid, nextDayUnixTime())
                .post(shouldBe(okEmptyJson()));

        Mops.deleteFolder(authClient, fid).post(shouldBe(okSync()));

        removeReplyLaterSticker(mid).post(shouldBe(okEmptyJson()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не переместилось из папки reply_later", envelope.getFid(),
                equalTo(folderList.defaultFID()));
    }

    @Test
    @Title("Проверяем сохранение таба при удалении стикера")
    @UseDataProvider("existingTabs")
    public void shouldRestoreTabAfterRemoveSticker(Tabs.Tab tab) throws Exception {
        String subject = Util.getRandomString();

        String mid = sendMail(subject).firstMid();
        markMessageForTab(authClient, mid, tab);

        createReplyLaterSticker(mid, nextDayUnixTime())
                .post(shouldBe(okEmptyJson()));

        removeReplyLaterSticker(mid)
                .post(shouldBe(okEmptyJson()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid))
                .get().via(authClient)
                .parsed().getEnvelopes().get(0);

        assertThat("Письмо не вернулось в прежний таб", envelope.getTab(), equalTo(tab.getName()));
    }

    @Test
    @Title("Проверяем проставление стикера после удаления на одном письме")
    public void shouldCreateReplyLaterStickerAfterRemove() {
        String mid = sendMail().firstMid();

        createReplyLaterSticker(mid, nextDayUnixTime())
                .post(shouldBe(okEmptyJson()));

        removeReplyLaterSticker(mid)
                .post(shouldBe(okEmptyJson()));

        Long newDate = nearFutureReplyLaterUnixTime();
        createReplyLaterSticker(mid, newDate)
                .post(shouldBe(okEmptyJson()));

        List<Sticker> stickers = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();

        assertThat("Должны получить один стикер", stickers, hasSize(1));
        assertThat("Должны новый стикер на тот же mid", stickers.get(0).getMid(), equalTo(mid));
        assertThat("Дата не изменилась", stickers.get(0).getDate(), equalTo(newDate));
    }
}
