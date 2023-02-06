package step;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import client.CombinatorClient;
import com.fasterxml.jackson.databind.JsonNode;
import dto.responses.combinator.YcomboParameters;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

@Slf4j
public class CombinatorSteps {

    private static final CombinatorClient COMBINATOR = new CombinatorClient();

    @Step("Проверяем disabled_dates в маршруте")
    public void verifyDateIsDisabledInRoute(YcomboParameters ycomboParameters, LocalDate date) {
        Retrier.retry(() -> {
                Set<Integer> disabledDaysOfYear = getDisabledDaysOfYear(ycomboParameters);
                Assertions.assertTrue(
                    disabledDaysOfYear.contains(date.getDayOfYear()),
                    String.format("Ни на одном сервисе из маршрута disabled_dates не содержит %s", date)
                );
            }
        );
    }

    @Step("Проверяем disabled_dates в маршруте")
    public void verifyDateIsEnabledInRoute(YcomboParameters ycomboParameters, LocalDate date) {
        Retrier.retry(() -> {
                Set<Integer> disabledDaysOfYear = getDisabledDaysOfYear(ycomboParameters);
                Assertions.assertFalse(
                    disabledDaysOfYear.contains(date.getDayOfYear()),
                    String.format("На одном из сервисов маршрута disabled_dates содержит %s", date)
                );
            }
        );
    }

    @Nonnull
    private Set<Integer> getDisabledDaysOfYear(YcomboParameters ycomboParameters) {
        List<JsonNode> routes = COMBINATOR.ycombo(ycomboParameters).getRoutes();
        Assertions.assertFalse(routes.isEmpty(), "Комбинатор не вернул ни одного маршрута");

        return StreamEx.of(routes)
            .flatMap(route -> StreamEx.of(route.get("points").iterator()))
            .flatMap(point -> StreamEx.of(point.get("services").iterator()))
            .flatMap(
                service -> Optional.ofNullable(service.get("disabled_dates"))
                    .map(JsonNode::iterator)
                    .map(StreamEx::of)
                    .orElseGet(StreamEx::empty)
            )
            .map(JsonNode::asText)
            .map(Integer::parseInt)
            .collect(Collectors.toSet());
    }
}
