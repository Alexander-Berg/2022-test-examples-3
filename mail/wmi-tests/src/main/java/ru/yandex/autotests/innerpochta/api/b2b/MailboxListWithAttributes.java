package ru.yandex.autotests.innerpochta.api.b2b;

import org.apache.commons.httpclient.NameValuePair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.pagediffer.document.filter.ExcludeXmlNodesFilter;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasXPath;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj.empty;

@Aqua.Test
@Title("[B2B] mailbox_list с параметрами")
@Description("Сравнение с продакшеном")
@RunWith(Parameterized.class)
@Features(MyFeatures.API_WMI)
@Stories({MyStories.B2B, MyStories.MESSAGES_LIST})
@Credentials(loginGroup = "Zoo")
public class MailboxListWithAttributes extends BaseTest {

    @Parameterized.Parameters
    public static Collection<Object[]> gotoParams() throws Exception {
        ArrayList<Object[]> data = new ArrayList<Object[]>();
        for (String gotoValue : new String[]{"spam", "sent", "trash", "draft", "inbox"}) {
            data.add(new Object[]{new NameValuePair("goto", gotoValue)});
        }
        for (String extraCondValue : new String[]{"only_new", "only_atta"}) {
            data.add(new Object[]{new NameValuePair("extra_cond", extraCondValue)});
        }
        for (String sortValue : new String[]{"subject", "from", "from1", "date", "date1", "size", "size1",
                "stat", "stat1", "atta", "atta1", "ord", "ord1"}) {
            data.add(new Object[]{new NameValuePair("sort", sortValue)});
        }
        for (String pageNumber : new String[]{"1", "2", "3"}) {
            data.add(new Object[]{new NameValuePair("page_number", pageNumber)});
        }
        return data;
    }

    private NameValuePair param;

    public MailboxListWithAttributes(NameValuePair param) {
        this.param = param;
    }

    @Test
    @Description("Сравниваем с продакшеном выдачу mailbox_list с различными\n" +
            "аттрибутами во всех папках.\n" +
            "В какой-то момент в 2016 году tzdata поменяла мнение,\n" +
            "когда переводили время в 2011 и 2012 году, из-за разницы\n" +
            "в таймстемпах старых писем в проде и тестинге тест падал.\n" +
            "Поэтому исключаем из сравнения ноды message c атрибутом date,\n" +
            "начинающимся с 2011 или 2012")
    public void mailboxListWithAttributes() throws Exception {
        logger.info("ATTRIBUTE: " + param.getName() + " = " + param.getValue());

        MailBoxList testing = api(MailBoxList.class)
                .params(empty().set(param.getName(), param.getValue()))
                .post().via(hc)
                .assertDocument(hasXPath("//details/@name"));

        testing.setHost(props().productionHost());
        MailBoxList base = testing.post().via(authClient.authHC());

        assertThat(testing.toDocument(), equalToDoc(base.toDocument())
        .filterWithCondition(new ExcludeXmlNodesFilter<>("//message[starts-with(@date,'2011')]"), true)
        .filterWithCondition(new ExcludeXmlNodesFilter<>("//message[starts-with(@date,'2012')]"), true));
    }
}
