package ru.yandex.autotests.innerpochta.wmi.b2b;

import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.ApiTestingB2BData;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oauth.OAuthRequestInterceptor;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.EmptyWmiOper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj.empty;


/**
 * Unmodify
 * Добавлять модифицирующие проверки запрещено, т.к. содержит банк писем для сравнения
 */
@Aqua.Test
@Title("[B2B] Вызов API методов без атрибутов и с ключом OAuth")
@Description("Сравнение выводов")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = "ZooNew")
public class ApiWithOAuthTest extends BaseTest {

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return ApiTestingB2BData.apiHandles();
    }

    @Parameterized.Parameter
    public Oper oper;

    @Parameterized.Parameter(1)
    public Obj obj;

    @Parameterized.Parameter(2)
    public String comment;


    @Test
    @Description("Получаем токен, и обращаемся с этим токеном через неавторизованный ХТТП-Клиент.\n" +
            "После этого повторяем эту же операцию с авторизованным клиентом и сравниваем результаты")
    public void isOauthAsHeaderWorks() throws Exception {
        assumeThat(oper, not(instanceOf(EmptyWmiOper.class)));

        Obj emptyObj = empty();
        String token = OAuthRequestInterceptor.getToken(authClient.acc());
        oper.headers(new BasicHeader("Authorization", "OAuth " + token));
        oper.params(emptyObj);

        Document withOAuthHeader = oper.post().via(authClient.notAuthHC()).toDocument();
        Document withCookie = oper.noparams().noHeaders().post().via(hc).toDocument();

        assertThat(withOAuthHeader, equalToDoc(withCookie).urlcomment(oper.getClass().getSimpleName()));
    }

    @Test
    public void isOAuthAsParameterWorks() throws Exception {
        assumeThat(oper, not(instanceOf(EmptyWmiOper.class)));

        Obj emptyObj = empty();
        emptyObj.add("oauth_token", OAuthRequestInterceptor.getToken(authClient.acc()));
        oper.params(emptyObj);

        Document withOAuthWithParam = oper.post().via(authClient.notAuthHC()).toDocument();
        Document withCookie = oper.noparams().noHeaders().post().via(hc).toDocument();

        assertThat(withOAuthWithParam, equalToDoc(withCookie).urlcomment(oper.getClass().getSimpleName()));
    }
}
