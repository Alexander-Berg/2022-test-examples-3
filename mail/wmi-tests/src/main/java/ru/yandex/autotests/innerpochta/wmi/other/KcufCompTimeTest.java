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
import ru.yandex.autotests.innerpochta.wmi.core.obj.DariaMessagesObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.DariaMessages;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetCountry;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetUserParameters;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;


@Aqua.Test
@Title("Проверка сетей конкурентов")
@Description("Проверка, что работает обновление времени после захода из сетей конкурентов [DARIA-16069]")
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "KcufCompTimeTest")
public class KcufCompTimeTest extends BaseTest {

    public static final String IP_FOREIGN_NET = "5.61.232.1";
    public static final String IP_CHINA = "203.17.39.0";

    public static final String X_REAL_IP_HEADER = "X-Real-IP";
    public static final String HOST_HEADER = "Host";

    public static final String IP_CRIMEA = "109.200.128.255";

    public static final String REGION_PARENTS_FOR_RU_CRIMEA = "146,121220,977,115092,225,10001,10000";
    public static final String REGION_PARENTS_FOR_UA_CRIMEA = "146,121220,977,187,166,10001,10000";

    public static final String REGION_PARENTS_FOR_IP_FOREIGN_NET = "213,1,3,225,10001,10000";
    public static final String REGION_PARENTS_FOR_CHINA_IP = "20925,114557,134,183,10001,10000";

    private GetUserParameters operWithNoHeaders;
    private GetUserParameters operForeignNet;
    private DariaMessages dariaMessages;
    private DariaMessages dariaMessagesForeignNet;
    public static final String HOST = UrlBuilder.fromString(props().betaHost())
            .withScheme("http").withPort(8079).toString();

    /**
     * Этот метод вызывается в @Before
     */
    @Before
    public void prepare() throws Exception {
        operWithNoHeaders = jsx(GetUserParameters.class)
                .setHost(HOST);

        operForeignNet = jsx(GetUserParameters.class)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_FOREIGN_NET))
                .setHost(HOST);

        dariaMessagesForeignNet = jsx(DariaMessages.class)
                .setHost(HOST)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_FOREIGN_NET))
                .params(DariaMessagesObj.getObjCurrFolder(folderList.defaultFID()));

        dariaMessages = jsx(DariaMessages.class)
                .params(DariaMessagesObj.getObjCurrFolder(folderList.defaultFID()));
    }

    @Test
    @Description("Работа изменения времени захода из сетей конкурентов\n" +
            "[DARIA-16069]\n" +
            "[WMI-591]\n" +
            "[DARIA-34930]")
    @Issues({@Issue("DARIA-16069"), @Issue("WMI-591"), @Issue("DARIA-34930")})
    public void foreignCompNets() throws Exception {
        GetUserParameters respFirstNotForeign = operWithNoHeaders.post().via(hc);

        dariaMessages.post().via(hc);

        Thread.sleep(1000);
        GetUserParameters respSecondNotForeign = operWithNoHeaders.post().via(hc);


        assertEquals("Время захода меняется самопроизвольно",
                respFirstNotForeign.getKcufComp(), respSecondNotForeign.getKcufComp());

        Thread.sleep(1000);
        logger.info("Заходим из сетки конкурента: " + IP_FOREIGN_NET);

        //дергаем daria_messages [DARIA-34930], время должно поменяться
        dariaMessagesForeignNet.post().via(hc);

        GetUserParameters respFirstForeign = operForeignNet.post().via(hc);
        assertThat("Ответ по-умолчанию не содержит вражий IP!",
                respSecondNotForeign.getKcufComp(), endsWith(IP_FOREIGN_NET));
        assertThat("Ответ не содержит вражий IP!", respFirstForeign.getKcufComp(), endsWith(IP_FOREIGN_NET));
        assertThat("После захода из чужой сети, время не изменилось",
                respSecondNotForeign.getKcufComp(), not(equalTo(respFirstForeign.getKcufComp())));

        //не дергаем daria_message
        GetUserParameters respForeignWithoutDariaMessage = operForeignNet.post().via(hc);
        assertThat("Ответ по-умолчанию не содержит вражий IP!",
                respSecondNotForeign.getKcufComp(), endsWith(IP_FOREIGN_NET));
        assertThat("После захода из чужой сети и не открытии списка писем, время изменилось [DARIA-34930]",
                respFirstForeign.getKcufComp(), equalTo(respForeignWithoutDariaMessage.getKcufComp()));

        Thread.sleep(1000);
        GetUserParameters respThirdNotForeign = operWithNoHeaders.post().via(hc);

        assertEquals("Время захода меняется самопроизвольно, после однократного захода из чужой сети",
                respFirstForeign.getKcufComp(), respThirdNotForeign.getKcufComp());
    }

    @Test
    @Issues({@Issue("DARIA-18292"), @Issue("DARIA-33421")})
    @Description("[DARIA-18292]\n" +
            "[DARIA-33421]\n" +
            "При обращении вроде:\n" +
            "curl -v -H 'X-Real-IP: 81.19.64.35'\n" +
            "-H 'Host: mail.yandex.ru' 'http://127.0.0.1:8079/host-root2/index.jsx'\n" +
            "Единственное что возвращалось, так это Exception: и все.\n" +
            "Корень зла именно в этой ручке\n" +
            "Айпишник должен был быть в запросе из вражеской сетки")
    public void getCountryWithForeignNetsIp() throws IOException {
        logger.warn("[DARIA-18292]");
        jsx(GetCountry.class)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_FOREIGN_NET))
                .setHost(HOST).post().via(hc).shouldBe().haveCountry(equalTo("ru"))
                .haveRegionParents(equalTo(REGION_PARENTS_FOR_IP_FOREIGN_NET));
    }

    @Test
    @Issue("DARIA-33421")
    @Description("Проверяем страну и номера областей\n" +
            "для Китайского айпишника\n" +
            "[DARIA-33421]")
    public void testRegionParentsWithChinaIp() throws IOException {
        logger.warn("[DARIA-33421]");
        jsx(GetCountry.class)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_CHINA))
                .setHost(HOST).post().via(hc)
                .shouldBe().haveCountry(equalTo("cn"))
                .haveRegionParents(equalTo(REGION_PARENTS_FOR_CHINA_IP));
    }

    @Description("Крым - Россия. После введения новой геобазы для ru - домена")
    @Test
    public void testRegionParentsWithCrimeaIp() throws IOException {
        jsx(GetCountry.class)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_CRIMEA))
                .setHost(HOST).post().via(hc)
                .shouldBe().haveCountry(equalTo("ru"))
                .haveRegionParents(equalTo(REGION_PARENTS_FOR_RU_CRIMEA));
    }

    @Description("Крым - Украина. После введения новой геобазы для ua - домена")
    @Test
    public void testRegionParentsWithCrimeaIpUa() throws IOException {
        jsx(GetCountry.class)
                .headers(new BasicHeader(X_REAL_IP_HEADER, IP_CRIMEA), new BasicHeader(HOST_HEADER, "mail.yandex.ua"))
                .setHost(HOST).post().via(hc)
                .shouldBe().haveCountry(equalTo("ua"))
                .haveRegionParents(equalTo(REGION_PARENTS_FOR_UA_CRIMEA));
    }
}
