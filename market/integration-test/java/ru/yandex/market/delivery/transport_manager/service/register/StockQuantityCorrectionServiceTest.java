package ru.yandex.market.delivery.transport_manager.service.register;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.service.axapta.EntityLogger;
import ru.yandex.market.delivery.transport_manager.service.register.splitter.dto.RegisterUnitQuantityChanges;

@DisplayName("Коррекция кол-ва товаров в реестре и создание красного реестра")
public class StockQuantityCorrectionServiceTest extends AbstractContextualTest {
    @Autowired
    private StockQuantityCorrectionService stockQuantityCorrectionService;

    @DisplayName("Сохранение результатов коррекции кол-ва")
    @DatabaseSetup({
        "/repository/transportation_task/additional_transportation_tasks.xml",
        "/repository/register_unit/register_unit_for_axapta_requests.xml",
        "/repository/register_unit/register_unit_for_axapta_requests_red.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_after_correction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persistChanges() {
        final RegisterUnitQuantityChanges changes = new RegisterUnitQuantityChanges();
        changes.getChanged().add(new RegisterUnit().setId(1001L).setCounts(List.of(
            new UnitCount().setQuantity(1).setCountType(CountType.FIT)
        )));
        changes.getRemoved().add(new RegisterUnit().setId(1002L));

        changes.getNewRed().add(new RegisterUnit()
            .setRegisterId(1L)
            .setType(UnitType.ITEM)
            .setCounts(List.of(
                new UnitCount()
                    .setQuantity(18)
                    .setCountType(CountType.FIT)
            )));

        changes.getChangedRed().add(new RegisterUnit().setId(2001L).setCounts(List.of(
            new UnitCount().setQuantity(10).setCountType(CountType.FIT),
            new UnitCount().setQuantity(5).setCountType(CountType.DEFECT)
        )));

        stockQuantityCorrectionService.persistChanges(
            1L,
            changes,
            new EntityLogger("Test", 1L) {
            },
            1L
        );
    }
}
