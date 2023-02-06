package ru.yandex.market.api.wishlist;

import it.unimi.dsi.fastutil.longs.LongLists;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.controller.v2.WishListControllerV2;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.wishlist.WishListResultV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.model.WishListSort;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.PersBasketTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by fettsery on 18.06.18.
 */
public class WishListControllerTest extends BaseTest {

    @Inject
    WishListControllerV2 controller;
    @Inject
    ReportTestClient reportTestClient;
    @Inject
    PersBasketTestClient persBasketTestClient;

    private static final WishListSort SORT_MODE = new WishListSort(WishListSort.Type.MODEL_NAME, SortOrder.ASC);

    @Test
    public void testNotFilteredItems() {
        long uid = 4001796369L;

        persBasketTestClient.getItems(uid, "wishlist-items.json");

        persBasketTestClient.getLabels(uid, "wishlist-labels.json");

        reportTestClient.getModelInfoById(Arrays.asList(14206636L, 1724554654L, 1721921261L), "wishlist-models.json");

        User user = new User(new OauthUser(uid), null, null, null);

        WishListResultV2 wishListResult = ((ApiDeferredResult<WishListResultV2>) controller.getItems(
            user,
            Collections.emptyList(),
            PageInfo.ALL_ITEMS,
            LongLists.EMPTY_LIST,
            SORT_MODE,
            null,
            null,
            GenericParams.DEFAULT))
            .waitResult();

        Assert.assertEquals(3, wishListResult.getItems().size());
    }

    @Test
    public void testFilteredItems() {
        long uid = 4001796369L;

        persBasketTestClient.getItems(uid, "wishlist-items.json");

        persBasketTestClient.getLabels(uid, "wishlist-labels.json");

        reportTestClient.getModelInfoById(Arrays.asList(14206636L, 1721921261L), "wishlist-models.json");

        User user = new User(new OauthUser(uid), null, null, null);

        WishListResultV2 wishListResult = ((ApiDeferredResult<WishListResultV2>) controller.getItems(
            user,
            Collections.emptyList(),
            PageInfo.ALL_ITEMS,
            LongLists.EMPTY_LIST,
            SORT_MODE,
            LocalDateTime.parse("2018-06-16T10:00:00"),
            LocalDateTime.parse("2018-06-20T10:00:00"),
            GenericParams.DEFAULT))
            .waitResult();

        Assert.assertEquals(2, wishListResult.getItems().size());
        Assert.assertEquals(14206636L, wishListResult.getItems().get(0).getModelId());
        Assert.assertEquals(1721921261L, wishListResult.getItems().get(1).getModelId());
    }
}
