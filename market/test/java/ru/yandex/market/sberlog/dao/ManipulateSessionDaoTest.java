package ru.yandex.market.sberlog.dao;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.sberlog.SberlogConfig;
import ru.yandex.market.sberlog.SberlogTest;
import ru.yandex.market.sberlog.controller.model.Links;
import ru.yandex.market.sberlog.controller.model.Marketid;
import ru.yandex.market.sberlog.dao.model.MarketidLinksModel;
import ru.yandex.market.sberlog.dao.model.StatusModel;
import ru.yandex.market.sberlog.dao.model.UserInfoModel;

import java.util.List;
import java.util.Random;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 13.04.19
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SberlogConfig.class})
public class ManipulateSessionDaoTest {

    @Autowired
    private ManipulateSessionDao manipulateSessionDao;

    @Test
    public void checkAndGetUserInfo() {

        UserInfoModel expectedUserInfoModel1 = SberlogTest.getUserInfoModel();
        expectedUserInfoModel1.setMarketid(SberlogTest.getNextMarketidCounter());
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        UserInfoModel userInfoModel1 = manipulateSessionDao.checkAndGetUserInfo(SberlogTest.getMarketidCounter(),
                SberlogTest.getSession(), SberlogTest.getR());
        Assert.assertEquals(expectedUserInfoModel1.getMarketid(), userInfoModel1.getMarketid());


        String postBodyTest1 = "{\"firstname\": \"Василий\", \"lastname\": \"Пупкин\"," +
                " \"sex\": \"1\", \"phones\": [\"81233213344\"]," +
                " \"emails\": [\"user@ya.ru\"]," +
                " \"deliveryaddrs\": [[\"Дом\",\"Россия г. Москва ул. Строителей 1, подъезд 1, кв. 1\"]," +
                " ]}";
        UserInfoModel expectedUserInfoModel2 = SberlogTest.getUserInfoModel(postBodyTest1);
        expectedUserInfoModel2.setMarketid(SberlogTest.getNextMarketidCounter());
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(postBodyTest1),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        UserInfoModel userInfoModel2 = manipulateSessionDao.checkAndGetUserInfo(SberlogTest.getMarketidCounter(),
                SberlogTest.getSession(), SberlogTest.getR());
        Assert.assertEquals(expectedUserInfoModel2.getMarketid(), userInfoModel2.getMarketid());
        Assert.assertEquals(expectedUserInfoModel2.getFirstname(), userInfoModel2.getFirstname());
        Assert.assertArrayEquals(expectedUserInfoModel2.getPhones(), userInfoModel2.getPhones());
        Assert.assertNull(userInfoModel2.getFathername());

        UserInfoModel NotFoundUserInfoModel = manipulateSessionDao.checkAndGetUserInfo("-1",
                SberlogTest.getSession(), SberlogTest.getR());
        Assert.assertEquals(2, NotFoundUserInfoModel.getStatus().getCode());
    }

    @Test
    public void getUsersInfo() {
        StringBuilder marketidSB = new StringBuilder();
        int count = 3;

        for (int i = 0; i < count; i++) {
            marketidSB.append(SberlogTest.getNextMarketidCounter()).append(",");
            manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                    SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        }

        List<UserInfoModel> userInfoModelList1 = manipulateSessionDao.getUsersInfo(marketidSB.toString());
        Assert.assertEquals(count, userInfoModelList1.size());
        Assert.assertEquals(0, userInfoModelList1.get(1).getStatus().getCode());

        //fake user
        String fakeUser = "0";
        List<UserInfoModel> userInfoModelList2 = manipulateSessionDao.getUsersInfo(fakeUser);
        Assert.assertEquals(1, userInfoModelList2.get(0).getStatus().getCode());
    }

    @Test
    public void updateUserInfo() {
        String newName = "Пафнутий";
        String marketid = SberlogTest.getNextMarketidCounter();
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        UserInfoModel updateUserInfoModel = SberlogTest.getUserInfoModel();
        updateUserInfoModel.setFirstname(newName);
        Assert.assertTrue(manipulateSessionDao.updateUserInfo(marketid, updateUserInfoModel));
        List<UserInfoModel> userInfoModelList = manipulateSessionDao.getUsersInfo(marketid);
        Assert.assertEquals(marketid, userInfoModelList.get(0).getMarketid());
        Assert.assertEquals(newName, userInfoModelList.get(0).getFirstname());
    }

    @Test
    public void createUserAndGetMarketidWithoutSession() {
        SberlogTest.getNextSberidForTest();
        Marketid marketid = manipulateSessionDao.getMarketid(SberlogTest.getSberidForTest());
        Assert.assertNull(marketid.getMarketid());

        SberlogTest.getNextMarketidCounter();
        UserInfoModel userInfoModel = manipulateSessionDao.createUserAndGetMarketidWithoutSession(
                SberlogTest.getSberidForTest());
        marketid = manipulateSessionDao.getMarketid(SberlogTest.getSberidForTest());
        Assert.assertEquals(SberlogTest.getMarketidCounter(), marketid.getMarketid());
        Assert.assertNull(userInfoModel.getFirstname());

        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());

        userInfoModel = manipulateSessionDao.createUserAndGetMarketidWithoutSession(
                SberlogTest.getSberidForTest());
        marketid = manipulateSessionDao.getMarketid(SberlogTest.getSberidForTest());
        Assert.assertEquals(SberlogTest.getMarketidCounter(), marketid.getMarketid());
        Assert.assertEquals("Василий", userInfoModel.getFirstname());
    }

    @Test
    public void createAlreadyExistUserAndGetMarketid() {
        Random r = new Random();

        Assert.assertEquals(SberlogTest.getMarketidCounter(),
                manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                        SberlogTest.getSberidForTest(), SberlogTest.getSession(),
                        String.valueOf(r.nextInt())));
    }

    @Test
    public void createUserAndGetMarketid() {
        Assert.assertEquals(SberlogTest.getNextMarketidCounter(),
                manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                        SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR()));
    }

    @Test
    public void getMarketid() {
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        SberlogTest.getNextMarketidCounter();
        Assert.assertEquals(SberlogTest.getMarketidCounter(),
                manipulateSessionDao.getMarketid(SberlogTest.getSberidForTest()).getMarketid());

    }

    @Test
    public void getSberid() {
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        SberlogTest.getNextMarketidCounter();
        Assert.assertEquals(SberlogTest.getSberidForTest(),
                manipulateSessionDao.getSberid(SberlogTest.getMarketidCounter()).getSberid());
    }

    @Test
    public void deleteUser() {
        String marketid = SberlogTest.getNextMarketidCounter();
        String fakeMarketid = "0";
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        Assert.assertTrue(manipulateSessionDao.deleteUser(marketid, true));
        Assert.assertFalse(manipulateSessionDao.deleteUser(fakeMarketid, true));
    }

    @Test
    public void deleteUserSession() {
        String marketid = SberlogTest.getNextMarketidCounter();
        String fakeMarketid = "0";

        UserInfoModel userInfo = SberlogTest.getUserInfoModel();
        String sberid = SberlogTest.getNextSberidForTest();
        String session = SberlogTest.getSession();
        String rnd = SberlogTest.getR();

        manipulateSessionDao.createUserAndGetMarketid(userInfo, sberid, session, rnd);
        Assert.assertTrue(manipulateSessionDao.deleteUserSession(marketid, session, rnd));
        Assert.assertFalse(manipulateSessionDao.deleteUserSession(fakeMarketid, session, rnd));
    }

    @Test
    public void createLinkAccountAndGetPuid() {
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        SberlogTest.getNextMarketidCounter();

        MarketidLinksModel marketidLinksModel = manipulateSessionDao.getPuidLink(SberlogTest.getNextPuidForTest());
        Assert.assertEquals("", marketidLinksModel.getMarketid());

        Links links = new Links(SberlogTest.getMarketidCounter(), SberlogTest.getPuidForTest());
        StatusModel statusModel = manipulateSessionDao.createLinkAccount(links);
        Assert.assertEquals(0, statusModel.getCode());

        marketidLinksModel = manipulateSessionDao.getPuidLink(SberlogTest.getPuidForTest());
        Assert.assertEquals(SberlogTest.getMarketidCounter(), marketidLinksModel.getMarketid());

        statusModel = manipulateSessionDao.createLinkAccount(links);
        Assert.assertEquals(400, statusModel.getCode());

        Links badLinks = new Links(null, null);
        statusModel = manipulateSessionDao.createLinkAccount(badLinks);
        Assert.assertEquals(404, statusModel.getCode());

        badLinks = new Links("fakemarketid", "fakepuid");
        statusModel = manipulateSessionDao.createLinkAccount(badLinks);
        Assert.assertEquals(404, statusModel.getCode());
    }

    @Test
    public void deleteUserLinkByAnyUid() {
        manipulateSessionDao.createUserAndGetMarketid(SberlogTest.getUserInfoModel(),
                SberlogTest.getNextSberidForTest(), SberlogTest.getSession(), SberlogTest.getR());
        SberlogTest.getNextMarketidCounter();

        MarketidLinksModel marketidLinksModel = manipulateSessionDao.getPuidLink(SberlogTest.getNextPuidForTest());
        Assert.assertEquals("", marketidLinksModel.getMarketid());

        Links links = new Links(SberlogTest.getMarketidCounter(), SberlogTest.getPuidForTest());
        StatusModel statusModel = manipulateSessionDao.createLinkAccount(links);
        Assert.assertEquals(0, statusModel.getCode());

        marketidLinksModel = manipulateSessionDao.getPuidLink(SberlogTest.getPuidForTest());
        Assert.assertEquals(SberlogTest.getMarketidCounter(), marketidLinksModel.getMarketid());

        Assert.assertTrue(manipulateSessionDao.deleteUserLinkByAnyUid(SberlogTest.getPuidForTest()));

        marketidLinksModel = manipulateSessionDao.getPuidLink(SberlogTest.getPuidForTest());
        Assert.assertEquals("", marketidLinksModel.getMarketid());
    }

    @Test
    public void getVersion() {
        Assert.assertTrue(manipulateSessionDao.getVersion());
    }
}
