package ru.yandex.market.fulfillment.stockstorage;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcSkuRepository;

public class JdbcSkuRepositoryTest extends AbstractContextualTest {

    @Autowired
    private JdbcSkuRepository repository;

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void enabledIdsByUnitsSuccessMultipleIds() {
        List<UnitId> unitIds = List.of(getFirstExistingSkuUnitId(), getSecondExistingSkuUnitId(),
                getFirstNonExistingSkuUnitId(), getExistingSkuNotEnabled());
        List<Long> enabledIdsByUnits = repository.findEnabledIdsByUnits(unitIds);
        Assertions.assertThat(enabledIdsByUnits).isEqualTo(List.of(10001L, 10002L));
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void enabledIdsByUnitsSuccessOneId() {
        List<UnitId> unitIds = List.of(getFirstExistingSkuUnitId());
        List<Long> enabledIdsByUnits = repository.findEnabledIdsByUnits(unitIds);
        Assertions.assertThat(enabledIdsByUnits).isEqualTo(List.of(10001L));
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void enabledIdsByUnitsEmptyResult() {
        List<UnitId> unitIds = List.of(getFirstNonExistingSkuUnitId(), getSecondNonExistingSkuUnitId());
        List<Long> enabledIdsByUnits = repository.findEnabledIdsByUnits(unitIds);
        Assertions.assertThat(enabledIdsByUnits).isEqualTo(List.of());
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void idsByUnitsSuccessMultipleIds() {
        List<UnitId> unitIds = List.of(getFirstExistingSkuUnitId(), getSecondExistingSkuUnitId(),
                getFirstNonExistingSkuUnitId(), getExistingSkuNotEnabled());
        List<Long> idsByUnits = repository.findIdsByUnits(unitIds);
        Assertions.assertThat(idsByUnits).isEqualTo(List.of(10001L, 10002L, 10003L));
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void idsByUnitsSuccessOneId() {
        List<UnitId> unitIds = List.of(getSecondExistingSkuUnitId());
        List<Long> idsByUnits = repository.findIdsByUnits(unitIds);
        Assertions.assertThat(idsByUnits).isEqualTo(List.of(10002L));
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void idsByUnitsEmptyResult() {
        List<UnitId> unitIds = List.of(getFirstNonExistingSkuUnitId(), getSecondNonExistingSkuUnitId());
        List<Long> idsByUnits = repository.findIdsByUnits(unitIds);
        Assertions.assertThat(idsByUnits).isEqualTo(List.of());
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void existentIdsByUnitsSuccessMultipleIds() {
        List<UnitId> unitIds = List.of(getFirstExistingSkuUnitId(), getSecondExistingSkuUnitId(),
                getFirstNonExistingSkuUnitId(), getExistingSkuNotEnabled());
        Set<UnitId> existentUnits = repository.findExistentUnits(unitIds);
        Assertions.assertThat(existentUnits).isEqualTo(
                Set.of(getFirstExistingSkuUnitId(), getSecondExistingSkuUnitId(), getExistingSkuNotEnabled()));
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void existentIdsByUnitsSuccessOneId() {
        List<UnitId> unitIds = List.of(getSecondExistingSkuUnitId());
        Set<UnitId> existentUnits = repository.findExistentUnits(unitIds);
        Assertions.assertThat(existentUnits).isEqualTo((Set.of(getSecondExistingSkuUnitId())));
    }

    @Test
    @DatabaseSetup("classpath:database/states/sku_repository/sku_many_ids.xml")
    public void existentIdsByUnitsEmptyResult() {
        List<UnitId> unitIds = List.of(getFirstNonExistingSkuUnitId(), getSecondNonExistingSkuUnitId());
        Set<UnitId> existentUnits = repository.findExistentUnits(unitIds);
        Assertions.assertThat(existentUnits).isEqualTo((Set.of()));
    }


    private UnitId getFirstExistingSkuUnitId() {
        return new UnitId("1", 1L, 1);
    }

    private UnitId getSecondExistingSkuUnitId() {
        return new UnitId("2", 2L, 1);
    }

    private UnitId getFirstNonExistingSkuUnitId() {
        return new UnitId("10", 10L, 2);
    }

    private UnitId getExistingSkuNotEnabled() {
        return new UnitId("3", 3L, 1);
    }

    private UnitId getSecondNonExistingSkuUnitId() {
        return new UnitId("20", 20L, 2);
    }
}
