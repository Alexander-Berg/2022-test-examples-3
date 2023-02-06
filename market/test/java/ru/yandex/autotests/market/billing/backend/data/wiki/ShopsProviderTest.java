package ru.yandex.autotests.market.billing.backend.data.wiki;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author ivmelnik
 * @since 15.03.17
 */
@Ignore
public class ShopsProviderTest {

    private static final String SINGLE_SHOP_TAG = "ShopPrepaymentTest";

    private static final String SEVERAL_SHOPS_TAG = "FinanceCutoffExecutorTest";
    private static final int SEVERAL_SHOPS_NUMBER = 5;

    private static final String FIRST_TAG_IN_LIST = "SingleApiShopDsOutletsTest";
    private static final String SECOND_TAG_IN_LIST = "SingleShopOutletsTest";

    private static final String NON_EXISTING_TAG = "ABCXYZTest";

    @Test
    public void getShop() throws Exception {
        Long shop = ShopsProvider.getShop(SINGLE_SHOP_TAG);
        assertThat(shop, is(notNullValue()));
    }

    @Test
    public void getShopsSimple() throws Exception {
        List<Long> shops = ShopsProvider.getShops(SEVERAL_SHOPS_TAG);
        assertThat(shops, is(not(empty())));
    }

    @Test
    public void getShopsWithNumber() throws Exception {
        List<Long> shops = ShopsProvider.getShops(SEVERAL_SHOPS_TAG, SEVERAL_SHOPS_NUMBER);
        assertThat(shops.size(), is(equalTo(SEVERAL_SHOPS_NUMBER)));
    }

    @Test
    public void getShopsWithTagList() throws Exception {
        List<Long> shops1 = ShopsProvider.getShops(FIRST_TAG_IN_LIST);
        List<Long> shops2 = ShopsProvider.getShops(SECOND_TAG_IN_LIST);
        assertThat(shops1, is(equalTo(shops2)));
    }

    // negative cases

    @Test(expected = AssertionError.class)
    public void getShopNegative() throws Exception {
        ShopsProvider.getShop(NON_EXISTING_TAG);
    }

    @Test(expected = AssertionError.class)
    public void getShopsNegative() throws Exception {
        ShopsProvider.getShops(NON_EXISTING_TAG);
    }

    @Test(expected = AssertionError.class)
    public void getShopsWithNumberNegative() throws Exception {
        ShopsProvider.getShops(NON_EXISTING_TAG, SEVERAL_SHOPS_NUMBER);
    }

}
