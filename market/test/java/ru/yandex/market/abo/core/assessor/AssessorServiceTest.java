package ru.yandex.market.abo.core.assessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.assessor.model.Assessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93@yndx-team.ru.
 */
@Transactional(transactionManager = "pgTransactionManager")
public class AssessorServiceTest extends EmptyTest {

    private static final AboRole PERMISSION_ID = AboRole.ROLE_SALESMANAGER;
    private static final AboRole PERMISSION_ID_2 = AboRole.ROLE_SUPPORTER;

    private static final Random rg = new Random();

    @Autowired
    private AssessorService assessorService;

    @Test
    public void testCreateAssessor() {
        Assessor assessor = newAssessor(PERMISSION_ID.getId(), PERMISSION_ID_2.getId());
        assessorService.saveAssessor(assessor);

        Assessor loaded = assessorService.getAssessor(assessor.getUid());
        Assertions.assertNotNull(loaded);
        assertEquals(2, loaded.getPermissions().size());
        assertTrue(loaded.getPermissions().contains(PERMISSION_ID.getId()));
        assertTrue(loaded.getPermissions().contains(PERMISSION_ID_2.getId()));

        assessorService.removeAssessor(loaded);
        loaded = assessorService.getAssessor(loaded.getUid());
        assertFalse(loaded.isWorking());
        assertNull(loaded.getPermissions());
    }

    @Test
    public void testHasPermission() {
        Assessor assessor = newAssessor(PERMISSION_ID.getId(), PERMISSION_ID_2.getId());
        assessorService.saveAssessor(assessor);

        assessor = assessorService.getAssessor(assessor.getUid());
        assertTrue(assessor.hasPermission(PERMISSION_ID));
        assertTrue(assessor.hasPermission(PERMISSION_ID_2));
    }

    @Test
    public void testLoadAssessors() {
        int count = 5;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Assessor newAssessor = newAssessor(PERMISSION_ID.getId());
            ids.add(newAssessor.getUid());
            assessorService.saveAssessor(newAssessor);
        }

        List<Assessor> assessors = assessorService.loadAssessors(ids);
        assertEquals(assessors.size(), 5);
    }

    @Test
    public void testChangePermissions() {
        Assessor a = newAssessor(PERMISSION_ID.getId());
        assessorService.saveAssessor(a);

        a.setPermissions(Arrays.asList(PERMISSION_ID_2.getId()));
        assessorService.saveAssessor(a);

        assertFalse(assessorService.getAssessor(a.getUid()).getPermissions().contains(PERMISSION_ID.getId()));
    }

    @Test
    public void testLoadBilledAssessor() {
        List<Assessor> assessors = IntStream.range(0, 3).mapToObj(i -> newAssessor()).collect(Collectors.toList());

        assessors.get(0).setWorking(true);
        assessors.get(0).setPermissions(List.of(AboRole.ROLE_ASSESSOR.getId()));
        assessors.get(1).setWorking(true);
        assessors.get(1).setPermissions(List.of(AboRole.ROLE_ASSESSOR.getId()));

        assessors.forEach(assessor -> assessorService.saveAssessor(assessor));
        assessorService.saveAssessorInfo(assessors.get(0).getUid(), "", "", false);
        assessorService.saveAssessorInfo(assessors.get(1).getUid(), "", "", true);

        Assessor notSaved = newAssessor();
        assessorService.saveAssessorInfo(notSaved.getUid(), "", "", false);

        // 0: billed; 1: ignoreForBilling = true; 2: working = false;
        List<Long> billedUids = assessorService.loadBilledAssessors();
        assertEquals(1, billedUids.size());
        assertEquals(assessors.get(0).getUid(), billedUids.get(0));
    }

    private static Assessor newAssessor(Long...roleIds) {
        Assessor assessor = new Assessor((long) rg.nextInt(), "login");
        assessor.setPermissions(Arrays.asList(roleIds));
        return assessor;
    }
}
