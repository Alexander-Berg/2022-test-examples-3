package ru.yandex.market.pers.notify.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 09.01.18
 */
public class PictureUtilTest extends MarketMailerMockedDbTest {

    @Autowired
    private PictureUtil pictureUtil;

    @Test
    public void testResolveCheckouterPicUrl() {
        String expectedAvatars =
            "https://avatars.mds.yandex.net/get-marketpic/367259/market_hYrCcU6WmWtrY0d50oSMyA/9hq";
        String sourceAvatars =
            "//avatars.mds.yandex.net/get-marketpic/367259/market_hYrCcU6WmWtrY0d50oSMyA/";
        assertEquals(expectedAvatars, pictureUtil.resolveCheckouterPicUrl(sourceAvatars));
    }

    @Test
    public void testResolveCheckouterPicUrl2() {
        String expectedAvatars =
            "https://avatars.mds.yandex.net/get-mpic/1864685/img_id5730302337378058578.jpeg/9hq";
        String sourceAvatars =
            "//avatars.mds.yandex.net/get-mpic/1864685/img_id5730302337378058578.jpeg/50x50";
        assertEquals(expectedAvatars, pictureUtil.resolveCheckouterPicUrl(sourceAvatars));
    }

    @Test
    public void testReplaceOrigWithSquare() {
        String expectedAvatars =
            "https://avatars.mds.yandex.net/get-marketpic/367259/market_hYrCcU6WmWtrY0d50oSMyA/9hq";
        String sourceAvatars =
            "https://avatars.mds.yandex.net/get-marketpic/367259/market_hYrCcU6WmWtrY0d50oSMyA/orig";
        assertEquals(expectedAvatars, PictureUtil.replaceOrigWithSquare(sourceAvatars));
    }
}
