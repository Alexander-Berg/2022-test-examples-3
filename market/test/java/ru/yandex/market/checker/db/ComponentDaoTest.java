package ru.yandex.market.checker.db;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.api.model.Component;
import ru.yandex.market.checker.api.model.ComponentRequestBody;
import ru.yandex.market.checker.matchers.ComponentMatchers;
import ru.yandex.market.checker.model.SortType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ParametersAreNonnullByDefault
class ComponentDaoTest extends FunctionalTest {

    @Autowired
    private ComponentDao componentDao;

    @Test
    @DbUnitDataSet(before = "getAllComponents.before.csv")
    @DisplayName("Получаем все компоненты с 1й страницы")
    void test_getFirstPageComponents() {
        List<Component> components = componentDao.getComponents(0, 50, SortType.ASC);

        assertEquals(components.size(), 3);
    }

    @Test
    @DbUnitDataSet(before = "getAllComponents.before.csv")
    @DisplayName("Не получаем компоненты тк неправильная страница")
    void test_getSecondPageComponents() {
        List<Component> components = componentDao.getComponents(1, 50, SortType.ASC);

        assertEquals(components.size(), 0);
    }

    @Test
    @DbUnitDataSet(after = "createComponent.after.csv")
    @DisplayName("Создать запись в таблице компонентов и положить туда список ответственных")
    void test_createComponent() {
        Component created = componentDao.createOrUpdateComponent(
                null,
                new ComponentRequestBody().name("mbi")
                        .addResponsibleListItem("ivanov-ivan")
                        .addResponsibleListItem("petrov-petr")
        );
        assertThat(created,
                allOf(
                        ComponentMatchers.hasId(1L),
                        ComponentMatchers.hasName("mbi"),
                        ComponentMatchers.hasResponsibleLogin(List.of("ivanov-ivan", "petrov-petr"))
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "updateComponent.before.csv",
            after = "createComponent.after.csv"
    )
    @DisplayName("Обновить запись в таблице компонентов")
    void test_updateComponent() {
        Component updated = componentDao.createOrUpdateComponent(1L,
                new ComponentRequestBody().name("mbi")
                        .addResponsibleListItem("ivanov-ivan")
                        .addResponsibleListItem("petrov-petr")
        );
        assertThat(updated, allOf(
                ComponentMatchers.hasId(1L),
                ComponentMatchers.hasName("mbi"),
                ComponentMatchers.hasResponsibleLogin(List.of("ivanov-ivan", "petrov-petr"))
        ));
    }
}
