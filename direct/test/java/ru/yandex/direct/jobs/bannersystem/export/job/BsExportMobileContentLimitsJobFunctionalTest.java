package ru.yandex.direct.jobs.bannersystem.export.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;

@JobsTest
@ExtendWith(SpringExtension.class)
class BsExportMobileContentLimitsJobFunctionalTest extends BaseBsExportMobileContentJobFunctionalTest {
    private static final int TEST_LIMIT = 4;

    /**
     * Проверяем, что, если объектов больше, чем разрешено отправлять в одном запросе, они все отправляются в нескольких
     * запросах пачками размером с заданный лимит.
     */
    @Test
    void testJobRespectsChunkSize() {
        MobileContentInfo baseMobileContentInfo = mobileContentSteps.createDefaultMobileContent();
        List<MobileContentInfo> infos = new ArrayList<>();
        infos.add(baseMobileContentInfo);
        for (int i = 0; i < TEST_LIMIT * 2; i++) {
            infos.add(mobileContentSteps.createDefaultMobileContent(baseMobileContentInfo.getShard()));
        }
        List<List<Map<String, Object>>> requests =
                performRequestsAndGetRequestBodies(baseMobileContentInfo.getShard(), TEST_LIMIT, infos, false);

        assertThat(requests).size()
                .as("Сделали три запроса")
                .isEqualTo(3);

        assertThat(requests.get(0)).size()
                .as("В первом запросе %s объектов", TEST_LIMIT)
                .isEqualTo(TEST_LIMIT);

        assertThat(requests.get(1)).size()
                .as("Во втором запросе %s объектов", TEST_LIMIT)
                .isEqualTo(TEST_LIMIT);

        assertThat(requests.get(2)).size()
                .as("В третьем запросе 1 объект")
                .isEqualTo(1);
    }

    /**
     * Проверяем, что, если объектов больше, чем разрешено отправлять в одном запросе, и задано ограничение на
     * количество итераций, отправляется только одна пачка максимального размера.
     */
    @Test
    void testJobRespectsChunkSizeOneIteration() {
        MobileContentInfo baseMobileContentInfo = mobileContentSteps.createDefaultMobileContent();
        List<MobileContentInfo> infos = new ArrayList<>();
        infos.add(baseMobileContentInfo);
        for (int i = 0; i < TEST_LIMIT * 2; i++) {
            infos.add(mobileContentSteps.createDefaultMobileContent(baseMobileContentInfo.getShard()));
        }
        List<List<Map<String, Object>>> requests =
                performRequestsAndGetRequestBodies(baseMobileContentInfo.getShard(), TEST_LIMIT, infos, true);

        assertThat(requests).size()
                .as("Сделали один запроса")
                .isEqualTo(1);

        assertThat(requests.get(0)).size()
                .as("В запросе %s объектов", TEST_LIMIT)
                .isEqualTo(TEST_LIMIT);
    }
}
