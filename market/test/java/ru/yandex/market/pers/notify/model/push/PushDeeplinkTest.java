package ru.yandex.market.pers.notify.model.push;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.ADD_MODEL_REVIEW;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.ADD_SHOP_REVIEW;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.ARTICLE;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.CART;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.COLLECTION;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.COMPARISON;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.MODEL;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.ORDERS;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.REFEREE;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.REVIEWS;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.START;
import static ru.yandex.market.pers.notify.model.push.PushDeeplink.WISHLIST;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         09.08.17
 */
public class PushDeeplinkTest {
    @Test
    public void nativeAndTouchLinksHasSameParams() throws Exception {
        checkAllFormatsWithSameArguments(START);
        checkAllFormatsWithSameArguments(CART);
        checkAllFormatsWithSameArguments(REFEREE);
        checkAllFormatsWithSameArguments(ORDERS);
        checkAllFormatsWithSameArguments(ADD_MODEL_REVIEW, 23123L);
        checkAllFormatsWithSameArguments(ADD_SHOP_REVIEW, 32847L);
        checkAllFormatsWithSameArguments(REVIEWS);
        checkAllFormatsWithSameArguments(WISHLIST);
        checkAllFormatsWithSameArguments(MODEL, 32423L);
        checkAllFormatsWithSameArguments(COMPARISON, 13123L, 321L, 98723412L);
        checkAllFormatsWithSameArguments(COLLECTION, "wha?");
        checkAllFormatsWithSameArguments(ARTICLE, "iewor23");
    }

    private void checkAllFormatsWithSameArguments(PushDeeplink deeplink, Object... params) {
        String nativeFormat = deeplink.nativeFormat(params);
        String touchFormat = deeplink.nativeFormat(params);
        assertTrue(nativeFormat != null && !nativeFormat.isEmpty());
        assertTrue(touchFormat != null && !touchFormat.isEmpty());

        for (Object param : params) {
            assertTrue(nativeFormat.contains(String.valueOf(param)));
            assertTrue(touchFormat.contains(String.valueOf(param)));
        }
    }
}
