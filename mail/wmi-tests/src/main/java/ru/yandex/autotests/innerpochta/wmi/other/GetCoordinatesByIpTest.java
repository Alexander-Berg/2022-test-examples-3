package ru.yandex.autotests.innerpochta.wmi.other;

import gumi.builders.UrlBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetCoordinatesByIp;
import ru.yandex.autotests.innerpochta.wmicommon.Arbiter;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.other.KcufCompTimeTest.X_REAL_IP_HEADER;


@Aqua.Test
@Title("Координаты по IP")
@Description("Получаем координаты по IP [DARIA-20706]")
@Features(MyFeatures.WMI)
@Stories(MyStories.GEOBASE)
@Credentials(loginGroup = "KcufCompTimeTest")
public class GetCoordinatesByIpTest extends BaseTest {

    public static final String IP_FOREIGN_NET = "5.61.232.1";

    private GetCoordinatesByIp operWithNoHeaders;
    private GetCoordinatesByIp operForeignNet;

    public static final String HOST = UrlBuilder.fromString(props().betaHost())
            .withScheme("http").withPort(8079).toString();

    /**
     * Этот метод вызывается в @Before
     */
    @Before
    public void prepare() throws Exception {
        operWithNoHeaders = jsx(GetCoordinatesByIp.class);
        operWithNoHeaders.setHost(HOST);

        operForeignNet = jsx(GetCoordinatesByIp.class)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_FOREIGN_NET));
        operForeignNet.setHost(HOST);
    }

    @Test
    @Description("Работа изменения времени захода из сетей конкурентов")
    @Issues({@Issue("DARIA-16069"), @Issue("WMI-591")})
    public void gettingCoordsByIpInHeaderAndParam() throws Exception {
        logger.warn("Проверка работы метода получения координат по ip [DARIA-20706]");

        GetCoordinatesByIp respWithoutAny = operWithNoHeaders.post().via(authClient.notAuthHC());

        assertThat(respWithoutAny.toDocument(), allOf(
                hasXPath("//latitude", not("")),  //широта центра региона в градусах
                hasXPath("//longitude", not("")),     //долгота центра региона в градусах
                hasXPath("//latitude_size", not("")),  //протяженность региона по широте в градусах
                hasXPath("//longitude_size", not("")), //протяженность региона по долготе в градусах
                hasXPath("//precision", not(""))   //точность в км (расстояние от центра до наиболее удаленной точки)
        ));


        GetCoordinatesByIp respWithHeader = operForeignNet.post().via(authClient.notAuthHC());
        GetCoordinatesByIp respWithParam = operWithNoHeaders
                .params(new EmptyObj().add("ip", IP_FOREIGN_NET))
                .post().via(authClient.notAuthHC());

        Arbiter.compareCommonMethodsResponse(respWithHeader.toDocument(), respWithParam.toDocument());
    }

}
