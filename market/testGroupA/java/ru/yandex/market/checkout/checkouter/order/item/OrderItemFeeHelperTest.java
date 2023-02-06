package ru.yandex.market.checkout.checkouter.order.item;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.report.MarketSearchSession;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class OrderItemFeeHelperTest extends AbstractWebTestBase {

    private static final String EXAMPLE_FEE_SHOW = "g0TnfFbEsOI3GekN8cHrdNfWRmdsjUj4bKGEvPHv2-UcjnEw3Y" +
            "-5OXSiJFz5FHbcCJ2TS-SysyY4IQTxSTpgnyaJp94s8cHLdexpp-A4rrvZCnYbUDHJzyVZEkrSb4Qq";
    private static final String EXAMPLE_LEGACY_FEE_SHOW = "dohMCvJp+kXVP9ssz/8VbNegTQ+tDDcgNi" +
            "/HP2NcAks4LCr3ItcBtljREc9FW1o0dsk371uIcC5SzzZ0CPwpkGzbUlRfCWV5HpQ2hou1rdKnZyr7NGdkJc6x6g49WY8o";
    private static final String EXAMPLE_NEW_FEE_SHOW = "g0TnfFbEsOLRqJt4gkdteY7iIOSe6v1GL151eZISM9ruuJt2dSv1d" +
            "/MvTyAsExlkEnR9AF3G3kZJD+taEG9fKbntJYSYKUi9n6qIyPsvZzs=";
    private static final String DECIPHERED_FEE = "3.0000";
    private static final String DECIPHERED_FEE_SUM = "20";
    private static final String DECIPHERED_SHOW_BLOCK_UID = "9968797411121234769";
    private static final String DECIPHERED_WARE_MD_5 = "-_40VqaS9BpXO1qaTtweBA";
    private static final Integer DECIPHERED_PP = 1000;
    private static final String DECIPHERED_SHOW_UID = "showUid";
    private static final String WRONG_SHOW_INFO = "THi2k" +
            "-pxPNKanO16Cm5gSztQhd1caxrACjcdb06KyNEF6bndMJO2G5o7KEP6lypjjJIfCSU7D03E9-sJ1HbkVFve7dJ10051117Xx" +
            "-aZGwQMyKsl3Pb6FkJQ1qGuY7Mr_okQDMMaVbYFNBRnqyhias5eXzTJipruJiOHnd";

    @Autowired
    private OrderItemFeeHelper orderItemFeeHelper;

    @Test
    public void testDecipherUsingLegacySecret() {
        MarketSearchSession.CpaContextRecord res = orderItemFeeHelper.decipherCpaShowInfo(EXAMPLE_FEE_SHOW);
        assertThat(res.getFee(), is(DECIPHERED_FEE));
        assertThat(res.getFeeSum(), is(DECIPHERED_FEE_SUM));
    }

    @Test
    public void testDecipherUsingNewSecret() {
        MarketSearchSession.CpaContextRecord res = orderItemFeeHelper.decipherCpaShowInfo(EXAMPLE_LEGACY_FEE_SHOW);
        assertThat(res.getFee(), is(DECIPHERED_FEE));
        assertThat(res.getFeeSum(), is(DECIPHERED_FEE_SUM));
    }

    @Test
    public void testDecipherNewFeeShow() {
        var result = orderItemFeeHelper.decipherCpaShowInfo(EXAMPLE_NEW_FEE_SHOW);
        assertThat(result.getPp(), is(DECIPHERED_PP));
        assertThat(result.getFee(), is(DECIPHERED_FEE));
        assertThat(result.getFeeSum(), is(DECIPHERED_FEE_SUM));
        assertThat(result.getShowUid(), is(DECIPHERED_SHOW_UID));
        assertThat(result.getWareMd5(), is(DECIPHERED_WARE_MD_5));
        assertThat(result.getShowBlockId(), is(DECIPHERED_SHOW_BLOCK_UID));
    }

    @Test
    public void decipheringOfWrongShowInfoShouldThrowError() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            orderItemFeeHelper.decipherCpaShowInfo(WRONG_SHOW_INFO);
            fail("Exception wasn't thrown");
        });
    }

    @Test
    public void cartWithWrongShowInfoShouldSetItemToZero() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        OrderItem oldOrderItem = OrderItemProvider.getOrderItem();
        oldOrderItem.setShowInfo(WRONG_SHOW_INFO);
        OrderItem newOrderItem = OrderItemProvider.getOrderItem();
        newOrderItem.setShowInfo(EXAMPLE_FEE_SHOW);
        parameters.getOrder().setItems(List.of(oldOrderItem, newOrderItem));
        parameters.configuration().cart().body().skipShowInfoAdjusting(true);
        parameters.setCheckCartErrors(false);
        var result = orderCreateHelper.cart(parameters);

        assertThat(result.getCarts().iterator().next().firstItemFor(oldOrderItem.getFeedOfferId()).getChanges(),
                Matchers.contains(ItemChange.MISSING));
    }
}
