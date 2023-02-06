package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.StartrekEntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.StartrekIssue;
import ru.yandex.market.delivery.transport_manager.repository.mappers.StartrekIssueMapper;
import ru.yandex.market.delivery.transport_manager.service.ticket.TicketQueue;

import static org.hamcrest.MatcherAssert.assertThat;

@DatabaseSetup("/repository/startrek/startrek_issues.xml")
class StartrekIssueMapperTest extends AbstractContextualTest {

    @Autowired
    private StartrekIssueMapper startrekIssueMapper;

    private static final StartrekIssue ISSUE = new StartrekIssue()
        .setId(1L)
        .setEntityType(StartrekEntityType.TRANSPORTATION)
        .setEntityId(1L)
        .setDate(LocalDate.of(2021, 6, 24))
        .setQueue(TicketQueue.FAILED_TRANSPORTATION)
        .setCreated(ZonedDateTime.parse("2021-06-02T00:00:00+03:00"))
        .setUpdated(ZonedDateTime.parse("2021-06-02T00:00:00+03:00"))
        .setHash("-961779365")
        .setTicketKey("TMFAILEDTRANSP-11");

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-06-24T20:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        StartrekIssue issue = new StartrekIssue()
            .setEntityType(StartrekEntityType.TRANSPORTATION)
            .setEntityId(20L)
            .setDate(LocalDate.of(2021, 6, 20))
            .setQueue(TicketQueue.FAILED_TRANSPORTATION)
            .setHash("hash")
            .setTicketKey("TMFAILEDTRANSP-20");
        startrekIssueMapper.insert(issue);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteBefore() {
        clock.setFixed(Instant.parse("2021-06-19T20:00:00Z"), ZoneOffset.UTC);
        startrekIssueMapper.cleanBefore(clock.instant());
    }

    @Test
    void getByEntity() {
        StartrekIssue issue = startrekIssueMapper.getByEntityAndDate(
            StartrekEntityType.TRANSPORTATION,
            1L,
            TicketQueue.FAILED_TRANSPORTATION,
            LocalDate.of(2021, 6, 24)
        );

        assertThatModelEquals(ISSUE, issue);
    }

    @Test
    void getKeys() {
        var keys = startrekIssueMapper.getKeys(
            StartrekEntityType.TRANSPORTATION,
            TicketQueue.FAILED_TRANSPORTATION,
            Set.of(1L, 10L)
        );

        assertThat(keys, Is.is(Map.of(
            1L, List.of("TMFAILEDTRANSP-11"),
            10L, List.of("TMFAILEDTRANSP-17")
        )));
    }

    @Test
    void getUpcoming() {
        List<StartrekIssue> upcoming = startrekIssueMapper.getUpcoming(
            TicketQueue.FAILED_TRANSPORTATION,
            StartrekEntityType.TRANSPORTATION,
            LocalDate.of(2021, 6, 24)
        );
        softly.assertThat(upcoming.stream().map(StartrekIssue::getId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void getById() {
        StartrekIssue issue = startrekIssueMapper.getById(1L);
        assertThatModelEquals(issue, ISSUE);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() {
        StartrekIssue issue = startrekIssueMapper.getById(1L);
        issue.setHash("hash123");
        startrekIssueMapper.update(issue);
    }
}
