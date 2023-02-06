package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.AssigmentType;
import ru.yandex.market.wms.common.spring.dao.entity.PickingAssignment;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class PickingAssignmentsDaoTest extends IntegrationTest {
    @Autowired
    private PickingAssignmentsDao dao;

    private static final PickingAssignment ASSIGNMENT_2 = PickingAssignment.builder()
            .assignmentNumber("00002")
            .type(AssigmentType.SORTABLE_CONVEYABLE)
            .waveKey("WAVE02")
            .zone("ZONE2")
            .consolidationLoc("SORT02")
            .build();

    private static final PickingAssignment ASSIGNMENT_3 = PickingAssignment.builder()
            .assignmentNumber("00003")
            .type(AssigmentType.SORTABLE_NON_CONVEYABLE)
            .waveKey("WAVE03")
            .zone("ZONE3")
            .consolidationLoc(null)
            .build();

    @Test
    @DatabaseSetup("/db/dao/picking-assignment/before.xml")
    @ExpectedDatabase(value = "/db/dao/picking-assignment/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void insertAssignments() {
        Collection<PickingAssignment> assignments = List.of(ASSIGNMENT_2, ASSIGNMENT_3);
        dao.insertAssignments(assignments, "TESTUSER");
    }

    @Test
    @DatabaseSetup("/db/dao/picking-assignment/before.xml")
    @ExpectedDatabase(value = "/db/dao/picking-assignment/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void insertAssignmentsEmptyList() {
        dao.insertAssignments(Collections.emptyList(), "TESTUSER");
    }

    @Test
    @DatabaseSetup("/db/dao/picking-assignment/after.xml")
    public void findAssignments() {
        List<PickingAssignment> actual = dao.findByAssignmentNumbers(List.of("00002", "00003"));
        assertDoubleEquals(2, actual.size());
        Assertions.assertTrue(actual.contains(ASSIGNMENT_2));
        Assertions.assertTrue(actual.contains(ASSIGNMENT_3));
    }

    @Test
    @DatabaseSetup("/db/dao/picking-assignment/after.xml")
    public void findAssignmentsEmptyList() {
        List<PickingAssignment> actual = dao.findByAssignmentNumbers(Collections.emptyList());
        assertDoubleEquals(0, actual.size());
    }

}
