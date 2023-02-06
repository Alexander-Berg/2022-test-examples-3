package ru.yandex.chemodan.app.telemost;

import java.util.UUID;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.testing.GrpcCleanupRule;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.config.TelemostWebActionsContextConfiguration;
import ru.yandex.chemodan.app.telemost.config.common.TelemostAdminDaemonContextConfiguration;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.mock.mediator.RoomGrpcMockService;
import ru.yandex.chemodan.app.telemost.mock.uaas.MockExperimentsManager;
import ru.yandex.chemodan.app.telemost.repository.dao.UserTokenDtoDao;
import ru.yandex.chemodan.app.telemost.repository.model.UserTokenDto;
import ru.yandex.chemodan.app.telemost.room.proto.MediatorOuterClass;
import ru.yandex.chemodan.app.telemost.services.ConferenceService;
import ru.yandex.chemodan.app.telemost.services.ConferenceUriService;
import ru.yandex.chemodan.app.telemost.services.UserService;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.chemodan.app.telemost.translator.TranslatorClient;
import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.chemodan.xiva.BasicXivaClient;
import ru.yandex.chemodan.xiva.XivaSecretSign;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.version.SimpleAppName;

@ContextConfiguration(classes = {
        TelemostAdminDaemonContextConfiguration.class,
        TelemostWebActionsContextConfiguration.class,
        TelemostTestContextConfiguration.class,
        TelemostTestBazingaContextConfiguration.class,
})
@ActiveProfiles({
        ActivateEmbeddedPg.EMBEDDED_PG
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TelemostBaseContextTest extends AbstractTest {

    protected static final XivaSecretSign DEFAULT_SECRET_SIGN = new XivaSecretSign("sign_value", "ts_value");

    public static final MapF<Long, UserData> usersData = Cf.hashMap();

    public static final PassportOrYaTeamUid TEST_OWNER = PassportOrYaTeamUid.passportUid(new PassportUid(11133));

    @Autowired
    @Qualifier("conferenceKeyService")
    protected ConferenceService conferenceService;

    @Autowired
    @Qualifier("conferenceUriService")
    protected ConferenceUriService conferenceUriService;

    @Autowired
    protected RoomGrpcMockService mockRoomGrpcService;

    @Autowired
    protected MockExperimentsManager mockExperimentsManager;

    @Autowired
    protected UserService userService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BasicXivaClient basicXivaClient;

    @Autowired
    private UserTokenDtoDao userTokenDtoDao;

    @Autowired
    private TranslatorClient translatorClient;

    @ClassRule
    public static GrpcCleanupRule grpcCleanupRule;

    @BeforeClass
    public static void setup()
    {
        AppNameHolder.setIfNotPresent(new SimpleAppName("telemost", "telemost-backend"));
        AbstractTest.setup();
        grpcCleanupRule = new GrpcCleanupRule();
    }

    @Before
    public void before()
    {
        DateTimeUtils.setCurrentMillisSystem();
        mockRoomGrpcService.cleanUp();
        Mockito.reset(translatorClient);
        userService.addUserIfNotExists(TEST_OWNER);
    }

    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    @PostConstruct
    public void initMocks()
    {
        mockBasicXivaClient(basicXivaClient);
    }

    protected void mockBasicXivaClient(BasicXivaClient xivaClient)
    {
        Mockito.when(xivaClient.getXivaSecretSign(Mockito.any(ListF.class), Mockito.any(ListF.class),
                Mockito.anyString(), Mockito.any(ObjectMapper.class))).thenReturn(getXivaSecretSign());
    }

    protected MediatorOuterClass.CreateRoomResponse getCreateRoomResponse()
    {
        return MediatorOuterClass.CreateRoomResponse.newBuilder().build();
    }

    protected MediatorOuterClass.AddParticipantResponse getAddParticipantResponse()
    {
        return MediatorOuterClass.AddParticipantResponse.newBuilder().build();
    }

    protected XivaSecretSign getXivaSecretSign()
    {
        return DEFAULT_SECRET_SIGN;
    }

    protected void expireToken(UUID conferenceId, String token, long expirationSecond)
    {
        Option<UserTokenDto> expiredToken = userTokenDtoDao.expireToken(conferenceId, token, expirationSecond);
        Assert.assertTrue(expiredToken.isPresent());
    }

    protected void addUsers(MapF<Long, UserData> users)
    {
        usersData.putAll(users);
    }

    protected void addUser(Long uid, UserData user)
    {
        usersData.put(uid, user);
    }

    protected void clearUserData()
    {
        usersData.clear();
    }

    protected User createTestUserForUid(long uid)
    {
        return createTestUserForUid(PassportOrYaTeamUid.passportUid(new PassportUid(uid)));
    }

    protected User createTestUserForUid(PassportOrYaTeamUid uid)
    {
        return new User(uid, Option.of("test"), Option.of("test"), Option.of(false), false, false,
                MoscowTime.TZ, "ru");
    }

    protected User createTestStaffUserForUid(PassportOrYaTeamUid uid)
    {
        return new User(uid, Option.of("test"), Option.of("test"), Option.of(false), true, false,
                MoscowTime.TZ, "ru");
    }

    protected void addExperimentFlagForUser(PassportUid uid, String flag)
    {
        mockExperimentsManager.addFlagsForUser(uid, Cf.list(flag));
    }

    protected Conference generateConference(User user) {
        return conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty())
                .build()
        );
    }
}
