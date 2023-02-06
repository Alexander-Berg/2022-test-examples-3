package ru.yandex.autotests.innerpochta.hound;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.Folder;
import ru.yandex.autotests.innerpochta.beans.yplatform.To;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.LabelsObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("Тесты hound с кириллическими штуками")
@Description("Тест парсинга реципиентов")
@Features(MyFeatures.HOUND)
@Stories({MyStories.OTHER})
@Credentials(loginGroup = "CyrillicRecipientInDraftTest")
@Issue("MAILPG-710")
@RunWith(DataProviderRunner.class)
public class CyrillicTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @Test
    @Title("Тест парсинга адресов с кириллическими символами")
    @DataProvider({
            "Вася",
            "вася@",
            "йцу@кен",
            "йцу@кен.рф",
            "привет@yandex.ru",
            "привет.пдд",
            "вася и три слова",
            "д'Вася",
            "@вася@",
            "@g@in@nd@g@in",
            "вася?",
            "вася!",
            "вася ^_^",
            "some.person@ya.ru",
            "do...ot@ya.ru"
    })
    public void testRecipientsWithCyrillicCharacters(String recipient) {
        String mid = sendWith(authClient).viaProd().to(recipient).saveDraft().waitDeliver().getMid();

        FilterSearchCommand search = filterSearch(empty().setUid(uid())
                .setOrder("date1").setFids(folderList.draftFID())
                .setMids(mid)).get().via(authClient);

        List<Envelope> envelopes = search.parsed().getEnvelopes();
        assertThat("Ожидается строго одно письмо", envelopes.size(), equalTo(1));

        List<To> to = envelopes.get(0).getTo();
        assertThat("Адресат должен быть строго один (" + recipient + ")", to.size(), equalTo(1));
        assertThat("Неверный display-name", to.get(0).getDisplayName(), equalTo(recipient));
    }

    @Test
    @Title("Ручка folders c кириллическим именем папки")
    @Description("Создаём папку. Проверяем, что ручка возвращает созданную папку.")
    public void testFoldersHandlerWithUserAndCyrillicFolder() {
        String folderName = CYRILLIC_STRING;
        String fid = Mops.newFolder(authClient, folderName);

        Map<String, Folder> folders = Hound.folders(authClient);

        assertTrue("Не нашли созданную папку", folders.entrySet().stream()
                .filter(f -> f.getKey().equals(fid))
                .anyMatch(f -> f.getValue().getName().equals(folderName)));
    }

    @Test
    @Title("Ручка labels с кириллическим именем метки")
    @Description("Создаём метку. Проверяем, что ручка возвращает созданную метку.")
    public void testLabelsHandlerWithUserCyrillicLabel() {
        String labelName = CYRILLIC_STRING;
        Mops.newLabelByName(authClient, labelName);

        Labels labels = api(Labels.class)
                .setHost(props().houndUri())
                .params(LabelsObj.empty().setUid(uid()))
                .get()
                .via(authClient);

        MatcherAssert.assertThat("Не нашли созданную метку", labels.lidByName(labelName), notNullValue());
    }

    @Test
    @Title("Должны возвращать письма в папке с кириллической темой")
    @Description("Посылаем письмо с кириллической темой. Ожидаем в ответе это письмо с правильной темой")
    public void testMessagesByFolderHandlerWithUserCyrillicSubject() {
        String subject = CYRILLIC_STRING;
        String expectedMid = sendWith(authClient).subj(subject).viaProd().send().waitDeliver().getMid();

        List<ru.yandex.autotests.innerpochta.beans.Envelope> envelopes = api(MessagesByFolder.class)
                .setHost(props().houndUri())
                .params(MessagesUnreadObj.empty().setUid(uid())
                        .setCount("30")
                        .setFirst("0"))
                .get()
                .via(authClient)
                .resp().getEnvelopes();

        assertTrue("Не нашли письмо с кириллической темой", envelopes.stream()
                .anyMatch(e -> e.getSubject().equals(subject) && e.getMid().equals(expectedMid)));
    }

    private static final String CYRILLIC_STRING = "Холодно ёлочке зимой";
}
