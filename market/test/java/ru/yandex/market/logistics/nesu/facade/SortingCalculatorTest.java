package ru.yandex.market.logistics.nesu.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionCost;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryOptionTag;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.SortingCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryInterval;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionContext;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliverySchedule;

@DisplayName("Расчет самого быстрого, самого дешевого и оптимального вариантов")
class SortingCalculatorTest extends AbstractTest {

    private SortingCalculator calculator = new SortingCalculator();

    @Test
    @DisplayName("Пустой список вариантов")
    void emptyList() {
        calculator.applyTags(List.of());
    }

    @Test
    @DisplayName("Единственный вариант оптимальный")
    void singleResult() {
        DeliveryOptionContext entry = context(0, 1, null);

        softly.assertThat(calculator.applyTags(List.of(entry)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(DeliveryOptionTag.OPTIMAL);
    }

    @Test
    @DisplayName("Разные по сроку и стоимости варианты")
    void differentResults() {
        DeliveryOptionContext first = context(1, 100, null);
        DeliveryOptionContext second = context(10, 10, null);
        DeliveryOptionContext third = context(100, 1, null);

        softly.assertThat(calculator.applyTags(List.of(first, second, third)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(DeliveryOptionTag.CHEAPEST, null, DeliveryOptionTag.FASTEST);
    }

    @Test
    @DisplayName("Отличие в стоимости")
    void costDifference() {
        DeliveryOptionContext first = context(10, 10, null);
        DeliveryOptionContext second = context(1, 10, null);

        softly.assertThat(calculator.applyTags(List.of(first, second)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(null, DeliveryOptionTag.OPTIMAL);
    }

    @Test
    @DisplayName("Отличие в начале срока")
    void daysFromDifference() {
        DeliveryOptionContext first = context(10, 10, 10, null);
        DeliveryOptionContext second = context(10, 1, 10, null);

        softly.assertThat(calculator.applyTags(List.of(first, second)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(null, DeliveryOptionTag.OPTIMAL);
    }

    @Test
    @DisplayName("Отличие в конце срока")
    void daysToDifference() {
        DeliveryOptionContext first = context(10, 10, 10, null);
        DeliveryOptionContext second = context(10, 10, 1, null);

        softly.assertThat(calculator.applyTags(List.of(first, second)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(null, DeliveryOptionTag.OPTIMAL);
    }

    @Test
    @DisplayName("Отличие в рейтинге службы")
    void ratingDifference() {
        DeliveryOptionContext first = context(10, 10, 1);
        DeliveryOptionContext second = context(10, 10, 10);

        softly.assertThat(calculator.applyTags(List.of(first, second)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(null, DeliveryOptionTag.OPTIMAL);
    }

    @Test
    @DisplayName("Отличие в отсутствии рейтинга службы")
    void ratingNullDifference() {
        DeliveryOptionContext first = context(10, 10, null);
        DeliveryOptionContext second = context(10, 10, 10);

        softly.assertThat(calculator.applyTags(List.of(first, second)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(null, DeliveryOptionTag.OPTIMAL);
    }

    @Test
    @DisplayName("Нет отличий")
    void noDifference() {
        DeliveryOptionContext first = context(10, 10, 10);
        DeliveryOptionContext second = context(10, 10, 10);

        softly.assertThat(calculator.applyTags(List.of(first, second)))
            .extracting(DeliveryOptionContext::getTag)
            .containsExactly(DeliveryOptionTag.OPTIMAL, null);
    }

    @Nonnull
    private DeliveryOptionContext context(long cost, int days, Integer rating) {
        return context(cost, days, days, rating);
    }

    @Nonnull
    private DeliveryOptionContext context(long cost, int daysFrom, int daysTo, Integer rating) {
        LocalDate shipmentDate = LocalDate.of(2020, 2, 20);
        return DeliveryOptionContext.builder()
            .cost(DeliveryOptionCost.builder()
                .deliveryForCustomer(BigDecimal.valueOf(cost))
                .build())
            .deliverySchedule(DeliverySchedule.builder()
                .deliveryInterval(new DeliveryInterval(
                    shipmentDate.plusDays(daysFrom),
                    shipmentDate.plusDays(daysTo)
                ))
                .build())
            .deliveryService(PartnerResponse.newBuilder()
                .id(0L)
                .marketId(0L)
                .name("Partner")
                .rating(rating)
                .build())
            .build();
    }

}
