package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.util.Sets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.DeliveryFloorValidationStrategy;
import ru.yandex.market.checkout.checkouter.actualization.utils.DeliveryOptionsUtils;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.LiftingOptions;
import ru.yandex.market.checkout.checkouter.delivery.LiftingOptionsType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties.TariffsAndLiftExperimentToggle;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;

public class CartLiftPriceTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @Test
    public void testLiftPriceCalculationIsDisabled() {
        disableLiftOptions();

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testLiftPriceCalculationIsDisabledByTarrifsToggle() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                TariffsAndLiftExperimentToggle.OFF);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);
        enableLiftOptions(parameters);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testOrderHasNoLargeSizeOffers() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(false);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testLiftTypeFree() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.NOT_NEEDED, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testElevatorLiftTypeWithForcedTarrifsExperiments() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters); // но вызываем без экспа тарифов, но должен зафорситься в препроцессоре
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                TariffsAndLiftExperimentToggle.FORCE);
        parameters.getOrder().getDelivery().setLiftType(LiftType.ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.ELEVATOR, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertEquals(BigDecimal.valueOf(150L), actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testElevatorLiftType() {
        testAnyElevatorLiftType(LiftType.ELEVATOR);
    }

    @Test
    public void testCargoElevatorLiftType() {
        testAnyElevatorLiftType(LiftType.CARGO_ELEVATOR);
    }

    @Test
    public void testManualLiftTypeWithoutFloor() {
        MultiCart actualCart = cartWithManualLiftTypeAndFloor(null);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.NOT_NEEDED, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testManualTypeWithIncorrectFloor() {
        MultiCart actualCart = cartWithManualLiftTypeAndFloor("floor");
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.NOT_NEEDED, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testManualTypeWithValidPositiveFloor() {
        MultiCart actualCart = cartWithManualLiftTypeAndFloor("2");
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.MANUAL, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertEquals(BigDecimal.valueOf(300L), actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testManualTypeWithFreeDeliveryEnabled() {
        ShopMetaData newMeta =
                ShopMetaDataBuilder.createTestDefault().withFreeLiftingEnabled(true).build();
        MultiCart actualCart = cartWithManualLiftTypeAndFloor("2", newMeta);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(BigDecimal.valueOf(0L),
                actualOrder.getDeliveryOptions().get(0).getPresentationFields().getLiftingOptions()
                        .getManualLiftPerFloorCost());
        assertEquals(LiftingOptionsType.INCLUDED,
                actualOrder.getDeliveryOptions().get(0).getPresentationFields().getLiftingOptions().getType());
    }


    @Test
    public void testManualTypeWithValidNegativeFloor() {
        MultiCart actualCart = cartWithManualLiftTypeAndFloor("-2");
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.MANUAL, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertEquals(BigDecimal.valueOf(300L), actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testManualTypeWithNonIntegerFloor() {
        MultiCart actualCart = cartWithManualLiftTypeAndFloor("2.5");
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.NOT_NEEDED, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testLiftTypeNotNeeded() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.NOT_NEEDED);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.NOT_NEEDED, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testLiftTypeNull() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(null);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertNull(actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testDeliveryTypeNull() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
        parameters.getOrder().getDelivery().setType(null);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(LiftType.CARGO_ELEVATOR, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertEquals(BigDecimal.valueOf(150), actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    @Test
    public void testFailFloorValidationStrategy() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.DELIVERY_FLOOR_VALIDATION_STRATEGY,
                DeliveryFloorValidationStrategy.FAIL);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor("floor"));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);
        parameters.configuration().cart().response().setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        List<ValidationResult> validationResults = multiCart.getCarts().get(0).getValidationErrors();
        MatcherAssert.assertThat(validationResults, Matchers.hasSize(1));
        ValidationResult validationResult = validationResults.get(0);
        assertEquals("DELIVERY_FLOOR_VALIDATION_ERROR", validationResult.getCode());
        assertEquals(ValidationResult.Severity.ERROR, validationResult.getSeverity());
        assertEquals("Field order.delivery.buyerAddress.floor should be non-zero number",
                validationResult.getMessage());
        assertEquals("basic", validationResult.getType());
    }

    @Test
    public void testElevatorAvailableTest() {
        checkouterProperties.setEnableLiftOptionsForApiMerch(true);
        checkouterProperties.setLiftOptionsBlackList(null);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getOrder().setShopId(777L);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        Delivery delivery = actualOrder.getDeliveryOptions().get(0);
        LiftingOptions liftingOptions = delivery.getPresentationFields().getLiftingOptions();
        assertEquals(LiftingOptionsType.AVAILABLE, liftingOptions.getType());
        assertEquals(DeliveryOptionsUtils.MANUAL_LIFT_PRICE, liftingOptions.getManualLiftPerFloorCost());
        assertNull(liftingOptions.getUnloadCost());
    }

    @Test
    public void testUnloadAvailableTest() {
        checkouterProperties.setEnableLiftOptionsForApiMerch(true);
        checkouterProperties.setLiftOptionsBlackList(null);
        checkouterProperties.setNewLargeSizeCalculation(true);
        checkouterProperties.setNewLargeSizeWithVolumeCalculation(true);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.setWeight(BigDecimal.valueOf(1000)); // 1 тонна
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getOrder().setShopId(777L);
        parameters.getReportParameters().setLargeSize(true);

        parameters.getReportParameters().getActualDelivery().getResults().get(0).setLargeSize(true);
        parameters.getReportParameters().getOrder().getItems().iterator().next().setHeight(1111L);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        Delivery delivery = actualOrder.getDeliveryOptions().get(0);
        LiftingOptions liftingOptions = delivery.getPresentationFields().getLiftingOptions();
        assertEquals(LiftingOptionsType.AVAILABLE, liftingOptions.getType());
        assertEquals(BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(10)), liftingOptions.getUnloadCost());

        checkouterProperties.setNewLargeSizeCalculation(false);
    }

    @Test
    public void testElevatorNotAvailableInBlackListTest() {
        checkouterProperties.setEnableLiftOptionsForApiMerch(true);
        checkouterProperties.setLiftOptionsBlackList(Sets.newLinkedHashSet(666L, 4L));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getOrder().setShopId(666L);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        Delivery delivery = actualOrder.getDeliveryOptions().get(0);
        LiftingOptions liftingOptions = delivery.getPresentationFields().getLiftingOptions();
        assertEquals(LiftingOptionsType.NOT_AVAILABLE, liftingOptions.getType());
        assertNull(liftingOptions.getUnloadCost());
    }

    @Test
    public void testElevatorNotAvailableSwitchedApiMerchTest() {
        checkouterProperties.setLiftOptionsBlackList(Sets.newLinkedHashSet(123L, 4L));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getOrder().setShopId(666L);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        Delivery delivery = actualOrder.getDeliveryOptions().get(0);
        LiftingOptions liftingOptions = delivery.getPresentationFields().getLiftingOptions();
        assertEquals(LiftingOptionsType.NOT_AVAILABLE, liftingOptions.getType());
    }

    @Test
    public void testNullLiftingOptionsTest() {
        checkouterProperties.setEnableLiftOptionsForApiMerch(true);
        checkouterProperties.setLiftOptionsBlackList(null);
        checkouterProperties.setNewLargeSizeCalculation(true);
        checkouterProperties.setNewLargeSizeWithVolumeCalculation(true);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.setWeight(BigDecimal.valueOf(1000)); // 1 тонна
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getOrder().setShopId(777L);
        parameters.getReportParameters().setLargeSize(true);

        parameters.getReportParameters().getActualDelivery().getResults().get(0).setLargeSize(true);
        OrderItem orderItem = parameters.getReportParameters().getOrder().getItems().iterator().next();
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId())
                .setDimensions(Arrays.asList("301", "12", "12"));

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        Delivery delivery = actualOrder.getDeliveryOptions().get(0);
        LiftingOptions liftingOptions = delivery.getPresentationFields().getLiftingOptions();
        assertEquals(LiftingOptionsType.AVAILABLE, liftingOptions.getType());
        assertNull(liftingOptions.getElevatorLiftCost());
        assertNull(liftingOptions.getCargoElevatorLiftCost());

        checkouterProperties.setNewLargeSizeCalculation(false);
    }

    @Test
    public void testNullLiftingOptionsWithoutCargoTest() {
        checkouterProperties.setEnableLiftOptionsForApiMerch(true);
        checkouterProperties.setLiftOptionsBlackList(null);
        checkouterProperties.setNewLargeSizeCalculation(true);
        checkouterProperties.setNewLargeSizeWithVolumeCalculation(true);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.setWeight(BigDecimal.valueOf(1000)); // 1 тонна
        parameters.getOrder().getDelivery().setLiftType(LiftType.FREE);
        parameters.getOrder().setShopId(777L);
        parameters.getReportParameters().setLargeSize(true);

        parameters.getReportParameters().getActualDelivery().getResults().get(0).setLargeSize(true);
        OrderItem orderItem = parameters.getReportParameters().getOrder().getItems().iterator().next();
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId())
                .setDimensions(Arrays.asList("201", "12", "12"));

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        Delivery delivery = actualOrder.getDeliveryOptions().get(0);
        LiftingOptions liftingOptions = delivery.getPresentationFields().getLiftingOptions();
        assertEquals(LiftingOptionsType.AVAILABLE, liftingOptions.getType());
        assertNull(liftingOptions.getElevatorLiftCost());
        assertNotNull(liftingOptions.getCargoElevatorLiftCost());

        checkouterProperties.setNewLargeSizeCalculation(false);
    }

    private void testAnyElevatorLiftType(LiftType liftType) {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setLiftType(liftType);
        parameters.getReportParameters().setLargeSize(true);

        MultiCart actualCart = orderCreateHelper.cart(parameters);
        Order actualOrder = actualCart.getCarts().get(0);
        assertEquals(liftType, actualOrder.getDeliveryOptions().get(0).getLiftType());
        assertEquals(BigDecimal.valueOf(150L), actualOrder.getDeliveryOptions().get(0).getLiftPrice());
    }

    private MultiCart cartWithManualLiftTypeAndFloor(String floor, ShopMetaData newMeta) {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        if (newMeta != null) {
            parameters.addShopMetaData(parameters.getShopId(), newMeta);
        }
        enableLiftOptions(parameters);
        parameters.getReportParameters().setLargeSize(true);
        parameters.getOrder().getDelivery().setLiftType(LiftType.MANUAL);
        Address testAddress = AddressProvider.getAddress(address -> address.setFloor(floor));
        parameters.getOrder().getDelivery().setBuyerAddress(testAddress);

        return orderCreateHelper.cart(parameters);
    }

    private MultiCart cartWithManualLiftTypeAndFloor(String floor) {
        return cartWithManualLiftTypeAndFloor(floor, null);
    }

    private void enableLiftOptions(Parameters parameters) {
        checkouterProperties.setEnableLiftOptions(true);
        parameters.setExperiments(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE);
    }

    private void disableLiftOptions() {
        checkouterProperties.setEnableLiftOptions(false);
    }
}
