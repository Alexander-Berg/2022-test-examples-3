package ru.yandex.autotests.innerpochta.barbet;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.beans.hound.ArchiveState;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnread;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesWithAttaches;
import ru.yandex.autotests.innerpochta.wmi.core.oper.shiva.Shiva;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmi.core.utils.FolderList;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.barbet.ArchiveMatcher.archiveIsInState;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesWithAttachesObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[Barbet] Позитивные тесты на архивацию")
@Credentials(loginGroup = "BarbetArchivation")
@RunWith(Parameterized.class)
@Features(MyFeatures.BARBET)
@Stories({MyStories.ARCHIVATION, MyStories.ATTACH})
public class ArchivationPositiveTest extends ArchivationBaseTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).before(false).allfolders();

    @Rule
    public DeleteFoldersRule deleteFolders = DeleteFoldersRule.with(authClient).all().before(true);

    @Before
    public void prepare() {
        prepareRestoredFolder();
    }

    public int msgCount;
    public int attachesCount;

    @Parameterized.Parameters(name = "msgCount = {0}, attachesCount = {1}")
    public static Collection<Object[]> data() {
        return newArrayList(
                new Object[]{4, 0},
                new Object[]{2, 1},
                new Object[]{3, 2}
        );
    }

    public ArchivationPositiveTest(int msgCount, int attachesCount) {
        this.msgCount = msgCount;
        this.attachesCount = attachesCount;
    }

    @SneakyThrows
    private void sendMessages() {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < attachesCount; ++i) {
            files.add(AttachUtils.genFile(1));
            files.get(files.size() - 1).deleteOnExit();
        }
        sendWith(authClient).count(msgCount).addAttaches(files).send().waitDeliver();
    }


    private void archiveUser() {
        Shiva.freezeUser(authClient).get(shouldBe(Shiva.done200()));
        Shiva.purgeTransferredUser(authClient).get(shouldBe(Shiva.done200()));
        Shiva.archiveUser(authClient).get(shouldBe(Shiva.done200()));
    }

    private void validateMessageCount(Integer expectedRestoredCount) {
        FolderList folders = new FolderList(authClient);
        assertThat(folders.count(folders.defaultFID()), equalTo(0));
        assertThat(folders.count(folders.fidBySymbol(Symbol.RESTORED)), equalTo(expectedRestoredCount));
    }

    @Test
    @Title("Успешная архивация и разархивация")
    public void shouldSuccessfullyArchiveAndRestore() {
        sendMessages();
        archiveUser();

        restore().post(shouldBe(ok200()));
        assertThat(authClient, withWaitFor(archiveIsInState(ArchiveState.RESTORATION_COMPLETE)));

        validateMessageCount(msgCount);

        List<Envelope> envelopes = api(MessagesWithAttaches.class)
                .setHost(props().houndUri())
                .params(empty().setUid(getUid())
                        .setFirst("0")
                        .setCount("30"))
                .get()
                .via(authClient)
                .resp().getEnvelopes();

        Integer unreadSize = api(MessagesUnread.class)
                .setHost(props().houndUri())
                .params(empty().setUid(getUid())
                        .setCount("30")
                        .setFirst("0"))
                .get()
                .via(authClient)
                .resp().getEnvelopes().size();

        Shiva.purgeArchive(authClient).get(shouldBe(Shiva.done200()));
        assertThat(authClient, withWaitFor(archiveIsInState(null)));

        assertThat("Неверное количество писем", envelopes.size(), equalTo(msgCount * Math.min(attachesCount, 1)));
        assertThat("Неправильное количество прочитанных писем", unreadSize, equalTo(0));

        assertThat("Неверное количество аттачей", envelopes.stream()
                .allMatch(e -> e.getAttachmentsCount() == attachesCount));
    }

    @Test
    @Title("Успешная архивация и восстановление, с удалением писем")
    public void shouldSuccessfullyArchiveAndDiscard() {
        sendMessages();
        archiveUser();

        discard().post(shouldBe(ok200()));
        assertThat(authClient, withWaitFor(archiveIsInState(ArchiveState.CLEANING_IN_PROGRESS)));
        Shiva.cleanArchive(authClient).get(shouldBe(Shiva.done200()));

        validateMessageCount(null);

        assertThat(authClient, withWaitFor(archiveIsInState(null)));
    }


}
