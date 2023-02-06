package ru.yandex.autotests.innerpochta.wmi.b2b;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.GetFirstEnvelopeDateObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetFirstEnvelopeDate;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.PagerMailboxList;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter.from;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.deleteNodesFromDocument;

//TODO Пустой папко, отправить тудо письмо, посмотреть дату

/**
 * @author lanwen
 *         Ручка jsxapi/simple.jsx?wmi-method=pager_mailbox_list
 *         unmodify
 */
@Aqua.Test
@Title("[B2B] Тестирование пейджера по датам")
@Description("Сравнение с выводом mailbox_list")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories({MyStories.B2B, MyStories.MESSAGES_LIST})
@Credentials(loginGroup = "ZooNew")
public class PagerMailboxListTest extends BaseTest {

    @Parameterized.Parameter
    public MailBoxListObj mbobj;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return newArrayList(

                //---------------------
                new Object[]{  // 1я страница
                        MailBoxListObj.empty().setPageNumber(1)
                },

                new Object[]{  // вывод daria2
                        MailBoxListObj.empty().setPageNumber(1)
                                .setXmlVersion(MailBoxListObj.XMLVERSION_DARIA2)
                },
                new Object[]{// 2я страница
                        MailBoxListObj.empty().setPageNumber(2)
                },

                new Object[]{ // goto атрибут
                        // Время определяется только для входящих, если в отправленных письмо окажется раньше
                        // то оно не попадет в выдачу пейджера
                        MailBoxListObj.empty().setGoto(MailBoxListObj.GOTO_SENT)
                },

                new Object[]{  // сортировки
                        MailBoxListObj.empty().setSortType(MailBoxListObj.SORT_FROM)
                },

                new Object[]{
                        MailBoxListObj.empty().setSortType(MailBoxListObj.SORT_DATE_ASC)
                },

                //MPROTO-2660 PG only!
//                new Object[]{
//                        MailBoxListObj.empty().setSortType(MailBoxListObj.SORT_SUBJ_DESC)
//                },

                new Object[]{ // Другая сортировка и номер страницы
                        MailBoxListObj.empty()
                                .setPageNumber(2)
                                .setSortType(MailBoxListObj.SORT_DATE_ASC)
                },

                new Object[]{ // Сортировка, номер страницы и другая папка
                        MailBoxListObj.empty()
                                .setPageNumber(2)
                                .setSortType(MailBoxListObj.SORT_DATE_ASC)
                                .setGoto(MailBoxListObj.GOTO_SENT)
                });

    }


    @Test
    @Description("Проверяем что пустая папка отдает пустую выдачу метода пейджера, а не ошибку")
    public void testCheckResultOfFirstEnvIsEmptyOnEmptyFldr() throws Exception {
        FolderList fList = api(FolderList.class).post().via(hc);
        assertTrue("Папки emptyFolder нет. Создайте ее пустой", fList.isThereFolder("emptyFolder"));
        String fid = fList.getFolderId("emptyFolder");

        assertTrue("That folder must be empty",
                jsx(GetFirstEnvelopeDate.class)
                        .params(GetFirstEnvelopeDateObj.getEmptyObj().setCurrentFolder(fid))
                        .post().via(hc).isGetFirstEnvelopeDateEmpty()
        );
    }

    @Test
    @Description("Проверка что при полном диапазоне писем выдачи обоих методов совпадают")
    public void testCheckEqualityOfTwoMethods() throws Exception {
        // Дата первого письма
        GetFirstEnvelopeDate fEnvDateResp = jsx(GetFirstEnvelopeDate.class)
                .params(GetFirstEnvelopeDateObj.getEmptyObj().setCurrentFolder(folderList.defaultFID()))
                .post().via(hc);

        String firstEnvDate = fEnvDateResp.datetimeOfFirstEnvelope(-1);

        // Смотрим во ВХОДЯЩИЕ (! Если не задан goTo - он приоритетней)
        mbobj.setCurrentFolder(folderList.defaultFID());

        // Получаем объекты операций
        PagerMailboxList pagerOper = jsx(PagerMailboxList.class)
                .params(mbobj.copyToPagerObj().setFrom(firstEnvDate).setTo(new Date()));
        MailBoxList mboxOper = jsx(MailBoxList.class).params(mbobj);


        // Получаем ответ пейджера
        Document responsePager = pagerOper.post().via(hc).toDocument();


        // Получаем ответ мейлбокса
        Document responseMailboxList = mboxOper.post().via(hc).toDocument();


        // Проверяем предварительно, что дефолтная папка не пуста
        assertFalse("Default folder cant be empty", fEnvDateResp.isGetFirstEnvelopeDateEmpty());
        // Сравниваем ответы
        compareTwoXmlFiles(responseMailboxList, responsePager);
    }


    /**
     * Удаляет ненужные теги из доков
     * Логирует количество сообщений, указанных в атрибуте msg_count
     * Сравнивает выдачу пейджера и мейлбокса
     *
     * @param mailboxList  - ответ мейлбокса
     * @param pagerMailbox - ответ пейджера
     */
    private void compareTwoXmlFiles(Document mailboxList, Document pagerMailbox) {
        deleteSomeTags(mailboxList);
        deleteSomeTags(pagerMailbox);
        from(mailboxList).log(logger, "MAILBOX_LIST msg_count").byXpath("//@msg_count").asInteger();
        from(pagerMailbox).log(logger, "PAGER msg_count").byXpath("//@msg_count").asInteger();

        assertThat(pagerMailbox, equalToDoc(mailboxList));
    }


    /**
     * Удаляем временные теги
     *
     * @param where документ где
     */
    private void deleteSomeTags(Document where) {
        String[] unnesessaryTags = new String[]{
                "timer_logic",
                "timer_db",
                "mobile_user_agent"
        };
        deleteNodesFromDocument(where, unnesessaryTags);
    }
}
