package ru.yandex.autotests.innerpochta.sendbernar;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SaveDraftResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.CountMessagesMatcher.hasCountMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;


@Aqua.Test
@Title("Удаляем черновики с включенной Скрытой Корзиной")
@Description("Удаленные после отправки черновики не должны оседать в скрытой корзине")
@Features(MyFeatures.SENDBERNAR)
@Credentials(loginGroup = "DraftsWithEnabledHiddenTrash")
public class DraftsWithEnabledHiddenTrashTest extends BaseSendbernarClass {
    private static final long WAIT_TIME = MINUTES.toMillis(1);

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Before
    public void prepare() throws Exception {
        Mops.createHiddenTrash(authClient).post(shouldBe(okFid()));
    }

    @Test
    @Title("Проверяем удаление черновиков")
    @Description("Сохраняем черновик, отправляем его и проверяем скрытую корзину")
    public void shouldDeleteDraftNotToHiddenTrash() throws Exception {
        String checkSubj = subj + "_CHECK_HIDDEN_TRASH_WORK";
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(checkSubj)
                .post(shouldBe(ok200()));

        String checkMid = waitWith.subj(checkSubj).waitDeliver().getMid();
        Mops.purge(authClient, new MidsSource(checkMid)).post(shouldBe(okSync()));

        assertThat("Письмо удалилось не в скрытую корзину - сбились настройки?",
                authClient,
                hasCountMsgsIn(equalTo(1), folderList.hiddenTrashFID()));

        String mid = saveDraft()
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class)
                .getStored()
                .getMid();

        assertThat("Черновик не сохранился",
                authClient,
                hasCountMsgsIn(equalTo(1), folderList.draftFID()));

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withSourceMid(mid)
                .post(shouldBe(ok200()));

        assertThat("Черновик не удалился",
                authClient,
                withWaitFor(hasCountMsgsIn(equalTo(0), folderList.draftFID()), WAIT_TIME));
        assertThat("Черновик удалился в скрытую корзину",
                authClient,
                hasCountMsgsIn(equalTo(1), folderList.hiddenTrashFID()));
    }
}
