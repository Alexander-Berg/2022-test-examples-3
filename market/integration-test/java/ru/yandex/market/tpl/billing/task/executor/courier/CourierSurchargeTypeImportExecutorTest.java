package ru.yandex.market.tpl.billing.task.executor.courier;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.repository.courier.CourierSurchargeTypeRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * Тесты для {@link ru.yandex.market.tpl.billing.task.executor.courier.CourierSurchargeTypeImportExecutor}
 */
public class CourierSurchargeTypeImportExecutorTest extends AbstractFunctionalTest {
    private CourierSurchargeTypeImportExecutor courierSurchargeTypeImportExecutor;

    @Autowired
    private Yt hahn;
    @Autowired
    private CourierSurchargeTypeRepository courierSurchargeTypeRepository;

    @BeforeEach
    void setUp() {
        courierSurchargeTypeImportExecutor = spy(new CourierSurchargeTypeImportExecutor(
                "//null",
                hahn,
                courierSurchargeTypeRepository
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/task/executor/courier/courierSurchargeTypeImport/before/saveData.csv",
            after = "/database/task/executor/courier/courierSurchargeTypeImport/after/saveData.csv"
    )
    void testSaveData() {
        doAnswer(invocation -> Stream.of(
                new YTreeBuilder()
                        .beginMap()
                        .key("code").value("code1")
                        .key("description").value("description1")
                        .key("name").value("name1")
                        .key("type").value("PENALTY")
                        .buildMap(),
                new YTreeBuilder()
                        .beginMap()
                        .key("code").value("code2")
                        .key("description").value("description2")
                        .key("name").value("name2")
                        .key("type").value("BONUS")
                        .buildMap()
        )).when(courierSurchargeTypeImportExecutor).stream(any(YPath.class));
        courierSurchargeTypeImportExecutor.doJob();
    }
}
