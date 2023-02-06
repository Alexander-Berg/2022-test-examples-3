package ru.yandex.market.deepmind.app.controllers;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.pojo.DisplayAssortInfo;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AssortSskuRepository;

@DbUnitDataSet(dataSource = "deepmindDataSource", before = "AssortSskuControllerTest.before.csv")
public class AssortSskuControllerTest extends DeepmindBaseAppDbTestClass {

    @Autowired
    private AssortSskuRepository assortSskuRepository;

    private AssortSskuController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AssortSskuController(assortSskuRepository);
    }

    @Test
    public void testFindBySubSsku() {
        var assortInfos1 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "sub11")
        ));
        Assertions.assertThat(assortInfos1).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13")
        );

        var assortInfos2 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "sub11"),
            new ServiceOfferKey(1, "sub13")
        ));
        Assertions.assertThat(assortInfos2).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13")
        );

        var assortInfos3 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "sub11"),
            new ServiceOfferKey(1, "sub13"),
            new ServiceOfferKey(2, "sub22")
        ));
        Assertions.assertThat(assortInfos3).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13"),
            new DisplayAssortInfo(2, "assort2", "sub21", "sub22")
        );
    }

    @Test
    public void testFindByAssortSsku() {
        var assortInfos1 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "assort1")
        ));
        Assertions.assertThat(assortInfos1).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13")
        );

        var assortInfos2 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "assort1"),
            new ServiceOfferKey(1, "assort1")
        ));
        Assertions.assertThat(assortInfos2).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13")
        );

        var assortInfos3 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "assort1"),
            new ServiceOfferKey(2, "assort2")
        ));
        Assertions.assertThat(assortInfos3).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13"),
            new DisplayAssortInfo(2, "assort2", "sub21", "sub22")
        );
    }

    @Test
    public void findMixed() {
        var assortInfos1 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "sub11"),
            new ServiceOfferKey(1, "assort1")
        ));
        Assertions.assertThat(assortInfos1).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13")
        );

        var assortInfos2 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "sub11"),
            new ServiceOfferKey(2, "assort2")
        ));
        Assertions.assertThat(assortInfos2).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13"),
            new DisplayAssortInfo(2, "assort2", "sub21", "sub22")
        );

        var assortInfos3 = controller.getByKeys(List.of(
            new ServiceOfferKey(1, "sub11"),
            new ServiceOfferKey(2, "sub22"),
            new ServiceOfferKey(2, "assort2")
        ));
        Assertions.assertThat(assortInfos3).containsExactlyInAnyOrder(
            new DisplayAssortInfo(1, "assort1", "sub11", "sub12", "sub13"),
            new DisplayAssortInfo(2, "assort2", "sub21", "sub22")
        );
    }
}
