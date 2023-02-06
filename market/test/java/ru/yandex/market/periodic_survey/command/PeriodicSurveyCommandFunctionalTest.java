package ru.yandex.market.periodic_survey.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.core.periodic_survey.service.provider.PeriodicSurveyPartnersProvider;
import ru.yandex.market.core.periodic_survey.service.scheduler.PeriodicSurveySchedulerService;
import ru.yandex.market.core.periodic_survey.yt.PeriodicSurveyYtDao;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.shop.FunctionalTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "PeriodicSurveyCommandFunctionalTest.before.csv")
public class PeriodicSurveyCommandFunctionalTest extends FunctionalTest {
    @Autowired
    private ProtocolService protocolService;
    @Autowired
    private PeriodicSurveySchedulerService schedulerService;
    @Autowired
    private List<PeriodicSurveyPartnersProvider> partnersProviders;
    @Autowired
    PeriodicSurveyYtDao periodicSurveyYtDao;
    @Autowired
    Clock clock;

    private PeriodicSurveyCommand command;
    private ByteArrayInputStream bais;
    private ByteArrayOutputStream baos;
    private Terminal terminal;

    @BeforeEach
    void setUp() {
        command = new PeriodicSurveyCommand(protocolService, schedulerService, partnersProviders, clock);
        bais = new ByteArrayInputStream(new byte[0]);
        baos = new ByteArrayOutputStream();
        terminal = new Terminal(bais, baos) {
            @Override
            protected void onStart() {

            }

            @Override
            protected void onClose() {

            }
        };
    }

    @Test
    @DisplayName("проверяем команду показа списка ID опросов")
    public void testYtUpdate() {
        when(clock.instant()).thenReturn(Instant.EPOCH.plus(114, ChronoUnit.DAYS));
        CommandInvocation commandInvocation = new CommandInvocation("periodic-survey-management",
                new String[]{"preview-survey-ids", "NPS_DROPSHIP", "10"},
                Map.of());
        baos.reset();

        command.executeCommand(commandInvocation, terminal);
        terminal.getWriter().flush();

        String expected = "Calculated survey ids for NPS_DROPSHIP are:\n" +
                "\tSurveyId{partnerId=114, userId=1140, surveyType=NPS_DROPSHIP, createdAt=1970-04-25T00:00:00Z}\n" +
                "\tSurveyId{partnerId=294, userId=2940, surveyType=NPS_DROPSHIP, createdAt=1970-04-25T00:00:00Z}\n";
        Assertions.assertEquals(expected, baos.toString());
    }

    @Test
    @DisplayName("проверяем команду создания опросов для списка партнеров")
    public void testCreateSurveysForPartners() {
        CommandInvocation commandInvocation = new CommandInvocation("periodic-survey-management",
                new String[]{"create", "NPS_DROPSHIP", "120,121"},
                Map.of());
        baos.reset();

        command.executeCommand(commandInvocation, terminal);
        terminal.getWriter().flush();

        Assertions.assertEquals("Survey NPS_DROPSHIP for partners will be created: [120, 121]\nOK\n",
                baos.toString());

        ArgumentCaptor<List<SurveyRecord>> partnerUserCaptor = ArgumentCaptor.forClass(List.class);
        verify(periodicSurveyYtDao, atLeastOnce()).upsertRecords(partnerUserCaptor.capture());
        var createdSurveys = partnerUserCaptor.getAllValues().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(createdSurveys.stream()
                        .map(SurveyRecord::getSurveyId)
                        .map(id -> Map.entry(id.getPartnerId(), id.getUserId()))
                        .collect(Collectors.toList()),
                containsInAnyOrder(
                        Map.entry(120L, 1200L),
                        Map.entry(120L, 1210L)
                )
        );
    }

    @Test
    @DisplayName("проверяем команду удаления открытых опросов по списку партнеров")
    public void testDeleteOpenSurveysForPartners() {
        SurveyRecord survey = getSurveyRecord();
        when(periodicSurveyYtDao.getPartnersSurveys(eq(List.of(120L, 121L)), eq(SurveyStatus.OPEN_SURVEY_STATUSES)))
                .thenReturn(List.of(survey));
        CommandInvocation commandInvocation = new CommandInvocation("periodic-survey-management",
                new String[]{"delete-open-surveys", "120,121"},
                Map.of());
        baos.reset();

        command.executeCommand(commandInvocation, terminal);
        terminal.getWriter().flush();

        Assertions.assertEquals("Open surveys for partners [120, 121] will be deleted\nOK\n",
                baos.toString());

        ArgumentCaptor<List<SurveyId>> partnerUserCaptor = ArgumentCaptor.forClass(List.class);
        verify(periodicSurveyYtDao).deleteSurveys(partnerUserCaptor.capture());
        var surveyIds = partnerUserCaptor.getValue();
        assertThat(surveyIds, equalTo(List.of(survey.getSurveyId())));
    }

    private SurveyRecord getSurveyRecord() {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(103L, 0L, SurveyType.NPS_DROPSHIP, Instant.now()))
                .withStatus(SurveyStatus.COMPLETED)
                .build();
    }
}
