package ru.yandex.market.vendors.analytics.core.service.widget;

import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.jpa.entity.dashboard.CalculationInfo;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
public class WidgetCategoryExtractorTest extends FunctionalTest {

    @Autowired
    private WidgetCategoryExtractor widgetCategoryExtractor;

    @ParameterizedTest
    @MethodSource("checkAllWidgetsHasApplicableStrategiesParams")
    @DisplayName("Каждый тип виджета имеет ровно одну стратегию определения айдишника категории")
    void checkAllWidgetsHasApplicableStrategies(WidgetType widgetType) {
        var calculationInfo = mock(CalculationInfo.class);
        when(calculationInfo.getType()).thenReturn(widgetType);
        var applicableStrategies = widgetCategoryExtractor.findApplicableStrategies(calculationInfo);
        assertEquals(1, applicableStrategies.size());
    }

    private static Stream<Arguments> checkAllWidgetsHasApplicableStrategiesParams() {
        return StreamEx.of(WidgetType.values())
                .remove(WidgetType.COMPARE::equals)
                .map(Arguments::of);
    }
}
