package ru.yandex.market.logistics.nesu.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOptionService;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryOptionServicePriceRule;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryServiceCode;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.converter.lms.LmsExternalParamConverter;
import ru.yandex.market.logistics.nesu.converter.modifier.CurrencyConverter;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionResultService;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterCost;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.model.entity.DeliveryTypeServiceSettings;
import ru.yandex.market.logistics.nesu.model.entity.SenderDeliveryTypeServiceSettings;
import ru.yandex.market.logistics.nesu.model.entity.ServiceType;
import ru.yandex.market.logistics.nesu.service.deliveryoption.DeliveryOptionNotAvailableException;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.CostCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.ServicesCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.ServicesCostCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionContext;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionShipment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Расчет стоимости услуг")
class ServicesCalculatorTest extends AbstractTest {

    private static final DeliveryType DELIVERY_TYPE = DeliveryType.COURIER;
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2020, 2, 20);

    private DeliveryOptionsFilterCost filterCost = new DeliveryOptionsFilterCost();
    private Map<DeliveryType, List<DeliveryTypeServiceSettings>> deliveryTypeServices = new HashMap<>();
    private Map<DeliveryType, List<SenderDeliveryTypeServiceSettings>> senderDeliveryTypeServices = new HashMap<>();
    private DeliveryOptionContext.DeliveryOptionContextBuilder context = DeliveryOptionContext.builder();
    private List<DeliveryOptionService> deliveryOptionServices = new ArrayList<>();
    private long deliveryOptionModifiedCost;
    private EnumConverter enumConverter = new EnumConverter();
    private CurrencyConverter currencyConverter = new CurrencyConverter();
    private CostCalculator costCalculator = new CostCalculator(
        currencyConverter,
        new LmsExternalParamConverter()
    );
    private ServicesCostCalculator servicesCostCalculator = new ServicesCostCalculator(currencyConverter);

    @Test
    @DisplayName("Фиксированная стоимость")
    void fixedCost() {
        addSettingsService(ServiceType.DELIVERY);

        deliveryOptionServices.add(defaultDelivery(10_00));
        deliveryOptionModifiedCost = 10_00;

        assertCost(ServiceType.DELIVERY, BigDecimal.TEN);
    }

    @Test
    @DisplayName("Сортировка для доставки без сортировочного центра")
    void sortNoSortingCenter() {
        addSettingsService(ServiceType.SORT);

        context.shipment(new DeliveryOptionShipment(
            SHIPMENT_DATE,
            SHIPMENT_DATE,
            ShipmentType.WITHDRAW,
            PartnerResponse.newBuilder()
                .id(1L)
                .marketId(100L)
                .partnerType(PartnerType.DELIVERY)
                .build()
        ));

        assertCost(ServiceType.SORT, null);
    }

    @Test
    @DisplayName("Сортировка для доставки в сортировочный центр")
    void sortSortingCenter() {
        addSettingsService(ServiceType.SORT);

        context.shipment(new DeliveryOptionShipment(
            SHIPMENT_DATE,
            SHIPMENT_DATE,
            ShipmentType.WITHDRAW,
            PartnerResponse.newBuilder()
                .id(1L)
                .marketId(100L)
                .partnerType(PartnerType.SORTING_CENTER)
                .build()
        ));

        assertCost(ServiceType.SORT, BigDecimal.valueOf(27));
    }

    @Test
    @DisplayName("Процент от объявленной стоимости товаров")
    void itemsPercentCost() {
        addSettingsService(ServiceType.INSURANCE);

        filterCost.setAssessedValue(BigDecimal.valueOf(1000));

        deliveryOptionServices.add(defaultInsurance());

        assertCost(ServiceType.INSURANCE, BigDecimal.valueOf(6));
    }

    @Test
    @DisplayName("Страховка по максимальной сумме")
    void insuranceLimit() {
        addSettingsService(ServiceType.INSURANCE);

        filterCost.setAssessedValue(BigDecimal.valueOf(1000_000));

        deliveryOptionServices.add(defaultInsurance());

        softly.assertThatThrownBy(this::getServices)
            .isInstanceOf(DeliveryOptionNotAvailableException.class)
            .hasMessage("Cost 6000.000 for delivery option service INSURANCE exceeds maximum 3000.00");
    }

    @Test
    @DisplayName("Страховка без указанной объявленной стоимости")
    void insuranceNoAssessedValue() {
        addSettingsService(ServiceType.INSURANCE);

        deliveryOptionServices.add(defaultInsurance());

        assertCost(ServiceType.INSURANCE, null);
    }

    @Test
    @DisplayName("Наложенный платеж, другая услуга")
    void cashServiceOtherService() {
        addSettingsService(ServiceType.DELIVERY, true);
        addSettingsService(ServiceType.WAIT_20, true);

        deliveryOptionModifiedCost = 2000_00;
        deliveryOptionServices.add(defaultDelivery(2000_00));
        deliveryOptionServices.add(defaultCheck(true));
        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();

        assertThat(services).hasSize(3);
        softly.assertThat(services.get(0).getCode()).isEqualTo(ServiceType.DELIVERY);
        softly.assertThat(services.get(1).getCode()).isEqualTo(ServiceType.WAIT_20);
        assertCashService(services.get(2), "34.51");
    }

    @Test
    @DisplayName("Наложенный платеж, только стоимость товаров")
    void cashServiceDeliveryAndItemsPrice() {
        filterCost.setItemsSum(BigDecimal.valueOf(10000));

        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();

        assertThat(services).hasSize(1);
        assertCashService(services.get(0), "170");
    }

    @Test
    @DisplayName("Наложенный платеж, стоимость доставки вручную")
    void cashServiceManualDeliveryForCustomer() {
        filterCost.setManualDeliveryForCustomer(BigDecimal.valueOf(2000));

        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();

        assertThat(services).hasSize(1);
        assertCashService(services.get(0), "34");
    }

    @Test
    @DisplayName("Наложенный платеж, доставка и стоимость товаров")
    void cashServiceItemsPrice() {
        addSettingsService(ServiceType.DELIVERY, true);

        filterCost.setItemsSum(BigDecimal.valueOf(10000));

        deliveryOptionModifiedCost = 200_00;
        deliveryOptionServices.add(defaultDelivery(200_00));
        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();

        assertThat(services).hasSize(2);
        softly.assertThat(services.get(0).getCode()).isEqualTo(ServiceType.DELIVERY);
        assertCashService(services.get(1), "173.40");
    }

    @Test
    @DisplayName("Наложенный платеж, минимальная сумма")
    void cashServiceMinPrice() {
        addSettingsService(ServiceType.DELIVERY, true);

        deliveryOptionModifiedCost = 10_00;
        deliveryOptionServices.add(defaultDelivery(10_00));
        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();

        assertThat(services).hasSize(2);
        softly.assertThat(services.get(0).getCode()).isEqualTo(ServiceType.DELIVERY);
        assertCashService(services.get(1), "30");
    }

    @Test
    @DisplayName("Наложенный платеж, максимальная сумма")
    void cashServiceMaxPrice() {
        addSettingsService(ServiceType.DELIVERY, true);

        deliveryOptionModifiedCost = 10_00;
        deliveryOptionServices.add(defaultDelivery(10_00));
        deliveryOptionServices.add(defaultCashService());

        filterCost.setManualDeliveryForCustomer(BigDecimal.valueOf(1000_000));

        softly.assertThatThrownBy(this::getServices)
            .isInstanceOf(DeliveryOptionNotAvailableException.class)
            .hasMessage("Cost 17000 for delivery option service CASH_SERVICE exceeds maximum 1700.00");
    }

    @Test
    @DisplayName("Наложенный платеж, модифицированная стоимость доставки")
    void cashServiceModifiedDelivery() {
        addSettingsService(ServiceType.DELIVERY, true);

        deliveryOptionModifiedCost = 10000_00;
        deliveryOptionServices.add(defaultDelivery(10_00));
        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();

        assertThat(services).hasSize(2);
        softly.assertThat(services.get(0).getCode()).isEqualTo(ServiceType.DELIVERY);
        assertCashService(services.get(1), "170");
    }

    @Test
    @DisplayName("Нет настроенных услуг")
    void noServices() {
        softly.assertThat(getServices()).isEmpty();
    }

    @Test
    @DisplayName("Обязательная услуга не настроена")
    void requiredServiceNotConfigured() {
        DeliveryTypeServiceSettings deliveryTypeServiceSettings = new DeliveryTypeServiceSettings()
            .setDeliveryType(DELIVERY_TYPE)
            .setServiceType(ServiceType.WAIT_20)
            .setEditableCustomerPay(true)
            .setDefaultCustomerPay(true)
            .setRequired(true);
        deliveryTypeServices.put(DELIVERY_TYPE, List.of(deliveryTypeServiceSettings));
        filterCost.setManualDeliveryForCustomer(BigDecimal.ZERO);

        deliveryOptionServices.add(defaultCheck(false));

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(service.isCustomerPay()).isFalse();
    }

    @Test
    @DisplayName("Услуга оплачивается магазином")
    void senderPayService() {
        addSettingsService(ServiceType.WAIT_20, false);

        deliveryOptionServices.add(defaultCheck(false));

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(service.isCustomerPay()).isFalse();
    }

    @Test
    @DisplayName("Услуга оплачивается магазином, но КД перевыставляет услугу на покупателя")
    void senderPayServiceWith() {
        addSettingsService(ServiceType.WAIT_20, false);

        deliveryOptionServices.add(defaultCheck(true));
        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(2);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(service.isCustomerPay()).isTrue();

        service = services.get(1);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.CASH_SERVICE);
        softly.assertThat(service.isCustomerPay()).isFalse();
    }

    @Test
    @DisplayName("Услуга оплачивается магазином, но КД перевыставляет услугу на покупателя (SORT)")
    void senderPaySortServiceWith() {
        addSettingsService(ServiceType.SORT, false);

        deliveryOptionServices.add(
            DeliveryOptionService.builder()
                .code(DeliveryServiceCode.SORT)
                .priceCalculationRule(DeliveryOptionServicePriceRule.FIX)
                .priceCalculationParameter(0)
                .minPrice(0)
                .maxPrice(0)
                .paidByCustomer(true)
                .enabledByDefault(false)
                .build()
        );
        context.shipment(
            new DeliveryOptionShipment(
                LocalDate.now(),
                LocalDate.now(),
                ShipmentType.WITHDRAW,
                PartnerResponse.newBuilder()
                    .partnerType(PartnerType.SORTING_CENTER)
                    .build()
            )
        );

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.SORT);
        softly.assertThat(service.isCustomerPay()).isTrue();
    }

    @Test
    @DisplayName("Услуга оплачивается клиентом")
    void customerPayService() {
        addSettingsService(ServiceType.WAIT_20, true);
        filterCost.setManualDeliveryForCustomer(BigDecimal.ZERO);

        deliveryOptionServices.add(defaultCheck(true));

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(service.isCustomerPay()).isTrue();
    }

    @Test
    @DisplayName("Услуга не может оплачиваться клиентом")
    void nonEditableCustomerPayService() {
        DeliveryTypeServiceSettings deliveryTypeServiceSettings = new DeliveryTypeServiceSettings()
            .setDeliveryType(DELIVERY_TYPE)
            .setServiceType(ServiceType.CASH_SERVICE)
            .setEditableCustomerPay(false)
            .setDefaultCustomerPay(false)
            .setRequired(true);
        deliveryTypeServices.put(DELIVERY_TYPE, List.of(deliveryTypeServiceSettings));
        filterCost.setManualDeliveryForCustomer(BigDecimal.TEN);
        senderDeliveryTypeServices.computeIfAbsent(DELIVERY_TYPE, key -> new ArrayList<>()).add(
            new SenderDeliveryTypeServiceSettings()
                .setDeliveryTypeServiceSettings(deliveryTypeServiceSettings)
                .setCustomerPay(true)
        );

        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.CASH_SERVICE);
        softly.assertThat(service.isCustomerPay()).isFalse();
    }

    @Test
    @DisplayName("Услуга оплачивается согласно настройкам сендера - оплачивает клиент")
    void senderSettingsCustomerPayService() {
        addSettingsService(ServiceType.WAIT_20, true);
        filterCost.setManualDeliveryForCustomer(BigDecimal.ZERO);

        deliveryOptionServices.add(defaultCheck(null));

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(service.isCustomerPay()).isTrue();
    }

    @Test
    @DisplayName("Услуга оплачивается согласно настройкам сендера - оплачивает продавец")
    void senderSettingsSellerPayService() {
        addSettingsService(ServiceType.WAIT_20, false);
        filterCost.setManualDeliveryForCustomer(BigDecimal.ZERO);

        deliveryOptionServices.add(defaultCheck(null));

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(1);
        DeliveryOptionResultService service = services.get(0);
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(service.isCustomerPay()).isFalse();
    }

    @Test
    @DisplayName("Несколько услуг")
    void multipleServices() {
        addSettingsService(ServiceType.WAIT_20, true);
        filterCost.setAssessedValue(BigDecimal.TEN);
        addSettingsService(ServiceType.INSURANCE, false);

        deliveryOptionServices.add(defaultCheck(true));
        deliveryOptionServices.add(defaultInsurance());
        deliveryOptionServices.add(defaultCashService());

        List<DeliveryOptionResultService> services = getServices();
        assertThat(services).hasSize(3);
        DeliveryOptionResultService first = services.get(0);
        softly.assertThat(first.getCode()).isEqualTo(ServiceType.WAIT_20);
        softly.assertThat(first.isCustomerPay()).isTrue();
        DeliveryOptionResultService second = services.get(1);
        softly.assertThat(second.getCode()).isEqualTo(ServiceType.INSURANCE);
        softly.assertThat(second.isCustomerPay()).isFalse();
        DeliveryOptionResultService third = services.get(2);
        softly.assertThat(third.getCode()).isEqualTo(ServiceType.CASH_SERVICE);
        softly.assertThat(third.isCustomerPay()).isFalse();
    }

    @Test
    @DisplayName("Услуга возврат")
    void returnService() {
        addSettingsService(ServiceType.RETURN);

        deliveryOptionModifiedCost = 10_00;
        deliveryOptionServices.add(defaultDelivery(10_00));

        deliveryOptionServices.add(DeliveryOptionService.builder()
            .code(DeliveryServiceCode.RETURN)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_DELIVERY)
            .priceCalculationParameter(0.75)
            .minPrice(0)
            .maxPrice(1000_00)
            .build());

        assertCost(ServiceType.RETURN, new BigDecimal("7.50"));
    }

    @Test
    @DisplayName("Услуга сортировка возврата")
    void returnSortService() {
        addSettingsService(ServiceType.RETURN_SORT, false);

        softly.assertThat(getServices())
            .usingFieldByFieldElementComparator()
            .isEqualTo(List.of(
                DeliveryOptionResultService.builder()
                    .name("Сортировка возврата")
                    .code(ServiceType.RETURN_SORT)
                    .cost(BigDecimal.valueOf(20))
                    .customerPay(false)
                    .enabledByDefault(false)
                    .build()
            ));
    }

    private DeliveryOptionService defaultDelivery(int cost) {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.DELIVERY)
            .priceCalculationRule(DeliveryOptionServicePriceRule.FIX)
            .priceCalculationParameter(cost)
            .minPrice(cost)
            .maxPrice(cost)
            .enabledByDefault(true)
            .paidByCustomer(true)
            .build();
    }

    private DeliveryOptionService defaultInsurance() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.INSURANCE)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_COST)
            .priceCalculationParameter(0.006)
            .minPrice(0)
            .maxPrice(3000_00)
            .enabledByDefault(true)
            .build();
    }

    @Nonnull
    private DeliveryOptionService defaultCheck(@Nullable Boolean paidByCustomer) {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.WAIT_20)
            .priceCalculationRule(DeliveryOptionServicePriceRule.FIX)
            .priceCalculationParameter(30_00)
            .minPrice(30_00)
            .maxPrice(30_00)
            .paidByCustomer(paidByCustomer)
            .enabledByDefault(true)
            .build();
    }

    private DeliveryOptionService defaultCashService() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.CASH_SERVICE)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_CASH)
            .priceCalculationParameter(0.017)
            .minPrice(30_00)
            .maxPrice(1700_00)
            .paidByCustomer(false)
            .enabledByDefault(true)
            .build();
    }

    private void addSettingsService(ServiceType serviceType) {
        addSettingsService(serviceType, false);
    }

    private void addSettingsService(ServiceType serviceType, boolean settingCustomerPay) {
        DeliveryTypeServiceSettings deliveryTypeServiceSettings = new DeliveryTypeServiceSettings()
            .setDeliveryType(DELIVERY_TYPE)
            .setEditableCustomerPay(true)
            .setServiceType(serviceType);
        deliveryTypeServices.computeIfAbsent(DELIVERY_TYPE, key -> new ArrayList<>()).add(deliveryTypeServiceSettings);

        senderDeliveryTypeServices.computeIfAbsent(DELIVERY_TYPE, key -> new ArrayList<>()).add(
            new SenderDeliveryTypeServiceSettings()
                .setDeliveryTypeServiceSettings(deliveryTypeServiceSettings)
                .setCustomerPay(settingCustomerPay)
        );
    }

    private void assertCashService(DeliveryOptionResultService service, String cost) {
        softly.assertThat(service.getCode()).isEqualTo(ServiceType.CASH_SERVICE);
        softly.assertThat(service.isCustomerPay()).isFalse();
        softly.assertThat(service.getCost()).isEqualByComparingTo(new BigDecimal(cost));
    }

    private void assertCost(@Nonnull ServiceType serviceType, @Nullable BigDecimal cost) {
        List<DeliveryOptionResultService> services = getServices();
        if (cost == null || BigDecimal.ZERO.equals(cost)) {
            softly.assertThat(services).isEmpty();
        } else {
            assertThat(services).hasSize(1);
            DeliveryOptionResultService service = services.get(0);
            softly.assertThat(service.getCode()).isEqualTo(serviceType);
            softly.assertThat(service.getCost()).isEqualByComparingTo(cost);
        }
    }

    private List<DeliveryOptionResultService> getServices() {
        context
            .fixedAssessedValue(filterCost.getAssessedValue())
            .deliveryType(DELIVERY_TYPE)
            .deliveryOption(
                DeliveryOption.builder()
                    .services(deliveryOptionServices)
                    .cost(deliveryOptionModifiedCost)
                    .build()
            );
        return new ServicesCalculator(
            filterCost,
            deliveryTypeServices,
            senderDeliveryTypeServices,
            enumConverter,
            servicesCostCalculator,
            costCalculator
        )
            .getServices(context.build());
    }

}
