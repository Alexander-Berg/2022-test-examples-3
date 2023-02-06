package ru.yandex.market.wms.picking.modules.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.picking.sample.dto.TaskDetail;

public class PickingCargotypeDaoTest extends IntegrationTest {

    @Autowired
    private PickingCargotypeDao underTest;

    @Test
    @DatabaseSetup("/repository/get-cargotype-of-taskdetail/1/before.xml")
    void tryToGetCargotypeSingleList() {
        TaskDetail givenTaskDetail = TaskDetail.builder()
                .sku("TEST0000000000000000000")
                .storerKey("909090909")
                .build();
        List<CargoType> given = List.of(CargoType.ELECTRONICS);

        List<CargoType> expected = underTest.getCargotypeOfTaskDetail(givenTaskDetail.getStorerKey(),
                givenTaskDetail.getSku());

        Assertions.assertEquals(given, expected);
        Assertions.assertNotNull(expected);
    }

    @Test
    @DatabaseSetup("/repository/get-cargotype-of-taskdetail/2/before.xml")
    void tryToGetCargotypeMultipleList() {
        TaskDetail givenTaskDetail = TaskDetail.builder()
                .sku("TEST0000000000000000000")
                .storerKey("909090909")
                .build();
        List<CargoType> given = List.of(CargoType.TECH_AND_ELECTRONICS, CargoType.ELECTRONICS);

        List<CargoType> expected = underTest.getCargotypeOfTaskDetail(givenTaskDetail.getStorerKey(),
                givenTaskDetail.getSku());

        Assertions.assertEquals(given, expected);
        Assertions.assertNotNull(expected);
    }
}
