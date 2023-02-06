package factory.orderservice;

import java.time.OffsetDateTime;

import toolkit.RandomUtil;

import ru.yandex.market.order_service.client.model.CreateExternalOrderRequest;
import ru.yandex.market.order_service.client.model.DeliveryOptionDto;
import ru.yandex.market.order_service.client.model.ExternalBuyerDto;
import ru.yandex.market.order_service.client.model.OrderSourcePlatform;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum OrderCreationRequest {
    DEFAULT {
        @Override
        public CreateExternalOrderRequest getRequest(DeliveryOptionDto option) {
            return new CreateExternalOrderRequest()
                .startTime(OffsetDateTime.now())
                .address(DeliveryAddress.DEFAULT.getAddress())
                .buyer(
                    new ExternalBuyerDto()
                        .firstName("Имя")
                        .lastName("Фамилия")
                        .middleName("Отчество")
                        .phone("+78005553535")
                        .email("test@test.ru")
                )
                .option(option)
                .items(Items.DEFAULT.getItems())
                .merchantOrderId(RandomUtil.randomStringNumbersOnly(32))
                .sourcePlatform(OrderSourcePlatform.OTHER);
        }
    },
    ;

    public abstract CreateExternalOrderRequest getRequest(DeliveryOptionDto option);
}
