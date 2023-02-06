package ru.yandex.market.adv.b2bmonetization.programs.service.autocreate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.adv.b2bmonetization.programs.model.PartnerColor;

@DisplayName("Тесты сервиса AutostrategyCollector для синих магазинов")
@ParametersAreNonnullByDefault
class BlueAutostrategyCollectorTest extends AbstractAutostrategyCollectorTest {

    BlueAutostrategyCollectorTest() {
        super(PartnerColor.BLUE);
    }
}
