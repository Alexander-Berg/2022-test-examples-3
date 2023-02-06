package ru.yandex.market.checkout.checkouter.test.config.services;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.common.common.ClassMapping;
import ru.yandex.market.checkout.common.xml.ErrorCodeExceptionXmlSerializer;
import ru.yandex.market.checkout.common.xml.NewClassMappingXmlMessageConverter;
import ru.yandex.market.checkout.common.xml.XmlDeserializer;
import ru.yandex.market.checkout.common.xml.XmlSerializer;
import ru.yandex.market.checkout.config.PushApiClassMappingsConfig;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.xml.CartXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.CourierXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.DeliveryXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.OrderXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.ParcelXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.PushApiDeliveryXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.PushApiOrderXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.ShopErrorCodeExceptionXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.ShopErrorExceptionXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.StocksRequestXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.StocksResponseXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.AddressXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.ItemParameterXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.OrderItemXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.order.UnitValueXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.shop.FeaturesXmlSerializer;
import ru.yandex.market.checkout.pushapi.client.xml.shop.SettingsXmlSerializer;
import ru.yandex.market.checkout.pushapi.in.xml.CartResponseXmlSerializer;
import ru.yandex.market.checkout.pushapi.in.xml.DeliveryResponseXmlSerializer;
import ru.yandex.market.checkout.pushapi.in.xml.OrderResponseXmlSerializer;

@Configuration
public class IntTestSerializersConfig {

    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ClassMapping<XmlSerializer> testingPushApiXmlSerializers(
            OrderItemXmlSerializer orderItemXmlSerializer,
            AddressXmlSerializer addressXmlSerializer,
            ParcelXmlSerializer parcelXmlSerializer,
            DeliveryXmlSerializer<Delivery> deliveryXmlSerializer,
            CourierXmlSerializer courierXmlSerializer,
            ItemParameterXmlSerializer itemParameterXmlSerializer,
            UnitValueXmlSerializer unitValueXmlSerializer,
            CartXmlSerializer cartXmlSerializer,
            OrderXmlSerializer orderXmlSerializer,
            PushApiOrderXmlSerializer pushApiOrderXmlSerializer,
            PushApiDeliveryXmlSerializer pushApiDeliveryXmlSerializer,
            SettingsXmlSerializer settingsXmlSerializer,
            FeaturesXmlSerializer featuresXmlSerializer,
            ShopErrorCodeExceptionXmlSerializer shopErrorCodeExceptionXmlSerializer,
            ShopErrorExceptionXmlSerializer shopErrorExceptionXmlSerializer,
            StocksRequestXmlSerializer stocksRequestXmlSerializer,
            StocksResponseXmlSerializer stocksResponseXmlSerializer,
            ErrorCodeExceptionXmlSerializer errorCodeExceptionSerializer,
            CartResponseXmlSerializer cartResponseInXmlSerializer,
            OrderResponseXmlSerializer orderResponseInXmlSerializer
    ) {
        Map<Class, XmlSerializer> map = new LinkedHashMap<>(PushApiClassMappingsConfig.serializerMappings(
                orderItemXmlSerializer,
                addressXmlSerializer,
                parcelXmlSerializer,
                deliveryXmlSerializer,
                courierXmlSerializer,
                itemParameterXmlSerializer,
                unitValueXmlSerializer,
                cartXmlSerializer,
                orderXmlSerializer,
                pushApiOrderXmlSerializer,
                pushApiDeliveryXmlSerializer,
                settingsXmlSerializer,
                featuresXmlSerializer,
                shopErrorCodeExceptionXmlSerializer,
                shopErrorExceptionXmlSerializer,
                stocksRequestXmlSerializer,
                stocksResponseXmlSerializer,
                errorCodeExceptionSerializer
        ));
        map.put(CartResponse.class, cartResponseInXmlSerializer);
        map.put(OrderResponse.class, orderResponseInXmlSerializer);

        ClassMapping<XmlSerializer> classMapping = new ClassMapping<>();
        classMapping.setMapping(map);
        return classMapping;
    }

    @Bean
    public DeliveryResponseXmlSerializer deliveryResponseXmlSerializer() {
        return new DeliveryResponseXmlSerializer();
    }

    @Bean
    public CartResponseXmlSerializer cartResponseXmlSerializer(
            DeliveryResponseXmlSerializer deliveryResponseXmlSerializer
    ) {
        CartResponseXmlSerializer serializer = new CartResponseXmlSerializer();
        serializer.setDeliveryResponseXmlSerializer(deliveryResponseXmlSerializer);
        return serializer;
    }

    @Bean
    public OrderResponseXmlSerializer orderResponseXmlSerializer() {
        return new OrderResponseXmlSerializer();
    }

    @Bean
    public NewClassMappingXmlMessageConverter pushApiTestMappingXmlMessageConverter(
            ClassMapping<ObjectFactory<XmlDeserializer>> pushApiXmlDeserializers,
            ClassMapping<XmlSerializer> testingPushApiXmlSerializers
    ) {
        NewClassMappingXmlMessageConverter newClassMappingXmlMessageConverter =
                new NewClassMappingXmlMessageConverter();
        newClassMappingXmlMessageConverter.setDeserializers(pushApiXmlDeserializers);
        newClassMappingXmlMessageConverter.setSerializers(testingPushApiXmlSerializers);
        return newClassMappingXmlMessageConverter;
    }
}
