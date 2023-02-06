package ru.yandex.market.api.partner.controllers.feedback.converter;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.api.partner.controllers.feedback.dto.type.FeedbackCommentAuthorType;
import ru.yandex.market.api.partner.controllers.feedback.dto.type.FeedbackDeliveryType;
import ru.yandex.market.api.partner.controllers.feedback.dto.type.FeedbackStateType;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.GradeState;
import ru.yandex.market.pers.qa.client.model.UserType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты для конвертации типов enum'ов, полученных от PERS.
 *
 * @author Vladislav Bauer
 */
class PersEnumConversationTest {

    @ParameterizedTest
    @EnumSource(Delivery.class)
    @DisplayName("Проверить соответствие типов доставки отзыва")
    void testConvertFeedbackDeliveryType(final Delivery delivery) {
        assertThat(FeedbackDeliveryType.convert(delivery), notNullValue());
    }

    @ParameterizedTest
    @EnumSource(GradeState.class)
    @DisplayName("Проверить соответствие статусов отзыва")
    void testConvertFeedbackStateType(final GradeState gradeState) {
        assertThat(FeedbackStateType.convert(gradeState), notNullValue());
    }

    @Test
    @DisplayName("Проверить соответствие статусов комментария")
    void testConvertFeedbackCommentAuthorType() {
        final List<UserType> mappedTypes = Arrays.asList(UserType.YANDEXUID, UserType.UID, UserType.SHOP);
        mappedTypes.forEach(type -> assertThat(FeedbackCommentAuthorType.convert(type), notNullValue()));

        final List<UserType> unmappedTypes = ListUtils.subtract(Arrays.asList(UserType.values()), mappedTypes);
        unmappedTypes.forEach(
                type -> Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> Assertions.fail(String.valueOf(FeedbackCommentAuthorType.convert(type)))
                )
        );
    }

}
