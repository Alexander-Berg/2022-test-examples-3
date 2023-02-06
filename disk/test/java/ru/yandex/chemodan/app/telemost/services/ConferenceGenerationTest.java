package ru.yandex.chemodan.app.telemost.services;

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.mock.properties.PropertyManagerStub;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceDtoDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceStateDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceUserDao;
import ru.yandex.chemodan.app.telemost.repository.dao.UserDao;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.time.MoscowTime;

public class ConferenceGenerationTest extends TelemostBaseContextTest {

    private static final String CONFERENCE_ID = "conf_id";

    private static final String CONFERENCE_PASSWORD = "conf_pwd";

    private static final PassportUidOrZero UID = PassportUidOrZero.fromUid(12);

    private final UserTokenService userTokenService = Mockito.mock(UserTokenService.class);

    private final HttpClient conferenceManagerHttpClient = Mockito.mock(HttpClient.class);

    private ConferenceManagerClient conferenceManagerClient;

    @Autowired
    private ConferenceDtoDao conferenceDtoDao;

    @Autowired
    private ConferenceStateDao conferenceStateDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ConferenceUserDao conferenceUserDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private StaffService staffService;

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private BroadcastUriService broadcastUriService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private ObjectMapper objectMapper;


    @Before
    public void init() {
        userService.addUserIfNotExists(PassportOrYaTeamUid.passportUid(UID.toPassportUid()));
        conferenceService = createConferenceService(CONFERENCE_ID,false);
        conferenceManagerClient = new ConferenceManagerClient(conferenceManagerHttpClient,
                URI.create("http://test.tcm.ru"), 1, objectMapper, false);
    }

    @Test
    public void testKeyGeneration() {
        Conference conference =
                conferenceService.generateConference(
                        ConferenceClientParameters.builder()
                                .user(Option.of(createUser()))
                                .permanent(Option.of(Boolean.FALSE))
                                .staffOnly(Option.of(Boolean.FALSE))
                                .externalMeeting(Option.empty())
                                .eventId(Option.empty()).build());
        Assert.assertEquals(CONFERENCE_ID, conference.getConferenceId());
        Assert.assertEquals(CONFERENCE_PASSWORD, conference.getConferencePassword());
        Assert.assertFalse(conference.isStaffOnly());
    }

    @Test
    public void testSendingConferenceInfoToTcm() throws IOException {
        conferenceService = createConferenceService("testSendingConferenceInfoToTcm",true);
        conferenceService.generateConference(
                ConferenceClientParameters.builder()
                        .user(Option.of(createUser()))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        Mockito.verify(conferenceManagerHttpClient, Mockito.times(1))
                .execute(Mockito.<HttpUriRequest>any(), Mockito.any(), Mockito.any());
    }

    private User createUser() {
        return new User(PassportOrYaTeamUid.passportUid(UID.toPassportUid()),
                Option.empty(), Option.empty(), Option.empty(), true, false,
                MoscowTime.TZ, "ru");
    }

    private ConferenceService createConferenceService(String confId, boolean saveConferenceInfoToTcm) {
        Supplier<String> passwordProvider = () -> CONFERENCE_PASSWORD;
        return new ConferenceService(() -> confId, conferenceDtoDao, conferenceStateDao,
                userDao, conferenceUserDao, calendarService,
                transactionTemplate, 1, conferenceUriService, userTokenService,
                Cf.list(new DefaultConferenceCreator(passwordProvider, conferenceUriService),
                        new YaTeamConferenceCreator(passwordProvider, conferenceUriService)),
                new LimitsService(), staffService, broadcastService, broadcastUriService, conferenceManagerClient,
                new PropertyManagerStub(), saveConferenceInfoToTcm);
    }
}
