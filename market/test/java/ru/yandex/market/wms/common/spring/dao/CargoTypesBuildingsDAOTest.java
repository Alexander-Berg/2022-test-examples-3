package ru.yandex.market.wms.common.spring.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.CargoTypesBuildingsDAO;

import static org.assertj.core.api.Assertions.assertThat;

public class CargoTypesBuildingsDAOTest extends IntegrationTest {
    @Autowired
    private CargoTypesBuildingsDAO cargoTypesBuildingsDAO;

    @Test
    @DatabaseSetup(value = "/db/dao/cargotypes-buildings/before.xml")
    void existingCargotypes() {
        List<Integer> existingCargotypes = cargoTypesBuildingsDAO.existingCargotypes();
        assertThat(existingCargotypes.size()).isEqualTo(3);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/cargotypes-buildings/before.xml")
    void buildingIdByCargotype() {
        int buildingId = cargoTypesBuildingsDAO.buildingIdByCargotype(50);
        assertThat(buildingId).isEqualTo(1);
    }
}
