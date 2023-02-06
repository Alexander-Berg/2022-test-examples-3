package ru.yandex.chemodan.app.telemost.services;

import javax.annotation.PostConstruct;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.exceptions.ConferenceLinkExpiredException;
import ru.yandex.chemodan.app.telemost.exceptions.ConferenceNotFoundTelemostException;
import ru.yandex.chemodan.app.telemost.repository.dao.BroadcastDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ClientConfigurationDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceDtoDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceStateDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceUserDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceDto;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceUserDto;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.services.model.Broadcast;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.XMPPLimitType;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;
import ru.yandex.chemodan.app.telemost.web.v2.model.BroadcastInitData;
import ru.yandex.inside.passport.PassportUid;

@ContextConfiguration(classes = ConferenceServiceTest.Context.class)
public class ConferenceServiceTest extends TelemostBaseContextTest {

    private static final PassportOrYaTeamUid EXTERNAL_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(18));
    private static final PassportOrYaTeamUid INTERNAL_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(6));
    private static final PassportOrYaTeamUid YA_TEAM_UID = PassportOrYaTeamUid.yaTeamUid("yt:66666777");
    private static final PassportOrYaTeamUid STAFF_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(410033615));
    private static final PassportOrYaTeamUid UNLIM_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(376338392));

    private static final String CLIENT_CONFIGURATION = "{\n" +
            "    \"v1\": {\n" +
            "        \"configuration\": \"v1_value\"\n" +
            "    },\n" +
            "    \"v2\": {\n" +
            "        \"configuration\": \"v2_value\"\n" +
            "    }\n" +
            "}";

    @Autowired
    private ClientConfigurationDao clientConfigurationDao;

    @Autowired
    private ConferenceStateDao conferenceStateDao;

    @Autowired
    private ConferenceDtoDao conferenceDtoDao;

    @Autowired
    private ConferenceUserDao conferenceUserDao;

    @Autowired
    private BroadcastDao broadcastDao;

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private ConferenceHelper conferenceHelper;

    @Autowired
    private StaffService staffServiceMock;

    @Autowired
    private PropertyManager propertyManager;

    @PostConstruct
    public void init() {
        super.before();
    }

    @Before
    public void removeClientConfigurations() {
        super.before();

        broadcastDao.deleteAll();
        conferenceUserDao.deleteAll();
        conferenceStateDao.deleteAll();
        conferenceDtoDao.deleteAll();
        clientConfigurationDao.deleteAll();

        Mockito.when(staffServiceMock.resolveUid(Option.of(YA_TEAM_UID))).thenReturn(Option.of(STAFF_UID.getPassportUid()));

        userService.addUserIfNotExists(EXTERNAL_UID);
        userService.addUserIfNotExists(INTERNAL_UID);
        userService.addUserIfNotExists(YA_TEAM_UID);
        userService.addUserIfNotExists(STAFF_UID);
        userService.addUserIfNotExists(UNLIM_UID);

        Mockito.reset(staffServiceMock);
    }

    @Test
    public void testStaffLimitTypeByUserRegistry() {
        Conference conference = conferenceHelper.createConference(createTestStaffUserForUid(EXTERNAL_UID),
                Option.of(Boolean.FALSE));
        Assert.assertEquals(XMPPLimitType.STAFF, conference.getLimitType());
    }

    @Test
    public void testStaffLimitTypeByList() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(STAFF_UID));
        Assert.assertEquals(XMPPLimitType.STAFF, conference.getLimitType());
    }

    @Test
    public void testUnlimTypeByList() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(UNLIM_UID));
        Assert.assertEquals(XMPPLimitType.UNLIM, conference.getLimitType());
    }

    @Test
    public void testDefaultLimitType() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(INTERNAL_UID));
        Assert.assertEquals(XMPPLimitType.DEFAULT, conference.getLimitType());
    }

    @Test
    public void testGettingConferenceByShortUrlId() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(INTERNAL_UID));
        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(conference.getUri());

        Conference conferenceFromStore = conferenceService.joinConference(
                Option.of(createTestStaffUserForUid(STAFF_UID)), conferenceUriData, conferenceUriData.getUserToken(),
                Option.empty());
        Assert.assertEquals(conference, conferenceFromStore);
    }

    @Test
    public void testGettingConferenceByBroadcastUri() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(INTERNAL_UID));

        Broadcast broadcast = broadcastService.createBroadcast(conference.getDbId(), INTERNAL_UID.asString(),
                new BroadcastInitData());

        ConferenceDto conferenceDto = conferenceService.findByBroadcastUri(broadcast.getUri());

        Assert.assertEquals(conference.getConferenceDto().getShortUrlId(), conferenceDto.getShortUrlId());
    }

    @Test
    public void testCreationAndGettingServiceWithEmptyOwnerUid() {
        @SuppressWarnings("deprecation")
        Conference conference = conferenceHelper.createConference();
        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(conference.getUri());

        Conference conferenceFromDb = conferenceService.joinConference(
                Option.empty(), conferenceUriData, conferenceUriData.getUserToken(), Option.empty());
        Assert.assertFalse(conferenceUserDao.findOwner(conferenceFromDb.getDbId()).isPresent());

        Conference updatedConference = conferenceService.joinConference(
                Option.of(createTestUserForUid(STAFF_UID)), conferenceUriData, conferenceUriData.getUserToken(),
                Option.empty());
        Option<ConferenceUserDto> owner = conferenceUserDao.findOwner(updatedConference.getDbId());
        Assert.assertTrue(owner.isPresent());
        Assert.assertEquals(STAFF_UID.asString(), owner.get().getUid());
    }

    @Test
    public void addStaffUserAsAdminIfOwnerIsYaTeam() {
        Mockito.when(staffServiceMock.resolveUid(Option.of(YA_TEAM_UID))).thenReturn(Option.of(STAFF_UID.getPassportUid()));
        Conference conference = conferenceHelper.createConference(YA_TEAM_UID);
        checkUsersCount(conference, 2);
        checkConferenceUser(conference, YA_TEAM_UID, UserRole.OWNER);
        checkConferenceUser(conference, STAFF_UID, UserRole.ADMIN);
    }

    @Test
    public void makeFirstConnectedUserAdminIfOwnerIsYaTeamAndHaveNoStaffAccount() {
        Mockito.when(staffServiceMock.resolveUid(Option.of(YA_TEAM_UID))).thenReturn(Option.empty());
        Mockito.when(staffServiceMock.resolveUid(Mockito.anyString())).thenReturn(Option.empty());
        Conference conference = conferenceHelper.createConference(YA_TEAM_UID);
        checkUsersCount(conference, 1);
        checkConferenceUser(conference, YA_TEAM_UID, UserRole.OWNER);
        conferenceHelper.connect(conference, EXTERNAL_UID);
        checkUsersCount(conference, 2);
        checkConferenceUser(conference, EXTERNAL_UID, UserRole.ADMIN);
    }

    @Test
    public void doNotAddAdminIfOwnerIsYaTeamAndAdminAlreadyExists() {
        Mockito.when(staffServiceMock.resolveUid(Option.of(YA_TEAM_UID))).thenReturn(Option.of(STAFF_UID.getPassportUid()));
        Conference conference = conferenceHelper.createConference(YA_TEAM_UID);
        conferenceHelper.connect(conference, EXTERNAL_UID);
        checkUsersCount(conference, 3);
        checkConferenceUser(conference, YA_TEAM_UID, UserRole.OWNER);
        checkConferenceUser(conference, STAFF_UID, UserRole.ADMIN);
        checkConferenceUser(conference, EXTERNAL_UID, UserRole.MEMBER);
    }

    @Test
    public void createdConferenceHaveOwnerInUsers() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(EXTERNAL_UID));
        checkConferenceUser(conference, EXTERNAL_UID, UserRole.OWNER);
    }

    @Test
    public void createConferenceUserWhenConnect() {
        Conference conference = conferenceHelper.createConference(createTestUserForUid(EXTERNAL_UID));
        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(conference.getUri());
        conferenceService.joinConference(
                Option.of(createTestUserForUid(INTERNAL_UID)), conferenceUriData, conferenceUriData.getUserToken(),
                Option.empty());
        checkConferenceUser(conference, INTERNAL_UID, UserRole.MEMBER);
    }

    @Test(expected = ConferenceLinkExpiredException.class)
    public void testNonPermanentTtl() {
        @SuppressWarnings("deprecation")
        Conference conference = conferenceHelper.createConference();
        DateTimeUtils.setCurrentMillisOffset(
                Duration.standardSeconds(propertyManager.getConferenceTtl() + 5).getMillis());
        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(conference.getUri());
        conferenceService.joinConference(
                Option.empty(), conferenceUriData, conferenceUriData.getUserToken(),
                Option.empty());
    }

    @Test(expected = ConferenceNotFoundTelemostException.class)
    public void testNotExistingConference() {
        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData("https://telemost.yandex" +
                ".ru/j/fake");
        conferenceService.joinConference(
                Option.empty(), conferenceUriData, conferenceUriData.getUserToken(),
                Option.empty());
    }

    @Test
    public void testPermanentTtl() {
        @SuppressWarnings("deprecation")
        Conference conference = conferenceHelper.createConference(true);
        DateTimeUtils.setCurrentMillisOffset(
                Duration.standardSeconds(propertyManager.getConferenceTtl() + 5).getMillis());

        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(conference.getUri());
        Assert.assertEquals(38, conferenceUriData.getShortUrlId().length());

        Conference conferenceFromDb = conferenceService.joinConference(
                Option.empty(), conferenceUriData, conferenceUriData.getUserToken(), Option.empty());
        Assert.assertEquals(conference.getConferenceId(), conferenceFromDb.getConferenceId());
        Assert.assertEquals(conference.getConferencePassword(), conferenceFromDb.getConferencePassword());
    }

    private void checkConferenceUser(Conference conference, PassportOrYaTeamUid uid, UserRole role) {
        Option<ConferenceUserDto> conferenceUser = conferenceUserDao.findByConferenceAndUid(conference.getDbId(),
                uid);
        Assert.assertTrue(conferenceUser.isPresent());
        Assert.assertEquals(role, conferenceUser.get().getRole());
    }

    private void checkUsersCount(Conference conference, int count) {
        Assert.assertEquals(count, conferenceUserDao.findByConference(conference.getDbId()).size());
    }

    @Configuration
    public static class Context {
        @Bean
        @Primary
        StaffService staffService() {
            return Mockito.mock(StaffService.class);
        }
    }
}
