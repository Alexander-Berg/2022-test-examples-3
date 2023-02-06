package ru.yandex.direct.jobs.bannersystem.dataimport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.sspplatform.container.SspInfo;
import ru.yandex.direct.core.entity.sspplatform.repository.SspInfoYtRepository;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


@JobsTest
@ExtendWith(SpringExtension.class)
class SspPlatformSynchronizerJobTest {

    @Autowired
    private SspPlatformsRepository sspPlatformsRepository;

    private static String nameBase = LocalDateTime.now().toString();
    private static SspInfo sspInfoAdd = new SspInfo(nameBase + "-add", 1L, true);
    private static SspInfo sspInfoNoAdd = new SspInfo(nameBase + "-noadd", 2L, false);
    private static List<SspInfo> sspInfoList = ImmutableList.of(sspInfoAdd, sspInfoNoAdd);

    private SspPlatformSynchronizerJob job;
    private SspInfoYtRepository ytRepository;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private PpcProperty<LocalDate> property;

    @BeforeEach
    void before() {
        ytRepository = mock(SspInfoYtRepository.class);
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        property = mock(PpcProperty.class);
        doReturn(sspInfoList).when(ytRepository).getAll();
        job = new SspPlatformSynchronizerJob(ytRepository, ppcPropertiesSupport, sspPlatformsRepository);
    }

    @Test
    void getPlatformsFromBs() {
        LocalDate propValue = LocalDate.now();
        doReturn(propValue).when(property).get();
        doReturn(property).when(ppcPropertiesSupport).get(SspPlatformSynchronizerJob.LAST_RUN_DATE_PROPERTY);
        assertThatCode(() -> job.syncSspPlatformsIfNecessary(true))
                .doesNotThrowAnyException();

        List<String> sspInfosFromDB = sspPlatformsRepository.getAllSspPlatforms();
        assertThat("не добавили non-production запись", sspInfoNoAdd.getTitle(), not(isIn(sspInfosFromDB)));
        assertThat("добавили production запись", sspInfoAdd.getTitle(), isIn(sspInfosFromDB));
    }

    @Test
    void canRunPositiveNoProperty() {
        doReturn(null).when(property).get();
        doReturn(property).when(ppcPropertiesSupport).get(SspPlatformSynchronizerJob.LAST_RUN_DATE_PROPERTY);
        assertTrue(job.canRun(false), "при отсутствующей property canRun == true");
    }

    @Test
    void canRunPositiveOldProperty() {
        LocalDate propValue = LocalDate.now().minusDays(1);
        doReturn(propValue).when(property).get();
        doReturn(property).when(ppcPropertiesSupport).get(SspPlatformSynchronizerJob.LAST_RUN_DATE_PROPERTY);
        assertTrue(job.canRun(false), "при property <= now() canRun == true");
    }

    @Test
    void canRunNegativeTodayProperty() {
        LocalDate propValue = LocalDate.now();
        doReturn(propValue).when(property).get();
        doReturn(property).when(ppcPropertiesSupport).get(SspPlatformSynchronizerJob.LAST_RUN_DATE_PROPERTY);
        assertFalse(job.canRun(false), "при property == now() canRun == false");
    }

    @Test
    void canRunPositiveTodayPropertyForce() {
        LocalDate propValue = LocalDate.now();
        doReturn(propValue).when(property).get();
        doReturn(property).when(ppcPropertiesSupport).get(SspPlatformSynchronizerJob.LAST_RUN_DATE_PROPERTY);
        assertTrue(job.canRun(true), "при property == now() и force == true canRun == true");
    }
}
