package ru.yandex.market.abo.core.assessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.assessor.model.AssessorInfo;
import ru.yandex.market.abo.core.assessor.service.AssessorInfoRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author agavrikov
 * @date 07.04.18
 */
public class AssessorInfoRepositoryTest extends EmptyTest {

    private static final AssessorInfo FIRST = new AssessorInfo(1, "stafflogin", "tablenum", false);
    private static final AssessorInfo SECOND = new AssessorInfo(2, "stafflogin2", null, false);
    private static final AssessorInfo THIRD = new AssessorInfo(3, null, null, false);

    @Autowired
    private AssessorInfoRepository assessorInfoRepository;

    @Test
    public void testRepo() {
        AssessorInfo assessorInfo = FIRST;
        assessorInfoRepository.save(assessorInfo);
        AssessorInfo dbAssessorInfo = assessorInfoRepository.findByIdOrNull(assessorInfo.getUid());
        assertEquals(assessorInfo, dbAssessorInfo);
    }

    @Test
    public void testFindAssessorInfosByUid() {
        List<AssessorInfo> assessorInfoList = Arrays.asList(FIRST, SECOND, THIRD);
        assessorInfoRepository.saveAll(assessorInfoList);
        List<AssessorInfo> loadedInfos = assessorInfoRepository.findAssessorInfoByUidIn(Arrays.asList(1L, 2L));
        List<AssessorInfo> expected = Arrays.asList(FIRST, SECOND);
        assertEquals(expected, loadedInfos);
    }

    @Test
    public void testEmptyInSection() {
        assessorInfoRepository.save(FIRST);
        List<AssessorInfo> loaded = assessorInfoRepository.findAssessorInfoByUidIn(Collections.emptyList());
        assertTrue(loaded.isEmpty());
    }
}
