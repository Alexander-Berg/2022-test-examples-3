package ru.yandex.market.delivery.transport_manager.facade.register.pallet;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.dto.TransportationTaskRegisterIdDto;

class DummyRegisterWithPalletsEnricherTest extends AbstractContextualTest {
    @Autowired
    private DummyRegisterWithPalletsEnricher enricher;

    @DisplayName("Нет товаров - проверяем, что отрабатывает и ничего не создаёт")
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void enrichEmpty() {
        enricher.enrich(new TransportationTaskRegisterIdDto(6L, 1L));
    }

    @DisplayName("Товары укладываются на 1 паллету")
    @DatabaseSetup({
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/register_unit/register_unit_1_pallet.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_1_pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void enrich1Pallet() {
        enricher.enrichInternal(
            new TransportationTaskRegisterIdDto(1L, 1L)
        );
    }

    @DisplayName("Товары укладываются на 2 паллеты")
    @DatabaseSetup({
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/register_unit/register_unit_2_pallet.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_2_pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void enrich2Pallet() {
        enricher.enrichInternal(
            new TransportationTaskRegisterIdDto(1L, 1L)
        );
    }

    @DisplayName("Одна из задач на перемещение уже разложена на паллеты, раскладываем только одну")
    @DatabaseSetup({
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/register_unit/register_unit_2_pallet.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register_unit/after/register_unit_2_pallet.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void alreadyProcessed() {
        enricher.enrichInternal(
            new TransportationTaskRegisterIdDto(1L, 1L)
        );
    }
}
