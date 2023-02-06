package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.report.MarketSearchSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class OrderItemFeeTest extends AbstractWebTestBase {

    private static final String ZERO_FEE = "DGzdsCjtjKP58DCIvwQeVlKlKoQHgGysMZli1dcNOvscfUe7fMUyGRRrVLCjMFv2sx-NsKk9X" +
            "MAeuGKQP0DYBhpExRwCEO3yeDWPLCX8JCzCdMF6vaWb1iUg3JbNskKAVG9W5KPViHNs42UvBIb3bmDEFjQCRQ264RroXAYVonQ,";
    private static final String NON_NULL_FEE = "x1gNCM9d6AOix9DLAnsgl6v_zgA83uE0_lWwWUU7f1ctnFCXvC-F7VAlg5IGH8b4ITcGI" +
            "DwNYFJrnutmOKcXpWH3Qt_r1DndvFo3Pm3NYcgl0EkWP5r-uwL7o9QZSR_bdc4dHVHl_no,";
    @Autowired
    private CipherService reportCipherService;

    @Test
    public void checkFeeSum() throws InvalidProtocolBufferException {
        // проверочный тест для констант, почти моментальный
        final MarketSearchSession.CpaContextRecord zeroFeeRecord = MarketSearchSession.CpaContextRecord.newBuilder()
                .mergeFrom(reportCipherService.performUnpadding(reportCipherService.decipher(ZERO_FEE))).build();
        assertThat(new BigDecimal(zeroFeeRecord.getFee()), comparesEqualTo(BigDecimal.ZERO));

        final MarketSearchSession.CpaContextRecord nonZeroFeeRecord = MarketSearchSession.CpaContextRecord.newBuilder()
                .mergeFrom(reportCipherService.performUnpadding(reportCipherService.decipher(NON_NULL_FEE))).build();

        assertThat(new BigDecimal(nonZeroFeeRecord.getFee()), greaterThan(BigDecimal.ZERO));
    }

    @Test
    public void testCartFailsIfFeeInsane() {
        String showInfo = "g0TnfFbEsOI3GekN8cHrdNfWRmdsjUj4bKGEvPHv2-UcjnEw3Y-5OXSiJFz5FHbcCJ2TS" +
                "-SysyY4IQTxSTpgnyaJp94s8cHLdexpp-A4rrvZCnYbUDHJzyVZEkrSb4Qq";
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(oi -> oi.setShowInfo(showInfo));
        parameters.configuration().cart().body().skipShowInfoAdjusting(true);
        parameters.setCheckCartErrors(false);
        MultiCart cart = orderCreateHelper.cart(parameters);

        cart.getCarts().iterator().next().getItems()
                .forEach(orderItem -> assertThat(orderItem.getChanges(), contains(ItemChange.MISSING)));
    }

    @Test
    public void zeroFeeFromReportShouldNotBeAppliedForBlueOrders() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        final ItemInfo overrideItemInfo = parameters.getReportParameters().overrideItemInfo(
                parameters.getOrder().getItems().iterator().next().getFeedOfferId()
        );
        overrideItemInfo.setShowInfo(ZERO_FEE);

        Order order = orderCreateHelper.createOrder(parameters);
        // Перечитаем из БД
        order = orderService.getOrder(order.getId());
        order.getItems().forEach(oi -> {
            assertThat(oi.getFeeInt(), equalTo(200));
            assertThat(oi.getShowUid(), equalTo("9968797411121234769"));
        });
    }
}
