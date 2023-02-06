package ru.yandex.market.vendors.analytics.platform.facade;

import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.core.jpa.entity.WidgetGroupEntity;
import ru.yandex.market.vendors.analytics.core.jpa.repository.WidgetGroupRepository;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Тесты фасада {@link WidgetFacade}.
 *
 * @author ogonek
 */
public class WidgetFacadeTest extends FunctionalTest {

    @Autowired
    private WidgetGroupRepository widgetGroupRepository;

    @Test
    @Disabled
    @DisplayName("Каждый тип виджета входит хотя бы в 1 группу виджетов")
    void allWidgetsHaveGroups() {
        List<WidgetType> allWidgetTypes = StreamEx.of(WidgetType.values())
                .remove(WidgetType.COMPARE::equals)
                .toList();
        Set<WidgetType> widgetsWithGroup = StreamEx.of(widgetGroupRepository.findAll())
                .map(WidgetGroupEntity::getWidgetType)
                .toSet();
        Assertions.assertTrue(widgetsWithGroup.containsAll(allWidgetTypes));
    }

}
