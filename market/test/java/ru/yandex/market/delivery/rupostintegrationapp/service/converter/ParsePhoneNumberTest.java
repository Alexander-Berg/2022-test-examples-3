package ru.yandex.market.delivery.rupostintegrationapp.service.converter;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import steps.ordersteps.OrderSteps;
import steps.ordersteps.ordersubsteps.PhoneSteps;

import ru.yandex.market.delivery.entities.common.Order;
import ru.yandex.market.delivery.entities.common.Phone;
import ru.yandex.market.delivery.entities.request.ds.DsCreateOrderRequest;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.russianpostapiclient.bean.createorder.CreateOrderRequest;

class ParsePhoneNumberTest extends BaseTest {
    private CreateOrderRequestConverter createOrderRequestConverter = new CreateOrderRequestConverter();
    private DsCreateOrderRequest dsCreateOrderRequest = new DsCreateOrderRequest();
    private DsCreateOrderRequest.RequestContent requestContent = dsCreateOrderRequest.new RequestContent();
    private Order order = OrderSteps.getOrder();

    static Stream<Arguments> getParameters() {
        return Stream.of(
            Arguments.of("+7 (123) 123-23-23", 71231232323L),
            Arguments.of("+8 (123) 123-23-23", 81231232323L),
            Arguments.of("7 (123) 123-23-23", 71231232323L),
            Arguments.of("8 (123) 123-23-23", 81231232323L),

            Arguments.of("+7 (123) 123 - 23 - 23", 71231232323L),
            Arguments.of("+8 (123) 123 - 23 - 23", 81231232323L),
            Arguments.of("7 (123) 123 - 23 - 23", 71231232323L),
            Arguments.of("8 (123) 123 - 23 - 23", 81231232323L),

            Arguments.of("+7 (123) 123 23 23", 71231232323L),
            Arguments.of("+8 (123) 123 23 23", 81231232323L),
            Arguments.of("8 (123) 123 23 23", 81231232323L),
            Arguments.of("7 (123) 123 23 23", 71231232323L),

            Arguments.of("+7(123)123 23 23", 71231232323L),
            Arguments.of("+8(123)123 23 23", 81231232323L),
            Arguments.of("7(123)123 23 23", 71231232323L),
            Arguments.of("8(123)123 23 23", 81231232323L),

            Arguments.of("+7(123)1232323", 71231232323L),
            Arguments.of("+8(123)1232323", 81231232323L),
            Arguments.of("7(123)1232323", 71231232323L),
            Arguments.of("8(123)1232323", 81231232323L),

            Arguments.of("+7(123)123-23-23", 71231232323L),
            Arguments.of("+8(123)123-23-23", 81231232323L),
            Arguments.of("8(123)123-23-23", 81231232323L),
            Arguments.of("7(123)123-23-23", 71231232323L),

            Arguments.of("+7 123 123-23-23", 71231232323L),
            Arguments.of("+8 123 123-23-23", 81231232323L),
            Arguments.of("8 123 123-23-23", 81231232323L),
            Arguments.of("7 123 123-23-23", 71231232323L),

            Arguments.of("+7 123 123 23 23", 71231232323L),
            Arguments.of("+8 123 123 23 23", 81231232323L),
            Arguments.of("8 123 123 23 23", 81231232323L),
            Arguments.of("7 123 123 23 23", 71231232323L),

            Arguments.of("+7-123-123-23-23", 71231232323L),
            Arguments.of("+8-123-123-23-23", 81231232323L),
            Arguments.of("7-123-123-23-23", 71231232323L),
            Arguments.of("8-123-123-23-23", 81231232323L),

            Arguments.of(" +7 123 123 23 23 ", 71231232323L),
            Arguments.of("  +8 123 123 23 23  ", 81231232323L),
            Arguments.of(" 8-123-123-23-23 ", 81231232323L),
            Arguments.of(" 7 (123) 123-23-23 ", 71231232323L),

            Arguments.of("+71231232323", 71231232323L),
            Arguments.of("+81231232323", 81231232323L),
            Arguments.of("71231232323", 71231232323L),
            Arguments.of("81231232323", 81231232323L),

            Arguments.of("0-123-123-23-23", 0L),
            Arguments.of("alkfjhipj", 0L),
            Arguments.of("123-23-23", 0L),
            Arguments.of("8!123@123#23$23", 0L)
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void setPhoneNumberTest(String phoneNumber, long requestPhoneNumber) {
        List<Phone> phones = PhoneSteps.getPhoneList(phoneNumber);
        order.getRecipient().setPhones(phones);
        requestContent.setOrder(order);
        dsCreateOrderRequest.setRequestContent(requestContent);

        CreateOrderRequest request = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(request.getRecipientPhone())
            .as("Wrong parse number " + phoneNumber)
            .isEqualTo(requestPhoneNumber);
    }
}
