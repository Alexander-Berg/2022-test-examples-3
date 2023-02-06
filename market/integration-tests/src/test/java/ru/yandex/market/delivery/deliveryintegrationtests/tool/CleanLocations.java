package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;

/**
 * Утилитарный класс для очистки локаций, которые генерит dataCreator
 *
 * Для того чтобы прибить все созданные dataCreator столы приемки
 * достаточно запустить cleanInbounTables и дождаться результата.
 *
 * Запускать нужно когда datacreator начнет ругаться ошибкой Free location not found
 * Запускать каждый раз перед тестами не нужно, чтобы вдруг при параллельном запуске чужих тестов
 * не прибить столы которые нужны.
 */
public class CleanLocations {
    private static final Logger log = LoggerFactory.getLogger(CleanLocations.class);
    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    @Step("Удаление стола приемки с ячейками STAGE{suffix}, STAGEOBM{suffix} и DAMAGE{suffix}")
    private void deleteInboundTable(String suffix) {
        log.info("Удаление стола приемки с ячейками STAGE" + suffix
                + ", STAGEOBM" + suffix
                + " и DAMAGE" + suffix
        );

        try {
            dataCreator.deleteLoc("STAGE" + suffix);
        } catch (Throwable e) {
            Allure.addAttachment("Error deleting loc", e.getMessage());
            log.info(e.getMessage());
        }

        try {
            dataCreator.deleteLoc("STAGEOBM" + suffix);
        } catch (Throwable e) {
            Allure.addAttachment("Error deleting loc", e.getMessage());
            log.info(e.getMessage());
        }

        try {
            dataCreator.deleteLoc("DAMAGE" + suffix);
        } catch (Throwable e) {
            Allure.addAttachment("Error deleting loc", e.getMessage());
            log.info(e.getMessage());
        }
    }

    /**
     * Удаляет все созданные datacreator столы приемки (ячейки STAGE, STAGEOBM, DAMAGE)
     * с индексами "1A" - "FF"
     *
     * Столы с индексами 01 - 19 используются для ручной работы, их удалять не нужно.
     */
    @Test
    public void cleanInbounTables() {
        int first = Integer.parseInt("1A", 16);
        int last = Integer.parseInt("FF", 16);

        for (int i = first; i < last; i++) {
            String suffix = Integer.toHexString(i).toUpperCase();
            deleteInboundTable(suffix);
        }
    }
}
