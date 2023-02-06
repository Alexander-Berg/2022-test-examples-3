package ru.yandex.direct.core.entity.client.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.AgencyClientProve;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.AgencyClientProveSteps;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AgencyClientProveRepositoryTest {
    private static int shard = 1;
    private LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private AgencyClientProveSteps agencyClientProveSteps;

    @Autowired
    private AgencyClientProveRepository repoUnderTest;

    @Test
    public void addNewEntries() {
        AgencyClientProve oldAndNotConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), false);
        AgencyClientProve oldAndConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), true);


        List<AgencyClientProve> allEntries = repoUnderTest.getAllRequests(shard);

        assertThat("обе записи с is_confirmed=(0|1) успешно выбраны из БД после вставки", allEntries,
                allOf(hasItem(oldAndNotConfirmed), hasItem(oldAndConfirmed)));
    }

    @Test
    public void fetchEntriesOlderThanDateTime() {
        AgencyClientProve oldAndNotConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), false);
        AgencyClientProve oldAndConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), true);
        AgencyClientProve newAndNotConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.plusHours(1), false);
        AgencyClientProve newAndConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.plusHours(1), true);

        List<AgencyClientProve> selectedEntries = repoUnderTest.getOutdatedRequests(shard, localDateTime);

        Assertions.assertThat(selectedEntries)
                .as("новые записи с is_confirmed=(0|1) не выбраны")
                .doesNotContain(newAndNotConfirmed, newAndConfirmed);
        assertThat("выбрана старая запись с is_confirmed=0", selectedEntries,
                hasItem(oldAndNotConfirmed));
        assertThat("выбрана старая запись с is_confirmed=1", selectedEntries,
                hasItem(oldAndConfirmed));
    }

    @Test
    public void deleteNotConfirmedEntry() {
        AgencyClientProve oldAndNotConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), false);
        AgencyClientProve oldAndNotConfirmed2 = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), false);

        repoUnderTest.deleteRequest(shard, oldAndNotConfirmed);
        List<AgencyClientProve> allEntries = agencyClientProveSteps.getAllClientAgencyProve(shard);
        assertThat("указанная к удалению запись с is_confirmed=0 удалена", allEntries,
                not(hasItem(oldAndNotConfirmed)));
        assertThat("не указанная к удалению запись с is_confirmed=0 не удалена", allEntries,
                hasItem(oldAndNotConfirmed2));
    }

    @Test
    public void deleteConfirmedEntry() {
        AgencyClientProve oldAndConfirmed = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), true);
        AgencyClientProve oldAndConfirmed2 = agencyClientProveSteps
                .createAgencyClientProveWithDateTimeAndIsConfirmed(shard, localDateTime.minusHours(1), true);

        repoUnderTest.deleteRequest(shard, oldAndConfirmed);
        List<AgencyClientProve> allEntries = agencyClientProveSteps.getAllClientAgencyProve(shard);
        assertThat("указанная к удалению запись с is_confirmed=1 удалена", allEntries,
                not(hasItem(oldAndConfirmed)));
        assertThat("не указанная к удалению запись с is_confirmed=1 не удалена", allEntries,
                hasItem(oldAndConfirmed2));
    }
}
