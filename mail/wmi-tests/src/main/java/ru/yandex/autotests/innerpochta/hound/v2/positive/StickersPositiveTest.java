package ru.yandex.autotests.innerpochta.hound.v2.positive;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.beans.hound.Sticker;
import ru.yandex.autotests.innerpochta.beans.hound.V2Stickers;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.stickers.ApiStickers;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/stickers")
@Description("Тесты на ручку v2/stickers")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2StickersTest")
public class StickersPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Before
    public void createReplyLaterFolderIfNotExists() {
        if (folderList.replyLaterFID() == null) {
            Mops.newFolder(authClient, "reply_later", Symbol.REPLY_LATER);
            folderList = updatedFolderList();
        }
    }

    @Test
    @Title("Проверяем, что ответ пустой, когда стикеров нет")
    public void shouldReturnEmptyResponseWhenStickersDoNotExist() {
        List<Sticker> stickers = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();
        assertThat("Должны получить ноль стикеров", stickers, hasSize(0));
    }

    @Test
    @Title("Создаём стикер, проверяем его аттрибуты")
    public void shouldCreateReplyLaterStickerWithCorrectAttributes() {
        Optional<Envelope> envelopeOpt = sendWith(authClient).viaProd().send().strict().waitDeliver().getEnvelope();
        assertTrue("Должны получить один envelope", envelopeOpt.isPresent());
        Envelope envelope = envelopeOpt.get();

        Mops.createReplyLaterSticker(authClient, envelope.getMid(), nextDayUnixTime)
                .post(shouldBe(okEmptyJson()));

        List<Sticker> stickers = Hound.stickers(authClient, ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(ok200()))
                .as(V2Stickers.class)
                .getStickers();
        assertThat("Должны получить один стикер", stickers, hasSize(1));

        Sticker sticker = stickers.get(0);
        assertThat("Неожиданный mid стикера", sticker.getMid(), equalTo(envelope.getMid()));
        assertThat("Неожиданный fid стикера", sticker.getFid(), equalTo(envelope.getFid()));
        assertThat("Неожиданный tid стикера", sticker.getTid(), equalTo(envelope.getThreadId()));
        assertThat("Неожиданный tab стикера", sticker.getTab(), equalTo(envelope.getTab()));
        assertThat("Неожиданный date стикера", sticker.getDate(), equalTo(nextDayUnixTime));
        assertThat("Неожиданный type стикера", sticker.getType(), equalTo(ApiStickers.TypeParam.REPLY_LATER.value()));
    }


    private static Long nextDayUnixTime = Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
}
