package ru.yandex.autotests.innerpochta.api.b2b;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.Common;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.pagediffer.document.filter.ExcludeXmlNodesFilter;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.SORT_DATE_ASC;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj.inFid;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;

@Aqua.Test
@Title("[B2B] mailbox_list без параметров")
@Description("Сравнение с продакшеном")
@RunWith(Parameterized.class)
@Features(MyFeatures.API_WMI)
@Stories({MyStories.B2B, MyStories.MESSAGES_LIST})
@Credentials(loginGroup = MailboxListBasicTest.LOGIN_GROUP)
public class MailboxListBasicTest extends BaseTest {
    public static final String LOGIN_GROUP = "Zoo";

    @Parameterized.Parameters(name = "folder-{0}")
    public static Collection<Object[]> folders() throws Exception {
        HttpClientManagerRule manager4 = auth().with(LOGIN_GROUP).login();

        List<String> folderIds = jsx(FolderList.class)
                .post().via(manager4.authHC()).getAllFolderIds();
        return Common.toParameterized(folderIds);
    }

    private String folderId;

    public MailboxListBasicTest(String folderId) {
        this.folderId = folderId;
    }

    @Test
    @Description("Сравниваем с продакшеном выдачу mailbox_list во всех папках.\n" +
            "В какой-то момент в 2016 году tzdata поменяла мнение,\n" +
            "когда переводили время в 2011 и 2012 году, из-за разницы\n" +
            "в таймстемпах старых писем в проде и тестинге тест падал.\n" +
            "Поэтому исключаем из сравнения ноды message c атрибутом date,\n" +
            "начинающимся с 2011 или 2012")
    public void mailboxesDontDifferInAllFolders() throws Exception {
        MailBoxList testing = api(MailBoxList.class)
                .params(inFid(folderId).setSortType(SORT_DATE_ASC))
                .post().via(hc);

        testing.setHost(props().productionHost());
        MailBoxList base = testing.post().via(hc);

        assertThat(testing.toDocument(), equalToDoc(base.toDocument())
                .filterWithCondition(new ExcludeXmlNodesFilter<>("//message[starts-with(@date,'2011')]"), true)
                .filterWithCondition(new ExcludeXmlNodesFilter<>("//message[starts-with(@date,'2012')]"), true));
    }

    @Test
    @Description("Проверяем для всех папок, что в mailbox_list для папки присутствует фид.")
    public void currentFidEqualsDetailsFid() throws Exception {
        MailBoxList testing = api(MailBoxList.class)
                .params(inFid(folderId))
                .post().via(hc);
        assertThat("Значение текущей папки не совпадает с запрошенным", testing.getCurrentFid(), equalTo(folderId));
    }
}
