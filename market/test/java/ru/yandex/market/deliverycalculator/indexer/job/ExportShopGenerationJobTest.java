package ru.yandex.market.deliverycalculator.indexer.job;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampTechCommands;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexer.service.datacamp.DataCampTechCommand;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ExportShopGenerationJobTest extends FunctionalTest {
    @Autowired
    private ExportShopGenerationJob exportShopGenerationJob;

    @Autowired
    private LogbrokerEventPublisher<DataCampTechCommand> datacampTechCommandsLogbrokerService;

    @Test
    @DbUnitDataSet(
            before = "shop-generations/exportFeedsTest.before.csv",
            after = "shop-generations/exportFeedsTest.after.csv"
    )
    void exportFeedsTest() {
        exportShopGenerationJob.doJob(null);
        // CPC-неЕкат магазин не отправляется на перемайнинг
        verifyNoMoreInteractions(datacampTechCommandsLogbrokerService);
    }

    /**
     * Тест варит тариф для 4 магазинов:
     * <ol>
     *     <li>Два ЕКат-DBS, чтобы получилось "мультизадание" на майнинг</li>
     *     <li>НЕ DBS</li>
     *     <li>НЕ ЕКат</li>
     * </ol>
     * В результате должно сгенерится одно сообщение с двумя заданиями.
     */
    @DisplayName("DBS-магазин в ЕКат должен отправляться на перемайнинг при изменении тарифа")
    @Test
    @DbUnitDataSet(
            before = "shop-generations/exportUcatDbsFeedsTest.before.csv",
            after = "shop-generations/exportUcatDbsFeedsTest.after.csv"
    )
    void exportUcatDbsFeedsTest() {
        exportShopGenerationJob.doJob(null);
        var captor = ArgumentCaptor.forClass(DataCampTechCommand.class);
        verify(datacampTechCommandsLogbrokerService).publishEvent(captor.capture());
        DatacampMessageOuterClass.DatacampMessage msg = captor.getValue().getPayload();

        assertThat(msg, allOf(
                hasProperty("techCommandCount", equalTo(2)),
                hasProperty("techCommandList", containsInAnyOrder(
                        allOf(
                                hasProperty("commandType",
                                        equalTo(DataCampTechCommands.DatacampTechCommandType.FORCE_MINE_FOR_SHOP)),
                                hasProperty("commandParams", hasProperty("shopId", equalTo(1001)))
                        ),
                        allOf(
                                hasProperty("commandType",
                                        equalTo(DataCampTechCommands.DatacampTechCommandType.FORCE_MINE_FOR_SHOP)),
                                hasProperty("commandParams", hasProperty("shopId", equalTo(1002)))
                        )
                ))
        ));

        verifyNoMoreInteractions(datacampTechCommandsLogbrokerService);
    }
}
