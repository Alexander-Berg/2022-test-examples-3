package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.color.BlueConfig;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.WhiteConfig;
import ru.yandex.market.checkout.checkouter.order.HsCodeToMaterialMapping;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInflationContext;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkupCoefficient;
import ru.yandex.market.checkout.checkouter.order.ReportOrderItemInflater;
import ru.yandex.market.checkout.checkouter.order.parallelimport.ParallelImportWarrantyAction;
import ru.yandex.market.checkout.checkouter.order.validation.cancelpolicy.CancelPolicyDataValidationService;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.MarkupData;
import ru.yandex.market.common.report.model.OfferSeller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;

public class ReportOrderItemInflaterTest {

    private static BigDecimal getCoefficient(Collection<PartnerPriceMarkupCoefficient> list, String name) {
        for (PartnerPriceMarkupCoefficient coefficient : list) {
            if (name.equals(coefficient.getName())) {
                return coefficient.getValue();
            }
        }
        return null;
    }

    @Test
    public void testNoPriceInfo() {
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                mock(ColorConfig.class),
                mock(CancelPolicyDataValidationService.class));
        FoundOffer offer = new FoundOffer();
        OfferSeller seller = new OfferSeller();
        offer.setOfferSeller(seller);
        OrderItem item = new OrderItem();
        inflater.fillInOrderItem(offer, item);
        assertThat(item.getPrices().getPartnerPriceMarkup().getCoefficients(), hasSize(0));
    }

    @Test
    public void testShouldSetPriceInfo() {
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                mock(ColorConfig.class),
                mock(CancelPolicyDataValidationService.class));
        FoundOffer offer = new FoundOffer();
        offer.setDynamicPriceStrategy(2);
        offer.setGoldenMatrix(true);
        offer.setRefMinPrice(new BigDecimal("250"));
        OfferSeller seller = new OfferSeller();
        MarkupData data = new MarkupData();
        data.setCoefficients(Map.of("coef1", new BigDecimal("100")));
        seller.setMarkupData(data);
        offer.setOfferSeller(seller);

        OrderItem item = new OrderItem();
        inflater.fillInOrderItem(offer, item);
        List<PartnerPriceMarkupCoefficient> coefficients = item.getPrices().getPartnerPriceMarkup().getCoefficients();
        assertThat(coefficients, hasSize(4));
        assertThat(getCoefficient(coefficients, "coef1"), comparesEqualTo(new BigDecimal("100")));
        assertThat(getCoefficient(coefficients, "refMinPrice"), comparesEqualTo(new BigDecimal("250")));
        assertThat(getCoefficient(coefficients, "isGoldenMatrix"), comparesEqualTo(new BigDecimal("1")));
        assertThat(getCoefficient(coefficients, "dynamicPriceStrategy"), comparesEqualTo(new BigDecimal("2")));
    }

    @Test
    public void testShouldRoundOrderItemDimensionsCorrectly() {
        // given
        var colorConfigMock = mock(ColorConfig.class);
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                colorConfigMock,
                mock(CancelPolicyDataValidationService.class));
        var foundOffer = new FoundOffer();
        foundOffer.setWidth(new BigDecimal("0.5"));
        foundOffer.setHeight(new BigDecimal("1.5"));
        foundOffer.setDepth(new BigDecimal("0.2"));
        OfferSeller seller = new OfferSeller();
        MarkupData data = new MarkupData();
        data.setCoefficients(Map.of("coef1", new BigDecimal("100")));
        seller.setMarkupData(data);
        foundOffer.setOfferSeller(seller);
        var orderItemInflationContext = OrderItemInflationContext.create(foundOffer, WHITE);
        var orderItem = new OrderItem();
        when(colorConfigMock.getFor(WHITE)).thenReturn(new WhiteConfig(false, false, "url", "url", null, null, true));

        // when
        inflater.inflate(orderItemInflationContext, orderItem);

        // then
        assertThat(orderItem.getWidth(), is(1L));
        assertThat(orderItem.getHeight(), is(2L));
        assertThat(orderItem.getWidth(), is(1L));
    }

    @Test
    public void testShouldSwapParallelImportFields() {
        // given
        //beans
        var colorConfigMock = mock(ColorConfig.class);
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                colorConfigMock,
                mock(CancelPolicyDataValidationService.class));

        //data
        OfferSeller seller = new OfferSeller();
        seller.setWarrantyPeriod("P1Y2M10DT2H30M");
        var foundOffer = new FoundOffer();
        foundOffer.setParallelImport(true);
        foundOffer.setOfferSeller(seller);
        foundOffer.setParallelImportWarrantyAction("WARRANTY");
        var orderItemInflationContext = OrderItemInflationContext.create(foundOffer, BLUE);
        var orderItem = new OrderItem();
        when(colorConfigMock.getFor(BLUE)).thenReturn(new BlueConfig("url", "url", 123L, null, null));

        // when
        inflater.inflate(orderItemInflationContext, orderItem);

        // then
        assertThat(orderItem.isParallelImport(), is(true));
        assertThat(orderItem.getSellerWarrantyPeriod(), is("P1Y2M10DT2H30M"));
        assertThat(orderItem.getParallelImportWarrantyAction(), is(ParallelImportWarrantyAction.WARRANTY));
    }

    @Test
    public void testShouldSetDefaultParallelImportWarrantyAction() {
        // given
        //beans
        var colorConfigMock = mock(ColorConfig.class);
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                colorConfigMock,
                mock(CancelPolicyDataValidationService.class));

        //data
        OfferSeller seller = new OfferSeller();
        seller.setWarrantyPeriod("P1Y2M10DT2H30M");
        var foundOffer = new FoundOffer();
        foundOffer.setParallelImport(true);
        foundOffer.setOfferSeller(seller);
        var orderItemInflationContext = OrderItemInflationContext.create(foundOffer, BLUE);
        var orderItem = new OrderItem();
        when(colorConfigMock.getFor(BLUE)).thenReturn(new BlueConfig("url", "url", 123L, null, null));

        // when
        inflater.inflate(orderItemInflationContext, orderItem);

        // then
        assertThat(orderItem.isParallelImport(), is(true));
        assertThat(orderItem.getSellerWarrantyPeriod(), is("P1Y2M10DT2H30M"));
        assertThat(orderItem.getParallelImportWarrantyAction(), is(ParallelImportWarrantyAction.CHARGE_BACK));
    }

    @Test
    public void testShouldSetDefaultParallelImportWarrantyActionOnUnknownValue() {
        // given
        //beans
        var colorConfigMock = mock(ColorConfig.class);
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                colorConfigMock,
                mock(CancelPolicyDataValidationService.class));

        //data
        OfferSeller seller = new OfferSeller();
        seller.setWarrantyPeriod("P1Y2M10DT2H30M");
        var foundOffer = new FoundOffer();
        foundOffer.setParallelImport(true);
        foundOffer.setOfferSeller(seller);
        foundOffer.setParallelImportWarrantyAction("KISS_ASS");
        var orderItemInflationContext = OrderItemInflationContext.create(foundOffer, BLUE);
        var orderItem = new OrderItem();
        when(colorConfigMock.getFor(BLUE)).thenReturn(new BlueConfig("url", "url", 123L, null, null));

        // when
        inflater.inflate(orderItemInflationContext, orderItem);

        // then
        assertThat(orderItem.isParallelImport(), is(true));
        assertThat(orderItem.getSellerWarrantyPeriod(), is("P1Y2M10DT2H30M"));
        assertThat(orderItem.getParallelImportWarrantyAction(), is(ParallelImportWarrantyAction.CHARGE_BACK));
    }

    @Test
    public void testShouldNotSwapParallelImportFields() {
        // given
        //beans
        var colorConfigMock = mock(ColorConfig.class);
        ReportOrderItemInflater inflater = new ReportOrderItemInflater(
                mock(HsCodeToMaterialMapping.class),
                colorConfigMock,
                mock(CancelPolicyDataValidationService.class));

        //data
        var foundOffer = new FoundOffer();
        var orderItemInflationContext = OrderItemInflationContext.create(foundOffer, BLUE);
        var orderItem = new OrderItem();
        when(colorConfigMock.getFor(BLUE)).thenReturn(new BlueConfig("url", "url", 123L, null, null));

        // when
        inflater.inflate(orderItemInflationContext, orderItem);

        // then
        assertThat(orderItem.isParallelImport(), is(false));
        assertThat(orderItem.getSellerWarrantyPeriod(), nullValue());
        assertThat(orderItem.getParallelImportWarrantyAction(), nullValue());
    }
}
