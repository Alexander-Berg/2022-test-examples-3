package ru.yandex.market.tpl.core.domain.ds;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.OrderTransferCode;
import ru.yandex.market.logistic.api.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.api.model.delivery.Order;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.Service;
import ru.yandex.market.logistic.api.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties.B2B_CUSTOMER_TYPE;


class TplDsOrderPropertiesResolverUnitTest {

    public static final String ORDER_TRANSFER_VERIFICATION_CODE = "CODE";
    private final TplDsOrderPropertiesResolver orderPropertiesResolver = new TplDsOrderPropertiesResolver();

    @DisplayName("Приходит свойство для примерки заказа из DS Api")
    @Test
    void has_TryingProperty_fromDSApiRequest() {
        //given
        ResourceId tryingResource = new ResourceId.ResourceIdBuilder()
                .setYandexId(TplDsConstants.Service.FASHION_SERVICE)
                .build();
        Service tryingService = new Service.ServiceBuilder(tryingResource, false).build();

        Order request = new OrderBuilder()
                .setServices(List.of(tryingService))
                .build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(request, null);

        //then
        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.IS_TRYING_AVAILABLE);
    }

    @DisplayName("Не приходит свойство для примерки заказа из DS Api")
    @Test
    void hasNot_TryingProperty_fromDSApiRequest() {
        //given
        Order request = new OrderBuilder().build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(request, null);

        //then
        assertThat(resolvedProperties).doesNotContainKey(TplOrderProperties.Names.IS_TRYING_AVAILABLE);
    }

    @DisplayName("Приходит свойство для примерки закза из Order Generator")
    @Test
    void has_TryingProperty_fromOrderGeneratorRequest() {
        //given
        OrderGenerateService.OrderGenerateParam fashionParam = OrderGenerateService.OrderGenerateParam.builder()
                .items(
                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                .isFashion(true)
                                .build()
                )
                .build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(fashionParam);

        //then
        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.IS_TRYING_AVAILABLE);
    }

    @DisplayName("Не приходит свойство для примерки закза из Order Generator")
    @Test
    void hasNot_TryingProperty_fromOrderGeneratorRequest() {
        //given
        OrderGenerateService.OrderGenerateParam notFashionParam = OrderGenerateService.OrderGenerateParam.builder()
                .items(
                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                .isFashion(false)
                                .build()
                )
                .build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(notFashionParam);

        //then
        assertThat(resolvedProperties).doesNotContainKey(TplOrderProperties.Names.IS_TRYING_AVAILABLE);
    }

    @DisplayName("Приходит тип клиента для доставки из DS Api")
    @Test
    void hasB2bCustomerTagFromDSApiRequest() {
        Order request = new OrderBuilder()
                .setTags(Set.of(TplDsConstants.Tag.B2B_CUSTOMER))
                .build();

        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(request, null);

        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.CUSTOMER_TYPE);
        assertThat(resolvedProperties.get(TplOrderProperties.Names.CUSTOMER_TYPE))
                .isEqualTo(B2B_CUSTOMER_TYPE.getDefaultValue());
    }

    @DisplayName("Не приходит тип клиента для доставки из DS Api")
    @Test
    void hasNotB2bCustomerTagFromDSApiRequest() {
        Order request = new OrderBuilder().build();

        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(request, null);

        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.CUSTOMER_TYPE);
        assertThat(resolvedProperties).doesNotContainValue(TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name());
    }

    @DisplayName("Приходит код выдачи заказа из DS Api")
    @Test
    void hasOrderCodeFromDsApiRequest() {
        CreateOrderRestrictedData restrictedData = getCreateOrderRestrictedData();

        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(null,
                restrictedData);

        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING);
        assertThat(resolvedProperties.get(TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING))
                .isEqualTo(ORDER_TRANSFER_VERIFICATION_CODE);
    }

    @DisplayName("Не приходит код выдачи заказа из DS Api")
    @Test
    void hasNotOrderCodeFromDsApiRequest() {
        CreateOrderRestrictedData restrictedData = CreateOrderRestrictedData.builder().build();

        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(null,
                restrictedData);

        assertThat(resolvedProperties).doesNotContainKey(TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING);
    }

    @DisplayName("Приходит из ds-api признак вскрытия")
    @Test
    void has_Unboxing_fromDSApiRequest() {
        //given
        ResourceId unboxingResource = new ResourceId.ResourceIdBuilder()
                .setYandexId(TplDsConstants.Service.UNBOXING_SERVICE)
                .build();
        Service unboxingService = new Service.ServiceBuilder(unboxingResource, false).build();

        Order request = new OrderBuilder()
                .setServices(List.of(unboxingService))
                .build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(request, null);

        //then
        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.IS_UNBOXING_AVAILABLE);
    }

    @DisplayName("Приходит свойство для вскрытия закза из Order Generator")
    @Test
    void has_Unboxing_fromOrderGeneratorRequest() {
        //given
        OrderGenerateService.OrderGenerateParam fashionParam = OrderGenerateService.OrderGenerateParam.builder()
                .properties(Map.of(
                                TplOrderProperties.Names.IS_UNBOXING_AVAILABLE.name(),
                                new OrderProperty(
                                        null,
                                        TplPropertyType.STRING,
                                        TplOrderProperties.Names.IS_UNBOXING_AVAILABLE.name(),
                                        TplOrderProperties.IS_UNBOXING_AVAILABLE.print(true))
                        )
                )
                .build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(fashionParam);

        //then
        assertThat(resolvedProperties).containsKey(TplOrderProperties.Names.IS_UNBOXING_AVAILABLE);
    }

    @DisplayName("Не приходит свойство для вскрытия закза из Order Generator")
    @Test
    void hasNot_UnboxingProperty_fromOrderGeneratorRequest() {
        //given
        OrderGenerateService.OrderGenerateParam notFashionParam = OrderGenerateService.OrderGenerateParam.builder().build();

        //when
        Map<TplOrderProperties.Names, String> resolvedProperties = orderPropertiesResolver.resolve(notFashionParam);

        //then
        assertThat(resolvedProperties).doesNotContainKey(TplOrderProperties.Names.IS_UNBOXING_AVAILABLE);
    }

    private CreateOrderRestrictedData getCreateOrderRestrictedData() {
        return CreateOrderRestrictedData.builder()
                .setTransferCodes(
                        new OrderTransferCodes.OrderTransferCodesBuilder()
                                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder()
                                        .setVerification(ORDER_TRANSFER_VERIFICATION_CODE)
                                        .build())
                                .build()
                )
                .build();
    }

    @Data
    private static class OrderBuilder {
        private List<Service> services;
        private Set<String> tags;

        public OrderBuilder() {
        }

        public OrderBuilder setServices(List<Service> services) {
            this.services = services;
            return this;
        }

        public OrderBuilder setTags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public Order build() {
            return new Order.OrderBuilder(null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null, null, null)
                    .setServices(services)
                    .setTags(tags)
                    .build();
        }
    }
}
