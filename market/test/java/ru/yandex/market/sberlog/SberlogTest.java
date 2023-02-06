package ru.yandex.market.sberlog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.yandex.market.sberlog.dao.model.UserInfoModel;

import java.util.Base64;
import java.util.Random;


/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 22/05/2018
 */
public class SberlogTest {

    private static final Gson GSON = new GsonBuilder().create();
    private static int sberidCounter = 5;
    private static long marketidCounter = 2_190_550_858_753_009_249L;
    private static int r = new Random().nextInt();
    private static long currTime = System.currentTimeMillis();
    private static final String clientIp = "127.0.0.1";
    private static final String hostname = "localhost_dwk7";

    public static String getPostBodyForTest() {
        return "{\"firstname\": \"Василий\", \"fathername\": \"Васильевич\", \"lastname\": \"Пупкин\"," +
                " \"sex\": \"1\", \"birthday\": \"2009-12-30\", \"phones\": [\"81233213344\", \"+79095612567\"]," +
                " \"emails\": [\"user@yandex.ru\", \"user@ya.ru\"]," +
                " \"deliveryaddrs\": [[\"Дом\",\"Россия г. Москва ул. Строителей 1, подъезд 1, кв. 1\"]," +
                " [\"Работа\",\"Россия г. Москва ул. Льва Толстого 16\"]]}";
    }

    public static UserInfoModel getUserInfoModel(String postBody) {
        return GSON.fromJson(postBody, UserInfoModel.class);
    }

    public static UserInfoModel getUserInfoModel() {
        return GSON.fromJson(getPostBodyForTest(), UserInfoModel.class);
    }

    public static String getSberidForTest() {
        return "c33cc0b0ce736529d5a06bc515383fc7849f0b837f513eff3188cca912ea73006d718939e8ff0ee" + getSberidCounter();
    }

    public static String getNextSberidForTest() {
        return "c33cc0b0ce736529d5a06bc515383fc7849f0b837f513eff3188cca912ea73006d718939e8ff0ee" + getNextCounter();
    }

    public static String getNextPuidForTest() {
        return "123432178623462376" + getNextCounter();
    }

    public static String getPuidForTest() {
        return "123432178623462376" + sberidCounter;
    }

    private static int getSberidCounter() {
        return sberidCounter;
    }

    private static int getNextCounter() {
        setSberidCounter(getSberidCounter() + 1);
        return sberidCounter;
    }

    private static void setSberidCounter(int sberidCount) {
        sberidCounter = sberidCount;
    }

    public static void setPrevSberidCounter() {
        setSberidCounter(getSberidCounter() - 1);
    }

    public static String getMarketidCounter() {
        return String.valueOf(marketidCounter);
    }

    public static String getNextMarketidCounter() {
        setMarketidCounter(Long.valueOf(getMarketidCounter()) + 1);
        return String.valueOf(marketidCounter);
    }

    public static void setPrevMarketidCounter() {
        setMarketidCounter(Long.valueOf(getMarketidCounter()) - 1);
    }

    private static void setMarketidCounter(long marketidCount) {
        marketidCounter = marketidCount;
    }

    public static String getSession() {
        String sessionid = currTime + ":" + clientIp + ":" + hostname;
        return Base64.getEncoder().encodeToString(sessionid.getBytes());
    }

    public static String getR() {
        return String.valueOf(r);
    }

    public static String getClientIp() {
        return clientIp;
    }

    public static String getCookie() {
        return "{\"market_session_id\": " +
                "\"1%3A1%3AMTU1NTQxNjYwNDM2NzoxMjcuMC4wOmxvY2FsaG9zdF9kd2s%3D%3A1229%3A1555416604367" +
                "%3A1555416604367%3A1819395609%3A0c8682b6ef1caeeec2004c5e8197687322ca9e39cdcab49d3c7e38f21a8a612c\"}";
    }

    public static String getLink(String marketid, String puid) {
        return String.format("{\"system\": \"passport\", \"marketid\": \"%s\",\"puid\": \"%s\"}", marketid, puid);
    }

}