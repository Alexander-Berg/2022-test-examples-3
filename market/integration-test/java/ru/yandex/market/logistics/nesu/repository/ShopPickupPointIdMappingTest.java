package ru.yandex.market.logistics.nesu.repository;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

@ParametersAreNonnullByDefault
@DisplayName("Тест на триггер и функцию в postgres")
class ShopPickupPointIdMappingTest extends AbstractContextualTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Изменение lms_id из NULL в NULL")
    @DatabaseSetup("/jobs/consumer/create_shop_pickup_point/before/shop_pickup_point.xml")
    @ExpectedDatabase(
        value = "/controller/shop-pickup-points/after/shop_pickup_point_id_mapping_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fromNullToNullNoConflict() {
        updateShopPickupPoint(null);
    }

    @Test
    @DisplayName("Изменение lms_id из NULL в 1000")
    @DatabaseSetup("/jobs/consumer/create_shop_pickup_point/before/shop_pickup_point.xml")
    @ExpectedDatabase(
        value = "/jobs/consumer/create_shop_pickup_point/after/shop_pickup_point_id_mapping_set.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fromNullToNonNullNoConflict() {
        updateShopPickupPoint(1000L);
    }

    @Test
    @DisplayName("Изменение lms_id из NULL в 1000, в таблице с маппингом уже есть ключ 1000, запись будет обновлена")
    @DatabaseSetup("/jobs/consumer/create_shop_pickup_point/before/shop_pickup_point.xml")
    @DatabaseSetup("/repository/shop-pickup-point-meta/shop_pickup_point_id_mapping_set.xml")
    @ExpectedDatabase(
        value = "/jobs/consumer/create_shop_pickup_point/after/shop_pickup_point_id_mapping_set.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fromNullToNonNullConflictLmsIdAlreadyExistsInMappingTable() {
        softly.assertThatCode(() -> updateShopPickupPoint(1000L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName(
        "Изменение lms_id из NULL в 1000, в таблице с маппингом уже есть запись (1000, 8001), " +
            "в таблице ПВЗ уже есть точка с lms_id = 1000 (в результате update'а таких точки станет две), " +
            "запись в таблице с маппингом будет успешно обновлена"
    )
    @DatabaseSetup("/jobs/consumer/create_shop_pickup_point/before/conflict.xml")
    @ExpectedDatabase(
        value = "/jobs/consumer/create_shop_pickup_point/after/conflict.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fromNullToNonNullConflictLmsIdAlreadyExistsInMappingAndPickupPointTables() {
        softly.assertThatCode(() -> updateShopPickupPoint(1000L)).doesNotThrowAnyException();
    }

    private void updateShopPickupPoint(@Nullable Long lmsId) {
        jdbcTemplate.update("UPDATE shop_pickup_point SET lms_id = ? WHERE id = ?", lmsId, 400L);
    }
}
