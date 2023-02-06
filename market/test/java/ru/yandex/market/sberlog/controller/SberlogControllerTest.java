package ru.yandex.market.sberlog.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.sberlog.SberlogConfig;
import ru.yandex.market.sberlog.SberlogTest;
import ru.yandex.market.sberlog.cache.LocalCacheService;
import ru.yandex.market.sberlog.cipher.CipherService;
import ru.yandex.market.sberlog.config.LocalCacheConfig;
import ru.yandex.market.sberlog.controller.model.DeleteUserDataFlag;
import ru.yandex.market.sberlog.controller.model.MarketSessionId;
import ru.yandex.market.sberlog.dao.ManipulateSessionDao;
import ru.yandex.market.sberlog.dao.model.MarketidLinksModel;
import ru.yandex.market.sberlog.dao.model.StatusModel;
import ru.yandex.market.sberlog.dao.model.UserInfoModel;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 13.04.19
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SberlogConfig.class})
public class SberlogControllerTest {
    private static final Gson GSON = new GsonBuilder().create();

    @Autowired
    private ManipulateSessionDao manipulateSessionDao;

    @Autowired
    private CipherService cipherService;

    @Autowired
    private EmbeddedPostgres embeddedPostgres;

    private LocalCacheService localCacheService;

    private SberlogController sberlogController;

    @Before
    public void setUp() {
        LocalCacheConfig localCacheConfig = new LocalCacheConfig();
        localCacheService = new LocalCacheService(embeddedPostgres.getPostgresDatabase(), localCacheConfig);
        sberlogController = new SberlogController(manipulateSessionDao, cipherService, localCacheService);
    }

    @After
    public void cleanUp() throws IOException {
        localCacheService.close();
    }

    @Test
    public void checkOauthMarketSessionId() {
        SberlogTest.getNextMarketidCounter();

        ResponseEntity<?> cookieResponce = sberlogController.createUserAndReturnCookieValue(
                SberlogTest.getNextSberidForTest(),
                SberlogTest.getClientIp(),
                SberlogTest.getPostBodyForTest());

        Assert.assertEquals(200, cookieResponce.getStatusCode().value());

        ResponseEntity<?> userInfoModelResponce = sberlogController.checkOauthMarketSessionId(
                String.valueOf(cookieResponce.getBody()));
        String postBody = String.valueOf(userInfoModelResponce.getBody()).substring(1,
                String.valueOf(userInfoModelResponce.getBody()).length() - 1);
        UserInfoModel userInfoModel = GSON.fromJson(postBody,
                UserInfoModel.class);

        Assert.assertEquals(SberlogTest.getMarketidCounter(), userInfoModel.getMarketid());
        Assert.assertEquals("Василий", userInfoModel.getFirstname());

        ManipulateSessionDao manipulateSessionDaoMock = Mockito.mock(ManipulateSessionDao.class);
        Mockito.when(manipulateSessionDaoMock.checkAndGetUserInfo(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new UserInfoModel(new StatusModel(2, "not found")));
        CipherService cipherServiceWithMock = Mockito.mock(CipherService.class);
        Mockito.when(cipherServiceWithMock.checkSign(Mockito.any(), Mockito.any())).thenReturn(true);

        SberlogController sberlogControllerWithMock = new SberlogController(manipulateSessionDaoMock,
                cipherServiceWithMock, localCacheService);

        ResponseEntity<?> responseEntityWithMock
                = sberlogControllerWithMock.checkOauthMarketSessionId(SberlogTest.getCookie());
        Assert.assertEquals(404, responseEntityWithMock.getStatusCode().value());
    }

    @Test
    public void checkCookieException() {
        String badCookie = "{\"market_session_id\": \"MTU1NDkyODQ0MTYwNDoxMjcuMC4wOmxvY2FsaG9zdF9kd2s%3D%3A1228" +
                "%3A1554928441604%3A1554928441604%3A1547958568%3A9a7cf620017cd2ebeda0bffc3e1ce688d769c120b9eadf34f" +
                "a3e59463e0b3a6d5c09502d10c3135548a1d303874b10ce1f59da55e684c2aa1ac1c1cbc4de765c7c6064b6b9ba853c91" +
                "da3ac7879a3f49a20394936ee81f2a39cd331ef234dc5e64802d2499c5532b9fa8b90837f3a2ee8ef1dee4494ff57d" +
                "c0ea6fbd3dfbb700\"}";

        ResponseEntity<?> userInfoModelResponce = sberlogController.checkOauthMarketSessionId(badCookie);
        Assert.assertEquals(400, userInfoModelResponce.getStatusCode().value());
    }

    @Test
    public void createUserAndReturnCookieValue() {
        SberlogTest.getNextMarketidCounter();

        ResponseEntity<?> cookieResponce = sberlogController.createUserAndReturnCookieValue(
                SberlogTest.getNextSberidForTest(),
                SberlogTest.getClientIp(),
                SberlogTest.getPostBodyForTest());

        Assert.assertEquals(200, cookieResponce.getStatusCode().value());
        MarketSessionId cookie = GSON.fromJson(String.valueOf(cookieResponce.getBody()),
                MarketSessionId.class);

        String[] cookieArr = URLDecoder.decode(cookie.getMarketSessionId(), StandardCharsets.UTF_8).split(":");
        Assert.assertEquals(SberlogTest.getMarketidCounter(), cookieArr[3]);

        String badPostBody = "\"abra\": \"Василий\", \"fathername\": \"Васильевич\", \"lastname\": \"Пупкин\"," +
                " \"sex\": \"1\", \"birthday\": \"2009-12-30\", \"phones\": [\"81233213344\", \"+79095612567\"]," +
                " \"emails\": [\"user@yandex.ru\", \"user@ya.ru\"]," +
                " \"deliveryaddrs\": [[\"Дом\",\"Россия г. Москва ул. Строителей 1, подъезд 1, кв. 1\"]," +
                " [\"Работа\",\"Россия г. Москва ул. Льва Толстого 16\"]]";
        ResponseEntity<?> badCookieResponce = sberlogController.createUserAndReturnCookieValue(
                SberlogTest.getSberidForTest(),
                SberlogTest.getClientIp(),
                badPostBody);
        Assert.assertEquals(400, badCookieResponce.getStatusCode().value());

        badPostBody = "{\"firstname\": \"Василий\", \"fathername\": \"Васильевич\", \"lastname\": \"Пупкин\"," +
                " \"sex\": \"1\", \"birthday\": \"20091230\", \"phones\": [\"81233213344\", \"+79095612567\"]," +
                " \"emails\": [\"user@yandex.ru\", \"user@ya.ru\"]," +
                " \"deliveryaddrs\": [[\"Дом\",\"Россия г. Москва ул. Строителей 1, подъезд 1, кв. 1\"]," +
                " [\"Работа\",\"Россия г. Москва ул. Льва Толстого 16\"]]}";
        badCookieResponce = sberlogController.createUserAndReturnCookieValue(
                SberlogTest.getSberidForTest(),
                SberlogTest.getClientIp(),
                badPostBody);
        Assert.assertEquals(400, badCookieResponce.getStatusCode().value());
    }

    @Test
    public void createOrGetMarketidBySberid() {
        SberlogTest.getNextMarketidCounter();

        ResponseEntity<?> sberidResponce2 = sberlogController.returnMarketId(SberlogTest.getNextSberidForTest());
        Assert.assertEquals(404, sberidResponce2.getStatusCode().value());

        ResponseEntity<?> userInfoModelResponce = sberlogController.createOrGetMarketidBySberid(
                SberlogTest.getSberidForTest());
        Assert.assertEquals(200, userInfoModelResponce.getStatusCode().value());

        sberidResponce2 = sberlogController.returnMarketId(SberlogTest.getSberidForTest());
        Assert.assertEquals(200, sberidResponce2.getStatusCode().value());


        ResponseEntity<?> cookieResponce = sberlogController.createUserAndReturnCookieValue(
                SberlogTest.getSberidForTest(),
                SberlogTest.getClientIp(),
                SberlogTest.getPostBodyForTest());

        Assert.assertEquals(200, cookieResponce.getStatusCode().value());

        userInfoModelResponce = sberlogController.createOrGetMarketidBySberid(
                SberlogTest.getSberidForTest());
        Assert.assertEquals(200, userInfoModelResponce.getStatusCode().value());

        UserInfoModel userInfoModel = GSON.fromJson(String.valueOf(userInfoModelResponce.getBody()),
                UserInfoModel.class);
        Assert.assertEquals("Василий", userInfoModel.getFirstname());
    }

    @Test
    public void returnUsersInfo() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        ResponseEntity<?> userInfoModelResponce = sberlogController.returnUsersInfo(SberlogTest.getMarketidCounter());

        Assert.assertEquals(200, userInfoModelResponce.getStatusCode().value());

        String postBody = String.valueOf(userInfoModelResponce.getBody()).substring(1,
                String.valueOf(userInfoModelResponce.getBody()).length() - 1);
        UserInfoModel userInfoModel = GSON.fromJson(postBody,
                UserInfoModel.class);

        Assert.assertEquals("Василий", userInfoModel.getFirstname());
    }

    @Test
    public void returnSberId() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        ResponseEntity<?> sberidResponce = sberlogController.returnSberId(SberlogTest.getMarketidCounter());
        Assert.assertEquals(200, sberidResponce.getStatusCode().value());


        ResponseEntity<?> sberidResponce2 = sberlogController.returnSberId(SberlogTest.getNextMarketidCounter());
        Assert.assertEquals(404, sberidResponce2.getStatusCode().value());
        SberlogTest.setPrevMarketidCounter();
    }

    @Test
    public void returnMarketId() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        ResponseEntity<?> marketidResponce = sberlogController.returnMarketId(SberlogTest.getSberidForTest());
        Assert.assertEquals(200, marketidResponce.getStatusCode().value());

        ResponseEntity<?> marketidResponce2 = sberlogController.returnMarketId(SberlogTest.getNextSberidForTest());
        Assert.assertEquals(404, marketidResponce2.getStatusCode().value());
        SberlogTest.setPrevSberidCounter();
    }

    @Test
    public void checkAndDeleteUserSession() {
        SberlogTest.getNextMarketidCounter();

        ResponseEntity<?> cookieResponce = sberlogController.createUserAndReturnCookieValue(
                SberlogTest.getNextSberidForTest(),
                SberlogTest.getClientIp(),
                SberlogTest.getPostBodyForTest());

        Assert.assertEquals(200, cookieResponce.getStatusCode().value());

        ResponseEntity<?> deleteUserSessionResponce1 = sberlogController.deleteUserSession(
                String.valueOf(
                        cookieResponce.getBody()
                )
        );
        Assert.assertEquals("{\"status\": {\"code\":0,\"text\":\"deleted\"}}", deleteUserSessionResponce1.getBody());

        ResponseEntity<?> deleteUserSessionResponce2 = sberlogController.deleteUserSession(
                String.valueOf(
                        cookieResponce.getBody()
                ).substring(2)
        );
        Assert.assertEquals(400, deleteUserSessionResponce2.getStatusCode().value());

        manipulateSessionDao.deleteUser(SberlogTest.getMarketidCounter(), false);

        ResponseEntity<?> deleteUserSessionResponce3 = sberlogController.deleteUserSession(
                String.valueOf(
                        cookieResponce.getBody()
                )
        );
        Assert.assertEquals("{\"status\": {\"code\":1,\"text\":\"not found\"}}", deleteUserSessionResponce3.getBody());
    }

    @Test
    public void deleteUser() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        DeleteUserDataFlag deleteUserDataFlag = new DeleteUserDataFlag();
        deleteUserDataFlag.setDeleteUserDataFlag(true);
        ResponseEntity<?> deleteResponce1 = sberlogController.deleteUser(SberlogTest.getMarketidCounter(),
                GSON.toJson(deleteUserDataFlag, DeleteUserDataFlag.class));
        Assert.assertEquals(200, deleteResponce1.getStatusCode().value());

        String fakeUser = "0";
        ResponseEntity<?> deleteResponce2 = sberlogController.deleteUser(fakeUser,
                GSON.toJson(deleteUserDataFlag, DeleteUserDataFlag.class));
        Assert.assertEquals(404, deleteResponce2.getStatusCode().value());
    }

    @Test
    public void updateUserInfo() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        String expectedName = "Фёкла";

        UserInfoModel userInfoModel = SberlogTest.getUserInfoModel();
        userInfoModel.setFirstname(expectedName);
        ResponseEntity<?> updateResponce = sberlogController.updateUserInfo(SberlogTest.getMarketidCounter(),
                GSON.toJson(userInfoModel, UserInfoModel.class));

        Assert.assertEquals(201, updateResponce.getStatusCode().value());

        List<UserInfoModel> userInfoModelList = manipulateSessionDao.getUsersInfo(SberlogTest.getMarketidCounter());
        Assert.assertEquals(1, userInfoModelList.size());
        Assert.assertEquals(expectedName, userInfoModelList.get(0).getFirstname());

        ManipulateSessionDao manipulateSessionDaoMock = Mockito.mock(ManipulateSessionDao.class);
        Mockito.when(manipulateSessionDaoMock.updateUserInfo(Mockito.any(), Mockito.any())).thenReturn(false);

        SberlogController sberlogControllerWithMock =
                new SberlogController(manipulateSessionDaoMock, cipherService, localCacheService);

        ResponseEntity<?> updateResponceWithMock =
                sberlogControllerWithMock.updateUserInfo(SberlogTest.getMarketidCounter(),
                        GSON.toJson(userInfoModel, UserInfoModel.class));

        Assert.assertEquals(404, updateResponceWithMock.getStatusCode().value());
    }

    @Test
    public void linkAccount() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        ResponseEntity<?> linkAccountResponce = sberlogController.linkAccount(
                SberlogTest.getLink(SberlogTest.getMarketidCounter(),
                        SberlogTest.getNextPuidForTest())
        );
        Assert.assertEquals(200, linkAccountResponce.getStatusCode().value());


        ResponseEntity<?> badLinkAccountResponce = sberlogController.linkAccount(
                SberlogTest.getLink(SberlogTest.getMarketidCounter(),
                        SberlogTest.getPuidForTest())
        );
        Assert.assertEquals(400, badLinkAccountResponce.getStatusCode().value());

        badLinkAccountResponce = sberlogController.linkAccount(
                SberlogTest.getLink("fakemarketid",
                        SberlogTest.getPuidForTest())
        );
        Assert.assertEquals(404, badLinkAccountResponce.getStatusCode().value());
    }

    @Test
    public void deleteLinkAccountByAnyUid() {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        ResponseEntity<?> linkAccountResponce = sberlogController.linkAccount(
                SberlogTest.getLink(SberlogTest.getMarketidCounter(), SberlogTest.getPuidForTest())
        );
        Assert.assertEquals(200, linkAccountResponce.getStatusCode().value());

        //check by marketid
        ResponseEntity<?> deleteLinkAccountResponce = sberlogController.deleteLinkAccountByAnyUid(
                SberlogTest.getMarketidCounter()
        );
        Assert.assertEquals(200, deleteLinkAccountResponce.getStatusCode().value());

        //check by puid
        linkAccountResponce = sberlogController.linkAccount(
                SberlogTest.getLink(SberlogTest.getMarketidCounter(), SberlogTest.getPuidForTest())
        );
        Assert.assertEquals(200, linkAccountResponce.getStatusCode().value());

        deleteLinkAccountResponce = sberlogController.deleteLinkAccountByAnyUid(
                SberlogTest.getPuidForTest()
        );
        Assert.assertEquals(200, deleteLinkAccountResponce.getStatusCode().value());

        //check for 404
        deleteLinkAccountResponce = sberlogController.deleteLinkAccountByAnyUid(
                SberlogTest.getPuidForTest()
        );
        Assert.assertEquals(404, deleteLinkAccountResponce.getStatusCode().value());

    }

    @Test
    public void returnIsLinked() throws ExecutionException {
        SberlogTest.getNextMarketidCounter();

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        ResponseEntity<?> returnIsLinkedResponce = sberlogController.returnIsLinked(SberlogTest.getNextPuidForTest());
        MarketidLinksModel marketidLinksModel = GSON.fromJson(String.valueOf(returnIsLinkedResponce.getBody()),
                MarketidLinksModel.class);
        Assert.assertEquals("", marketidLinksModel.getMarketid()); //404

        ResponseEntity<?> linkAccountResponce = sberlogController.linkAccount(
                SberlogTest.getLink(SberlogTest.getMarketidCounter(), SberlogTest.getPuidForTest())
        );
        Assert.assertEquals(200, linkAccountResponce.getStatusCode().value());

//        LocalCache.isLinkedCache.invalidateAll();

        returnIsLinkedResponce = sberlogController.returnIsLinked(SberlogTest.getPuidForTest());
        marketidLinksModel = GSON.fromJson(String.valueOf(returnIsLinkedResponce.getBody()),
                MarketidLinksModel.class);
        Assert.assertEquals(SberlogTest.getPuidForTest(), marketidLinksModel.getPuid()); //200

    }
}
