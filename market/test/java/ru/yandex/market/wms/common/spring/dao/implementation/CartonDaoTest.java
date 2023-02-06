package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.pojo.Carton;

import static org.assertj.core.api.Assertions.assertThat;

class CartonDaoTest extends IntegrationTest {

    @Autowired
    private CartonDao dao;

    /**
     * Должны быть выданы только те коробки, у которых нужная группа и DISPLAYRFPACK=1.
     * Сортировка по объему коробки.
     */
    @Test
    @DatabaseSetup("/db/dao/carton/get_cartons/setup.xml")
    void getCartons() {
        String cartonGroup1 = "PK";

        List<Carton> expected1 = Arrays.asList(
            Carton.builder()
                    .group(cartonGroup1).type("YMB").length(1).width(1).height(1).volume(1).maxWeight(200).build(),
            Carton.builder()
                    .group(cartonGroup1).type("YMA").length(2).width(2).height(2).volume(8).maxWeight(100).build(),
            Carton.builder()
                    .group(cartonGroup1).type("YMC").length(3).width(3).height(3).volume(27).maxWeight(300).build()
        );
        List<Carton> cartons1 = dao.getCartons(cartonGroup1);
        assertThat(cartons1).usingFieldByFieldElementComparator().isEqualTo(expected1);

        String cartonGroup2 = "XX";
        List<Carton> expected2 = List.of(
                Carton.builder()
                        .group(cartonGroup2).type("XXX").length(1).width(2).height(3).volume(6).maxWeight(500).build()
        );
        List<Carton> cartons2 = dao.getCartons(cartonGroup2);
        assertThat(cartons2).usingFieldByFieldElementComparator().isEqualTo(expected2);

        assertThat(dao.getCartons("NON-EXISTENT-GROUP")).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/carton/get_cartons/setup-weighted.xml")
    void getCartonsWithWeight() {
        String cartonGroup1 = "PK";
        List<Carton> expected1 = Arrays.asList(
                Carton.builder().group(cartonGroup1).type("YMB")
                        .length(1).width(1).height(1).volume(1).maxWeight(200).tareWeight(50).build(),
                Carton.builder().group(cartonGroup1).type("YMA")
                        .length(2).width(2).height(2).volume(8).maxWeight(100).tareWeight(50).build(),
                Carton.builder().group(cartonGroup1).type("YMC")
                        .length(3).width(3).height(3).volume(27).maxWeight(300).tareWeight(50).build()
        );
        List<Carton> cartons1 = dao.getCartons(cartonGroup1);
        assertThat(cartons1).usingFieldByFieldElementComparator().isEqualTo(expected1);

        String cartonGroup2 = "XX";

        Carton carton = Carton.builder().group(cartonGroup2).type("XXX")
                .length(1).width(2).height(3).volume(6).maxWeight(500).tareWeight(50).build();

                List<Carton> expected2 = List.of(carton);
        List<Carton> cartons2 = dao.getCartons(cartonGroup2);
        assertThat(cartons2).usingFieldByFieldElementComparator().isEqualTo(expected2);

        assertThat(dao.getCartons("NON-EXISTENT-GROUP")).isEmpty();
    }

}
