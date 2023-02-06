package ru.yandex.market.delivery.transport_manager.facade.register.pallet;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.dto.TransportationTaskRegisterIdDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;

class LayerBasedRegisterWithPalletsEnricherTest extends AbstractContextualTest {
    @Autowired
    private LayerBasedRegisterWithPalletsEnricher enricher;

    @Autowired
    private RegisterMapper registerMapper;

    @DisplayName("Товары разложились на 1 паллету. Существующая паллета осталась без изменений")
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void enrich() {
        final TransportationTaskRegisterIdDto taskAndRegister = new TransportationTaskRegisterIdDto();
        taskAndRegister.setRegisterId(1L);
        taskAndRegister.setTransportationTaskId(1L);
        enricher.enrich(taskAndRegister);

        // Тут не можем проверить связи через dbunit, так как не знаем id добавленной паллеты
        final Register register = registerMapper.getRegisterWithUnits(1L);

        softly.assertThat(register.getPallets())
            .size()
            .withFailMessage("Should have 2 pallets: 1 existing and 1 newly created")
            .isEqualTo(2);
        register.getPallets().stream().filter(p -> p.getId() != 1001L).findFirst().ifPresent(newPallet -> {
            assertParent(register.getBoxes(), 1002L, 1001L);
            assertParent(register.getBoxes(), 1003L, newPallet.getId());
            assertParent(register.getItems(), 1004L, 1002L);
            assertParent(register.getItems(), 1005L, 1003L);
            assertParent(register.getItems(), 1006L, newPallet.getId());
        });
    }

    @DisplayName("Товары разложились на 2 паллеты. У item-а проставлен count")
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register_with_count.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void enrichUsingCounts() {
        moreThenOnePalletBecauseOfPalletLimitations();
    }

    @DisplayName("Товары разложились на 2 паллеты из-за лимита 1000 sku на паллету. Физически влезают на одну")
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register_with_count_more_then_1000.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void enrich1000skuPerPallet() {
        moreThenOnePalletBecauseOfPalletLimitations();
    }

    @DisplayName("Товары разложились на 2 паллеты из-за лимита 400 кг на паллету. Физически влезают на одну")
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register_with_weight_more_400kg.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void enrich400kgPerPallet() {
        moreThenOnePalletBecauseOfPalletLimitations();
    }

    private void moreThenOnePalletBecauseOfPalletLimitations() {
        final TransportationTaskRegisterIdDto taskAndRegister = new TransportationTaskRegisterIdDto();
        taskAndRegister.setRegisterId(1L);
        taskAndRegister.setTransportationTaskId(1L);
        enricher.enrich(taskAndRegister);

        // Тут не можем проверить связи через dbunit, так как не знаем id добавленной паллеты
        final Register register = registerMapper.getRegisterWithUnits(1L);

        softly.assertThat(register.getPallets())
            .size()
            .withFailMessage(
                "Should have 3 pallets: 1 existing and 2 newly created, but was " + register.getPallets().size())
            .isEqualTo(3);
        List<Long> newPalletIds =
            register
                .getPallets()
                .stream()
                .map(RegisterUnit::getId)
                .filter(id -> id != 1001L)
                .collect(Collectors.toList());

        assertParent(register.getBoxes(), 1002L, 1001L);
        // Не валидируем 1003, так как она может быть на одной любой из 2 паллет
        assertParent(register.getItems(), 1004L, 1002L);
        assertParent(register.getItems(), 1005L, 1003L);
        softly
            .assertThat(getUnitById(register.getItems(), 1006L).getParentIds())
            .containsExactlyInAnyOrderElementsOf(newPalletIds);
    }

    @DisplayName("Паллетизация сломалась из-за превышения максимально возможной массы товара, сработал fallback")
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register_huge_weight.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void fallbackOverweight() {
        final TransportationTaskRegisterIdDto taskAndRegister = new TransportationTaskRegisterIdDto();
        taskAndRegister.setRegisterId(1L);
        taskAndRegister.setTransportationTaskId(1L);
        enricher.enrich(taskAndRegister);

        // Тут не можем проверить связи через dbunit, так как не знаем id добавленной паллеты
        final Register register = registerMapper.getRegisterWithUnits(1L);

        softly.assertThat(register.getPallets()).size()
            .withFailMessage(
                "Items passed to dummy enrichment -> pallets: required %d pallets, was %d",
                4,
                register.getPallets().size()
            )
            .isEqualTo(4);
    }

    @DisplayName("Паллетизация сломалась из-за не заполненных ВГХ, сработал fallback")
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register_bad_size.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void fallbackNullSize() {
        final TransportationTaskRegisterIdDto taskAndRegister = new TransportationTaskRegisterIdDto();
        taskAndRegister.setRegisterId(1L);
        taskAndRegister.setTransportationTaskId(1L);
        enricher.enrich(taskAndRegister);

        // Тут не можем проверить связи через dbunit, так как не знаем id добавленной паллеты
        final Register register = registerMapper.getRegisterWithUnits(1L);

        softly.assertThat(register.getPallets()).size()
            .withFailMessage(
                "Items passed to dummy enrichment -> pallets: required %d pallets, was %d",
                4,
                register.getPallets().size()
            )
            .isEqualTo(4);
    }

    @DisplayName(
        "Укладка длинных тонких предметов, длинна которых больше 1 паллеты (швабры, лопаты, карнизы), "
            + "но меньше макс. высоты паллеты"
    )
    @DatabaseSetup({
        "/repository/facade/register_with_pallet_enricher/register_long_thin.xml",
        "/repository/facade/register_with_pallet_enricher/transportation_task.xml",
    })
    @Test
    void testLongThin() {
        final TransportationTaskRegisterIdDto taskAndRegister = new TransportationTaskRegisterIdDto();
        taskAndRegister.setRegisterId(1L);
        taskAndRegister.setTransportationTaskId(1L);

        enricher.enrich(taskAndRegister);

        final Register register = registerMapper.getRegisterWithUnits(1L);
        softly.assertThat(register.getPallets()).size().isEqualTo(1);
        softly.assertThat(
            register.getItems().stream()
                .map(RegisterUnit::getParentIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        )
            .containsExactly(1L, 1L, 1L);
    }

    private void assertParent(List<RegisterUnit> units, long id, long parentId) {
        softly.assertThat(getUnitById(units, id).getParentIds()).containsExactly(parentId);
    }

    private RegisterUnit getUnitById(List<RegisterUnit> items, long id) {
        return items.stream()
            .filter(b -> b.getId() == id)
            .findFirst()
            .orElseThrow();
    }
}
