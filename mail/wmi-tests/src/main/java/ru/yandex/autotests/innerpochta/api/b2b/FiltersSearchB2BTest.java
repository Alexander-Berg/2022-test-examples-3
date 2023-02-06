package ru.yandex.autotests.innerpochta.api.b2b;

import org.apache.http.message.BasicHeader;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.onlyapi.FilterSearch;
import ru.yandex.autotests.innerpochta.wmi.mailsend.InsertSystemMessageTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.10.15
 * Time: 15:07
 */
@Aqua.Test
@Title("[API] Внутреняя ручка filter_search")
@Description("Сравниваем с продакшеном выдачу ручку filter_search")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.SEARCH)
@Credentials(loginGroup = "ApiFilterSearchTest")
public class FiltersSearchB2BTest extends BaseTest {

    public static final int COUNT = 2;

    @Test
    public void b2bFilterSearchTest() throws Exception {
        String mid = sendWith.viaProd().waitDeliver().send().getMid();

        FilterSearch filterSearch = api(FilterSearch.class)
                .headers(new BasicHeader(InsertSystemMessageTest.X_REAL_IP, InsertSystemMessageTest.IP_HOME))
                .setHost(props().on8079Host())
                .params(FilterSearchObj.empty().setUid(composeCheck.getUid())
                        .setMids(mid));

        FilterSearch respNew = filterSearch.get().via(hc).withDebugPrint();
        FilterSearch respBase = filterSearch.setHost(props().on8079Host(props().b2bUri().toString())).get().via(hc);

        assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }

    @Test
    public void b2bFilterSearchWithMidsTest() throws Exception {
        List<String> mids = sendWith.viaProd().waitDeliver().count(COUNT).send().getMids();

        FilterSearch filterSearch = api(FilterSearch.class)
                .headers(new BasicHeader(InsertSystemMessageTest.X_REAL_IP, InsertSystemMessageTest.IP_HOME))
                .setHost(props().on8079Host())
                .params(FilterSearchObj.empty().setUid(composeCheck.getUid())
                        .setMids(mids.toArray(new String[mids.size()])));

        FilterSearch respNew = filterSearch.get().via(hc);
        FilterSearch respBase = filterSearch.setHost(props().on8079Host(props().b2bUri().toString())).get().via(hc);

        assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }


    @Test
    public void b2bFilterSearchHistoryTest() throws Exception {
        String mid = sendWith.viaProd().waitDeliver().send().getMid();

        FilterSearch filterSearch = api(FilterSearch.class)
                .headers(new BasicHeader(InsertSystemMessageTest.X_REAL_IP, InsertSystemMessageTest.IP_HOME))
                .setHost(props().on8079Host())
                .params(FilterSearchObj.empty().setMids(mid).setUid(composeCheck.getUid()).history());

        FilterSearch respNew = filterSearch.get().via(hc);
        FilterSearch respBase = filterSearch.setHost(props().on8079Host(props().b2bUri().toString())).get().via(hc);

        assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));
    }
}
