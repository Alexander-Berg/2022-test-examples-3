package ru.yandex.direct.core.entity.mobilecontent.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.MobileContentSteps;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileContentRepositoryGetOlderThanTest {
    @Autowired
    private MobileContentSteps steps;
    @Autowired
    private MobileContentRepository repository;

    private int shard;
    private MobileContentInfo newAvailableApp;
    private MobileContentInfo oldAvailableApp;
    private MobileContentInfo oldUnavailableApp;

    @Before
    public void setUp() throws Exception {
        shard = 1;
        newAvailableApp = steps.createMobileContent(shard,
                new MobileContentInfo().withMobileContent(iosMobileContent()
                        .withStoreRefreshTime(LocalDateTime.now().minus(1, MINUTES))
                        .withIsAvailable(true)));
        oldAvailableApp = steps.createMobileContent(shard,
                new MobileContentInfo().withMobileContent(iosMobileContent()
                        .withStoreRefreshTime(LocalDateTime.now().minus(2, DAYS))
                        .withIsAvailable(true)));
        oldUnavailableApp = steps.createMobileContent(shard,
                new MobileContentInfo().withMobileContent(iosMobileContent()
                        .withStoreRefreshTime(LocalDateTime.now().minus(2, DAYS))
                        .withIsAvailable(false)));
    }

    @After
    public void tearDown() {
        repository.deleteMobileContentById(shard, StreamEx.of(newAvailableApp.getMobileContentId(),
                oldAvailableApp.getMobileContentId(), oldUnavailableApp.getMobileContentId()).toList());
    }

    @Test
    public void getMobileContentOlderThan_getAvailable_returnAllOlder() {
        List<Long> result = StreamEx
                .of(repository.getMobileContentOlderThan(shard, Duration.ofDays(1L), 10L, true))
                .map(MobileContent::getId)
                .toList();
        assertThat(oldAvailableApp.getMobileContentId(), isIn(result));
        assertThat(newAvailableApp.getMobileContentId(), not(isIn(result)));
        assertThat(oldUnavailableApp.getMobileContentId(), not(isIn(result)));
    }

    @Test
    public void getMobileContentOlderThan_getUnavailable_returnAllOlder() {
        List<Long> result = StreamEx
                .of(repository.getMobileContentOlderThan(shard, Duration.ofDays(1L), 10L, false))
                .map(MobileContent::getId)
                .toList();
        assertThat(oldUnavailableApp.getMobileContentId(), isIn(result));
        assertThat(newAvailableApp.getMobileContentId(), not(isIn(result)));
        assertThat(oldAvailableApp.getMobileContentId(), not(isIn(result)));
    }
}
