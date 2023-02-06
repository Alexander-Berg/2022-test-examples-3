package ru.yandex.calendar.logic.sending.so;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.ParticipantsOrInvitationsData;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsProperty;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantsData;
import ru.yandex.calendar.micro.MicroCoreContext;
import ru.yandex.calendar.micro.so.Form;
import ru.yandex.calendar.micro.so.SoCheckClient;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.io.http.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.calendar.logic.sending.so.SoChecker.SO_CHECKER_METRIC_ERROR;
import static ru.yandex.calendar.logic.sending.so.SoChecker.SO_CHECKER_METRIC_HAM;
import static ru.yandex.calendar.logic.sending.so.SoChecker.SO_CHECKER_METRIC_SPAM;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SoCheckerSmallTest.SoCheckerSmallTestConf.class)
public class SoCheckerSmallTest {
    @Autowired
    private SoChecker soChecker;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private SoCheckClient client;

    private static final EnvironmentType ENVIRONMENT_TYPE = EnvironmentType.TESTS;
    private static final String SUBJECT = "SUBJECT";
    private static final long FORM_AUTHOR = 12345L;
    private static final String URL = "URL";
    private static final String LOCATION = "LOCATION";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String ID = "12345";
    private static final Instant START_INSTANT = DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0).toInstant();
    private static final Instant END_INSTANT = START_INSTANT.plus(10000);
    private static final List<String> PARTICIPANTS = List.of("participant1@example.com", "participant2@example.com");
    private static final ActionInfo ACTION_INFO = new ActionInfo("testAction", ActionSource.WEB, "123456", Instant.now());
    private static final IcsVEvent SIMPLE_ICS_V_EVENT = getIcsVEvent("");
    private static final Form FORM = SoChecker.constructForm(
            ENVIRONMENT_TYPE, ACTION_INFO.getActionSource(), ACTION_INFO.getAction(), Optional.of(SUBJECT), new PassportUid(FORM_AUTHOR), Optional.of(URL),
            new Form.FormFields(LOCATION, DESCRIPTION, SUBJECT, ID, START_INSTANT.toString(), END_INSTANT.toString(), PARTICIPANTS));

    private static final EventData EVENT_DATA = new EventData();
    static {
        val participantsDataList = StreamEx.of(PARTICIPANTS)
                .limit(PARTICIPANTS.size() - 1)
                .map(Email::new)
                .map(e -> new ParticipantData(e, "", Decision.UNDECIDED, true, false, false)) //just participants
                .append(new ParticipantData(new Email(PARTICIPANTS.get(PARTICIPANTS.size() - 1)), //organizer
                        "", Decision.UNDECIDED, true, true, false))
                .collect(CollectorsF.toList());
        val event = EVENT_DATA.getEvent();
        event.setName(SUBJECT);
        event.setUrl(URL);
        event.setId(Long.parseLong(ID));
        event.setLocation(LOCATION);
        event.setDescription(DESCRIPTION);
        event.setStartTs(START_INSTANT);
        event.setEndTs(END_INSTANT);
        EVENT_DATA.setParticipantsData(ParticipantsOrInvitationsData.participantsData(new ParticipantsData.Meeting(participantsDataList)));
    }


    @BeforeEach
    public void setUp() {
        reset(client);
        reset(meterRegistry);
        when(meterRegistry.counter(anyString())).thenReturn(mock(Counter.class));
    }

    @Test
    public void validateEventNotSpam() {
        when(client.checkForm(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(false));

        soChecker.validateEvent(new PassportUid(FORM_AUTHOR), EVENT_DATA, ACTION_INFO);
        verify(meterRegistry, only()).counter(SO_CHECKER_METRIC_HAM);
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void validateEventSpam() {
        when(client.checkForm(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertThatThrownBy(() -> soChecker.validateEvent(new PassportUid(FORM_AUTHOR), EVENT_DATA, ACTION_INFO))
                .isInstanceOf(SoCheckFailedException.class);
        verify(meterRegistry, only()).counter(SO_CHECKER_METRIC_SPAM);
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void validateEventRequestException() {
        when(client.checkForm(any(), any(), any(), any()))
                .thenThrow(new RuntimeException());

        soChecker.validateEvent(new PassportUid(FORM_AUTHOR), EVENT_DATA, ACTION_INFO);
        verify(meterRegistry, only()).counter(SO_CHECKER_METRIC_ERROR);
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void checkIcsEventNotSpam() {
        when(client.checkForm(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(false));

        assertThat(soChecker.isEventSpam(new PassportUid(FORM_AUTHOR), SIMPLE_ICS_V_EVENT, ACTION_INFO, Optional.empty()))
                .isFalse();
        verify(meterRegistry, only()).counter(SO_CHECKER_METRIC_HAM);
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void checkIcsEventSpam() {
        when(client.checkForm(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertThat(soChecker.isEventSpam(new PassportUid(FORM_AUTHOR), SIMPLE_ICS_V_EVENT, ACTION_INFO, Optional.empty()))
                .isTrue();
        verify(meterRegistry, only()).counter(SO_CHECKER_METRIC_SPAM);
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void checkIcsEventRequestException() {
        when(client.checkForm(any(), any(), any(), any()))
                .thenThrow(new RuntimeException());

        assertThat(soChecker.isEventSpam(new PassportUid(FORM_AUTHOR), SIMPLE_ICS_V_EVENT, ACTION_INFO, Optional.empty()))
                .isFalse();
        verify(meterRegistry, only()).counter(SO_CHECKER_METRIC_ERROR);
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void constructFormFromEventData() {
        val realForm = soChecker.constructForm(new PassportUid(FORM_AUTHOR), EVENT_DATA, ACTION_INFO);
        assertThat(realForm).isEqualToComparingFieldByFieldRecursively(FORM);
    }

    @Test
    public void constructFormFromIcsVEvent() {
        val realForm = soChecker.constructForm(new PassportUid(FORM_AUTHOR), SIMPLE_ICS_V_EVENT, ACTION_INFO, Optional.of(URL));
        assertThat(realForm).isEqualToComparingFieldByFieldRecursively(FORM);
    }

    private static IcsVEvent getIcsVEvent(String suffix) {
        val participants = StreamEx.of(PARTICIPANTS)
                .map(Email::new)
                .map(IcsAttendee::new)
                .select(IcsProperty.class)
                .collect(CollectorsF.toList());
        return new IcsVEvent()
                .withProperties(participants)
                .withSummary(SUBJECT + suffix)
                .withLocation(LOCATION)
                .withDescription(DESCRIPTION)
                .withUid(ID)
                .withDtStart(START_INSTANT)
                .withDtEnd(END_INSTANT);
    }

    @Configuration
    public static class SoCheckerSmallTestConf {
        @Bean
        public SoCheckClient soCheckerClient() {
            return mock(SoCheckClient.class);
        }

        @Bean
        public SoChecker checker(SoCheckClient client) {
            val timeout = mock(Timeout.class);
            return new SoChecker("", timeout, client);
        }

        @Bean
        public MeterRegistry meterRegistry() {
            return mock(MeterRegistry.class);
        }

        @Bean
        public EnvironmentType environmentType() {
            return EnvironmentType.TESTS;
        }

        @Bean
        public MicroCoreContext microCoreContext() {
            return mock(MicroCoreContext.class);
        }
    }
}


