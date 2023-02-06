package ru.yandex.market.logistics.management.controller.health;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;

@DatabaseSetup("/data/controller/health/polygonal/before/prepare.xml")
public class YtPolygonalZonesTest extends AbstractContextualTest {
    @Test
    @ExpectedDatabase(
        value = "/data/controller/health/polygonal/after/insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successInsert() {
        // ничего не делаем - данные появляются после настройки базы
    }

    @Test
    @DatabaseSetup(value = "/data/controller/health/polygonal/before/update.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/data/controller/health/polygonal/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successUpdate() {
        // ничего не делаем - данные появляются после настройки базы
    }

    @Test
    @DatabaseSetup(value = "/data/controller/health/polygonal/before/delete.xml", type = DatabaseOperation.DELETE)
    @ExpectedDatabase(
        value = "/data/controller/health/polygonal/after/delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successDelete() {
        // ничего не делаем - данные появляются после настройки базы
    }
}
