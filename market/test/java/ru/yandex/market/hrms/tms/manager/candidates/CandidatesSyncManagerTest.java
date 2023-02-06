package ru.yandex.market.hrms.tms.manager.candidates;


import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.StaffConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.EmployeeCandidatesSyncManager;

@DbUnitDataSet(before = "CandidatesSyncManagerTest.before.csv")
public class CandidatesSyncManagerTest extends AbstractTmsTest {

    @Autowired
    private EmployeeCandidatesSyncManager candidatesSyncManager;

    @Autowired
    private StaffConfigurer staffConfigurer;

    @Test
    @DbUnitDataSet(
            before = "EmployeeSyncManagerTest.shouldAddEmployeeCandidates.before.csv",
            after = "EmployeeSyncManagerTest.shouldAddEmployeeCandidates.after.csv")
    void shouldAddCandidates() {
        mockClock(LocalDateTime.of(2022, 2, 2, 23, 59, 59));
        staffConfigurer.mockGetPrehired("/results/staff_candidates_add_new.json");
        candidatesSyncManager.loadEmployeeCandidates();
    }

    @Test
    @DbUnitDataSet(
            before = "EmployeeSyncManagerTest.shouldBlockCancelledEmployeeCandidates.before.csv",
            after = "EmployeeSyncManagerTest.shouldBlockCancelledEmployeeCandidates.after.csv")
    void shouldBlockCancelledCandidates() {
        mockClock(LocalDateTime.of(2022, 2, 4, 12, 0, 0));
        staffConfigurer.mockGetPrehired("/results/staff_candidates_block_cancelled.json");
        candidatesSyncManager.loadEmployeeCandidates();
    }

    @Test
    @DbUnitDataSet(
            before = "EmployeeSyncManagerTest.shouldBlockCancelledEmployeeCandidates.before.csv",
            after = "EmployeeSyncManagerTest.shouldDeleteNotComingEmployeeCandidates.after.csv")
    void shouldDeleteNotComingCandidates() {
        mockClock(LocalDateTime.of(2022, 2, 4, 12, 0, 0));
        staffConfigurer.mockGetPrehired("/results/staff_candidates_not_coming_one.json");
        candidatesSyncManager.loadEmployeeCandidates();
    }

    @Test
    @DbUnitDataSet(
            before = "EmployeeSyncManagerTest.shouldRehireCandidates.before.csv",
            after = "EmployeeSyncManagerTest.shouldRehireCandidates.after.csv")
    void shouldRehireCandidates() {
        mockClock(LocalDateTime.of(2022, 2, 10, 12, 0, 0));
        staffConfigurer.mockGetPrehired("/results/staff_candidates_rehiring.json");
        candidatesSyncManager.loadEmployeeCandidates();
    }
}
