package ru.yandex.market.pers.qa.tms.export.yt;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.tms.export.yt.model.QuestionForUpload;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.pers.yt.YtClusterType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class NewQuestionExportExecutorTest extends PersQaTmsTest {

    @Autowired
    private DayOfWeekProvider dayOfWeekProvider;
    @Autowired
    private NewQuestionExportExecutor executor;
    @Autowired
    private YtClientProvider ytClientProvider;
    @Autowired
    private QuestionService questionService;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<List<?>> entitiesToUploadCaptor;

    @Captor
    private ArgumentCaptor<YPath> createTableCaptor;

    @Captor
    private ArgumentCaptor<YPath> removeLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> createLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> currentLinkCaptor;

    @Captor
    private ArgumentCaptor<YPath> appendCaptor;

    private YtClient ytClient;

    @Override
    protected void resetMocks() {
        super.resetMocks();
        MockitoAnnotations.initMocks(this);
        ytClient = ytClientProvider.getClient(YtClusterType.HAHN);
    }

    @Test
    void testExportFailedBecauseOfWrongDay() {
        EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach(dayOfWeek -> {
            Mockito.when(dayOfWeekProvider.getCurrentDayOfWeek()).thenReturn(dayOfWeek);
            Exception e = Assertions.assertThrows(IllegalStateException.class, () -> executor.export());
            Assertions.assertEquals("Run at wrong day: " + dayOfWeek, e.getMessage());
        });
    }

    @Test
    void testExportFailedBecauseOfWrongDayForWeek() {
        EnumSet.of(DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY).forEach(dayOfWeek -> {
            Mockito.when(dayOfWeekProvider.getCurrentDayOfWeek()).thenReturn(dayOfWeek);

            Exception e = Assertions
                .assertThrows(IllegalStateException.class, () -> executor.exportForWeek());
            Assertions.assertEquals("Run at wrong day: " + dayOfWeek, e.getMessage());
        });
    }

    void successfulRunOnCertainDay(DayOfWeek dayOfWeek, String expectedLink, int expectedCount) {
        int count = 10;
        for (int i = 0; i < count; i++) {
            Question question = questionService.createModelQuestion(i, UUID.randomUUID().toString(), count - i);
            jdbcTemplate.update("update qa.question set cr_time = ? where id = ?", LocalDateTime.now().minus(i, ChronoUnit.DAYS), question.getId());
        }

        String expectedTableName = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);

        Mockito.when(dayOfWeekProvider.getCurrentDayOfWeek()).thenReturn(dayOfWeek);
        executor.export();
        Mockito.verify(ytClient).createTable(createTableCaptor.capture(), eq(QuestionForUpload.tableSchema()));
        Mockito.verify(ytClient).append(appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).removeLink(any(), removeLinkCaptor.capture());
        Mockito.verify(ytClient).createLink(any(), createLinkCaptor.capture(), currentLinkCaptor.capture());
        Assertions.assertEquals(expectedCount, entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(removeLinkCaptor.getValue(), currentLinkCaptor.getValue());
        Assertions.assertEquals(expectedLink, currentLinkCaptor.getValue().name());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());
        Assertions.assertEquals(expectedTableName, createTableCaptor.getValue().name());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }

    @Test
    void testRunExportOnMonday() {
        successfulRunOnCertainDay(DayOfWeek.MONDAY, NewQuestionExportExecutor.MONDAY_QUESTION_EXPORT_LINK, NewQuestionExportExecutor.MONDAY_EXPORT_DAYS);
    }

    @Test
    void testRunExportOnThursday() {
        successfulRunOnCertainDay(DayOfWeek.THURSDAY, NewQuestionExportExecutor.THURSDAY_QUESTION_EXPORT_LINK, NewQuestionExportExecutor.THURSDAY_EXPORT_DAYS);
    }

    @Test
    void testOkRunForWeek() {
        int countToGenerate = 40;
        int delay = 12;
        ChronoUnit delayUnit = ChronoUnit.HOURS;

        int expectedCount = 14;

        String expectedLink = NewQuestionExportExecutor.LAST_LINK;

        LocalDateTime currentDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endDate = currentDay.minus(NewQuestionExportExecutor.WEEK_EXPORT_DAYS, ChronoUnit.DAYS);
        LocalDateTime startDate = endDate.minus(NewQuestionExportExecutor.WEEK_EXPORT_DAYS, ChronoUnit.DAYS);
        String expectedTableName = String.format(
            "%s-%s",
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );

        for (int idx = 0; idx < countToGenerate; idx++) {
            Question question = questionService.createModelQuestion(
                idx,
                UUID.randomUUID().toString(),
                countToGenerate - idx
            );

            jdbcTemplate.update(
                "update qa.question " +
                    "set cr_time = ? " +
                    "where id = ?",
                LocalDateTime.now().minus(idx * delay, delayUnit),
                question.getId());
        }

        Mockito.when(dayOfWeekProvider.getCurrentDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
        executor.exportForWeek();

        Mockito.verify(ytClient).createTable(createTableCaptor.capture(), eq(QuestionForUpload.tableSchema()));
        Mockito.verify(ytClient).append(appendCaptor.capture(), entitiesToUploadCaptor.capture());
        Mockito.verify(ytClient).removeLink(any(), removeLinkCaptor.capture());
        Mockito.verify(ytClient).createLink(any(), createLinkCaptor.capture(), currentLinkCaptor.capture());

        Assertions.assertEquals(expectedCount, entitiesToUploadCaptor.getValue().size());
        Assertions.assertEquals(removeLinkCaptor.getValue(), currentLinkCaptor.getValue());
        Assertions.assertEquals(expectedLink, currentLinkCaptor.getValue().name());
        Assertions.assertEquals(createTableCaptor.getValue(), appendCaptor.getValue());

        Assertions.assertEquals(expectedTableName, createTableCaptor.getValue().name());
        Assertions.assertEquals(createLinkCaptor.getValue(), appendCaptor.getValue());
    }
}
