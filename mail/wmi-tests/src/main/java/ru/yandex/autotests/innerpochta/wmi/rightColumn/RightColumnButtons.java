package ru.yandex.autotests.innerpochta.wmi.rightColumn;

import org.hamcrest.Matcher;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SearchObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageHeader;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Search;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SearchObj.*;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.CommonUtils.getFirstMidAllPagesInFolder;

/**
 * @author jetliner
 *         Тестового пользователя менять нельзя,
 *         если завести свежего, присылать ему письма
 *         и тут же их искать, они не успеют проиндексироваться
 */
@Aqua.Test
@Title("Тестирование правой колонки в Дарье. Общие тесты")
@Description("Проверяет, соответствует ли то, что лежит в ящике, отображенному справа")
@Features(MyFeatures.WMI)
@Stories(MyStories.RIGHT_COLUMN)
@Issue("PS-972")
@Credentials(loginGroup = "Group3")
public class RightColumnButtons extends BaseTest {
    public static final String SIMPLE_MESSAGE_SUBJECT = "just a message";
    public static final String ANOTHER_MESSAGE_SUBJECT = "another message";
    public static final String YET_ANOTHER_MESSAGE_SUBJECT = "yet another message";
    public static final String ANOTHER_MESSAGE_SUBJECT_WITH_LINK_AND_ATTACH = "another message with link and attach";
    public static final String YET_ANOTHER_MSG_SUBJECT_WITH_LINK_AND_ATT = "yet another message with link and attach";

    //single MAIL with no ATTACHES or LINKS
    @Test
    public void attachesSingleMail() throws Exception {
        attachesCountIn(SIMPLE_MESSAGE_SUBJECT, is(0));
    }

    @Test
    public void historySingleMail() throws Exception {
        mailCountIn(SIMPLE_MESSAGE_SUBJECT, is(1));
    }

    @Test
    @Issue("DARIA-55789")
    public void linksSingleMail() throws Exception {
        linksCountIn(SIMPLE_MESSAGE_SUBJECT, is(0));
    }

    @Test
    public void historySomeMailWithAttachORLinks() throws Exception {
        mailCountIn(ANOTHER_MESSAGE_SUBJECT_WITH_LINK_AND_ATTACH, is(7));
    }

    @Test
    public void linksSomeMailNoWithAttachORLinks() throws Exception {
        logger.warn("[SAAS-1433]");
        linksCountIn(ANOTHER_MESSAGE_SUBJECT_WITH_LINK_AND_ATTACH, is(6));
    }

    //more than 100 messages with and witout ATTACHES/LINKS
    @Test
    public void attachesManyMailNoAttachNoLinks() throws Exception {
        logger.warn("[SAAS-1433]");
        attachesCountIn(YET_ANOTHER_MSG_SUBJECT_WITH_LINK_AND_ATT, greaterThanOrEqualTo(10));
    }

    @Test
    public void historyManyMailNoAttachNoLinks() throws Exception {
        mailCountIn(YET_ANOTHER_MSG_SUBJECT_WITH_LINK_AND_ATT, greaterThanOrEqualTo(10));
    }

    @Test
    public void linksManyMailNoAttachNoLinks() throws Exception {
        logger.warn("[SAAS-1433]");
        linksCountIn(YET_ANOTHER_MSG_SUBJECT_WITH_LINK_AND_ATT, greaterThanOrEqualTo(10));
    }

    @Test
    public void attachesManyMailWithAttachOrLinks() throws Exception {
        attachesCountIn(YET_ANOTHER_MESSAGE_SUBJECT, greaterThanOrEqualTo(10));
    }

    @Test
    public void historyManyMailWithAttachORLinks() throws Exception {
        mailCountIn(YET_ANOTHER_MESSAGE_SUBJECT, greaterThanOrEqualTo(10));
    }

    @Test
    public void linksManyMailNoWithAttachORLinks() throws Exception {
        logger.warn("[SAAS-1433]");
        linksCountIn(YET_ANOTHER_MESSAGE_SUBJECT, greaterThanOrEqualTo(10));
    }


    private void attachesCountIn(String subject, Matcher<Integer> asExpected) throws IOException {
        int result = searchMessage(subject, history(HISTORY_ATTACHMENTS)).getNumberOfAttaches();
        assertThat("Неверное количество аттачей [SAAS-1037]", result, asExpected);
    }

    private void mailCountIn(String subject, Matcher<Integer> asExpected) throws IOException {
        int result = searchMessage(subject, history(HISTORY_MAIL)).getNumberOfMsgs();
        assertThat("Неверное количество писем", result, asExpected);
    }

    private void linksCountIn(String subject, Matcher<Integer> asExpected) throws IOException {
        int result = searchMessage(subject, history(HISTORY_LINKS)).getNumberOfLinks();
        assertThat("Неверное количество ссылок", result, asExpected);
    }

    private Search searchMessage(String subject, SearchObj whatSearch) throws IOException {
        logger.info("Проверяем сообщение с темой: '" + subject + "'");
        String mid = getFirstMidAllPagesInFolder(subject, folderList.defaultFID());
        String fromHeader = jsx(MessageHeader.class)
                .params(MessageObj.getMsg(mid)).post().via(hc)
                .as(Message.class).getMessageFromHeader();

        return jsx(Search.class)
                .params(whatSearch.addMidRequest(mid, fromHeader))
                .post().via(hc);
    }

}
