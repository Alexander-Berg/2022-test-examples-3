package ru.yandex.market.abo.core.premod.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.model.PremodRegionConstraint;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 20.05.18
 */
public class PremodRegionConstraintRepoTest extends EmptyTest {

    @Autowired
    PremodRegionConstraintRepo premodRegionConstraintRepo;

    @Test
    public void testRepo() throws Exception {
        List<PremodRegionConstraint> premodRegionConstraintList = initPremodRegionConstraintList();
        premodRegionConstraintRepo.saveAll(premodRegionConstraintList);
        List<PremodRegionConstraint> dbPremodRegionConstraintList = premodRegionConstraintRepo.findAll();
        assertEquals(premodRegionConstraintList.size(), dbPremodRegionConstraintList.size());
        premodRegionConstraintRepo.deleteAll();
        assertEquals(0, premodRegionConstraintRepo.findAll().size());
    }

    private static List<PremodRegionConstraint> initPremodRegionConstraintList() {
        List<PremodRegionConstraint> premodRegionConstraintList = new ArrayList<>();
        premodRegionConstraintList.add(new PremodRegionConstraint(225L, 1L));
        premodRegionConstraintList.add(new PremodRegionConstraint(225L, 2L));
        premodRegionConstraintList.add(new PremodRegionConstraint(225L, 3L));
        premodRegionConstraintList.add(new PremodRegionConstraint(225L, 4L));
        return premodRegionConstraintList;
    }
}

