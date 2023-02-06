package ru.yandex.market.logistics.lom.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.ChangeOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.model.dto.OrderActionsDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.utils.WaybillSegmentFactory;
import ru.yandex.market.logistics.lom.validators.UpdatePlacesValidator;

import static ru.yandex.market.logistics.lom.entity.enums.OrderStatus.CANCELLED;

@DisplayName("Конвертация доступных для заказа действий")
class OrderAvailableActionsConverterTest extends AbstractTest {

    private final OrderAvailableActionsConverter availableActionsConverter = new OrderAvailableActionsConverter(
        new UpdatePlacesValidator()
    );

    private static final long PARTNER_ID = 12L;

    @MethodSource("recipientUpdateArgumentsWithoutLmsCalling")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Проверка признака возможности редактирования получателя в заказе без вызова функции")
    void recipientUpdate(
        String displayName,
        Order order,
        Set<OptionalOrderPart> optionalOrderParts,
        Boolean recipientUpdate
    ) {
        OrderActionsDto result = availableActionsConverter.toAvailableActions(
            order,
            optionalOrderParts,
            OrderAvailableActionsConverterTest::emptyAction
        );
        softly.assertThat(result.getUpdateRecipient()).isEqualTo(recipientUpdate);
    }

    @Nonnull
    private static Stream<Arguments> recipientUpdateArgumentsWithoutLmsCalling() {
        return Stream.of(
            Arguments.of(
                "Есть активная заявка на изменение данных получателя",
                createPrefilledOrderBuilder()
                    .changeOrderRequests(Set.of(
                        new ChangeOrderRequest()
                            .setRequestType(ChangeOrderRequestType.RECIPIENT)
                            .setStatus(ChangeOrderRequestStatus.PROCESSING)))
                    .build(),
                OptionalOrderPart.ALL,
                false
            ),
            Arguments.of(
                "Статус заказа неподходящий",
                createPrefilledOrderBuilder().status(CANCELLED).build(),
                OptionalOrderPart.ALL,
                false
            ),
            Arguments.of(
                "Отсутствует UPDATE_RECIPIENT_ENABLED в optionalParts",
                createPrefilledOrderBuilder().build(),
                Set.of(),
                null
            )
        );
    }

    @Test
    @DisplayName("Все условия корректны, у партнёра есть метод")
    void correctForRecipientUpdate() {
        OrderActionsDto result = availableActionsConverter.toAvailableActions(
            createPrefilledOrderBuilder().build(),
            OptionalOrderPart.ALL,
            OrderAvailableActionsConverterTest::partnerWithMethod
        );
        softly.assertThat(result.getUpdateRecipient()).isTrue();
    }

    @Test
    @DisplayName("Все условия корректны, у партнёра нет метода")
    void recipientUpdateWithoutPartnerSettingMethod() {
        OrderActionsDto result = availableActionsConverter.toAvailableActions(
            createPrefilledOrderBuilder().build(),
            OptionalOrderPart.ALL,
            OrderAvailableActionsConverterTest::partnerWithoutMethod
        );
        softly.assertThat(result.getUpdatePlaces()).isTrue();
    }

    @Test
    @DisplayName("Обновление грузомест доступно")
    void updatePlacesAvailable() {
        OrderActionsDto result = availableActionsConverter.toAvailableActions(
            createPrefilledOrderBuilder().build(),
            Set.of(),
            OrderAvailableActionsConverterTest::partnerWithMethod
        );
        softly.assertThat(result.getUpdatePlaces()).isTrue();
    }

    @Test
    @DisplayName("Обновление грузомест недоступно - заказ отгружен")
    void updatePlacesUnavailableOrderShipped() {
        WaybillSegment waybillSegment = new WaybillSegment()
            .setPartnerId(PARTNER_ID)
            .setPartnerType(PartnerType.SORTING_CENTER)
            .addTag(WaybillSegmentTag.DIRECT);

        WaybillSegmentFactory.writeWaybillSegmentCheckpoint(
            waybillSegment,
            SegmentStatus.IN,
            WaybillSegmentFactory.FIXED_TIME
        );

        OrderActionsDto result = availableActionsConverter.toAvailableActions(
            createPrefilledOrderBuilder()
                .waybill(List.of(waybillSegment))
                .status(OrderStatus.ENQUEUED)
                .build(),
            Set.of(),
            OrderAvailableActionsConverterTest::partnerWithMethod
        );
        softly.assertThat(result.getUpdatePlaces()).isFalse();
    }

    @Test
    @DisplayName("Обновление грузомест недоступно - пустой waybill")
    void updatePlacesUnavailableEmptyWaybill() {
        OrderActionsDto result = availableActionsConverter.toAvailableActions(
            Order.builder()
                .status(OrderStatus.ENQUEUED)
                .build(),
            Set.of(),
            OrderAvailableActionsConverterTest::partnerWithMethod
        );
        softly.assertThat(result.getUpdatePlaces()).isFalse();
    }

    @Nonnull
    private static Order.OrderBuilder createPrefilledOrderBuilder() {
        return Order.builder()
            .waybill(List.of(new WaybillSegment().setPartnerId(PARTNER_ID).setPartnerType(PartnerType.DELIVERY)))
            .status(OrderStatus.ENQUEUED);
    }

    @Nullable
    private static Boolean emptyAction(Set<OptionalOrderPart> orderParts, Long partnerId) {
        return null;
    }

    @Nonnull
    private static Boolean partnerWithMethod(Set<OptionalOrderPart> orderParts, Long partnerId) {
        return true;
    }

    @Nonnull
    private static Boolean partnerWithoutMethod(Set<OptionalOrderPart> orderParts, Long partnerId) {
        return false;
    }
}
