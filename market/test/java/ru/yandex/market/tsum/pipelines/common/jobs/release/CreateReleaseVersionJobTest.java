package ru.yandex.market.tsum.pipelines.common.jobs.release;

import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekSettings;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.startrek.client.Versions;
import ru.yandex.startrek.client.model.Version;
import ru.yandex.startrek.client.model.VersionCreate;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 25.09.17
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateReleaseVersionJobTest {

    @Test
    public void createNewVersionTest() {
        StartrekSettings startrekSettings = new StartrekSettings("MBO");
        Versions versions = Mockito.mock(Versions.class);
        CreateReleaseVersionJob createReleaseVersionJob = new CreateReleaseVersionJob()
            .setStartrekSettings(startrekSettings)
            .setVersions(versions);

        LocalDate now = LocalDate.now();
        String versionName = getVersionName(now.getYear(), now.getMonthOfYear(), 9);
        Version latestVersion = TestVersionBuilder.aVersion().withId(1).withName(versionName).build();

        ListF<Version> vers = new ArrayListF<>();
        vers.add(latestVersion);

        @SuppressWarnings("unchecked")
        IteratorF<Version> versionsIterator = (IteratorF<Version>) Mockito.mock(IteratorF.class);

        Mockito.when(versions.getAll(startrekSettings.getQueue())).thenReturn(versionsIterator);
        Mockito.when(versionsIterator.toList()).thenReturn(vers);

        createReleaseVersionJob.createNewVersion();

        Mockito.verify(versions).create(new VersionCreate.Builder()
            .name(getVersionName(now.getYear(), now.getMonthOfYear(), 10))
            .queue(latestVersion.getQueue())
            .startDate(org.mockito.Matchers.any())
            .build()
        );
    }

    @Test
    public void createdNewVersionNotificationTest() {
        Version version = TestVersionBuilder.aVersion().withId(1).withName("2017.9.13").build();
        CreateReleaseVersionJob createReleaseVersionJob = new CreateReleaseVersionJob();

        String telegramMessage = createReleaseVersionJob.createdNewVersionNotification(version).getTelegramMessage();

        Assert.assertThat(telegramMessage, Matchers.containsString("Создана релизная версия 2017.9.13"));
    }

    @Test
    public void createNewVersionNameTest() {
        LocalDate now = LocalDate.now();
        int releaseNumber = 9;
        String versionName = getVersionName(now.getYear(), now.getMonthOfYear(), releaseNumber);
        Version latestVersion = TestVersionBuilder.aVersion().withId(1).withName(versionName).build();

        CreateReleaseVersionJob createReleaseVersionJob = new CreateReleaseVersionJob();
        String newVersionsName = createReleaseVersionJob.createNewVersionsName(latestVersion);

        Assert.assertThat(newVersionsName,
            Matchers.is(getVersionName(now.getYear(), now.getMonthOfYear(), releaseNumber + 1))
        );
    }

    @Test
    public void createdNewVersionNameWithDifMonth() {
        LocalDate now = LocalDate.now();
        if (now.getMonthOfYear() == 1) {
            now = now.plusMonths(1);
        } else {
            now = now.minusMonths(1);
        }

        String versionName = getVersionName(now.getYear(), now.getMonthOfYear(), 9);

        Version latestVersion = TestVersionBuilder.aVersion().withId(1).withName(versionName).build();

        CreateReleaseVersionJob createReleaseVersionJob = new CreateReleaseVersionJob();
        String newVersionsName = createReleaseVersionJob.createNewVersionsName(latestVersion);

        Assert.assertEquals(newVersionsName, getVersionName(now.getYear(), LocalDate.now().getMonthOfYear(), 1));
    }

    private String getVersionName(int year, int month, int number) {
        return String.format(CreateReleaseVersionJob.VERSION_TEMPLATE, year, month, number);
    }
}
