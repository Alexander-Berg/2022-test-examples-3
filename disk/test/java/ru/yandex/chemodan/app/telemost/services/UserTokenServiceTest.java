package ru.yandex.chemodan.app.telemost.services;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.repository.model.UserTokenDto;
import ru.yandex.chemodan.app.telemost.repository.model.UserTokenStatus;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class UserTokenServiceTest extends TelemostBaseContextTest {

    private final static PassportOrYaTeamUid UID = PassportOrYaTeamUid.yaTeamUid("yt:1234567");

    private final static PassportUid OWNER_UID = PassportUid.cons(12235);

    private final static PassportUid INVALID_UID = PassportUid.cons(12236);

    @Autowired
    private UserTokenService userTokenService;

    @Before
    public void init() {
        userService.addUserIfNotExists(UID);
        userService.addUserIfNotExists(PassportOrYaTeamUid.passportUid(OWNER_UID));
        userService.addUserIfNotExists(PassportOrYaTeamUid.passportUid(INVALID_UID));
    }

    @Test
    public void testActivationToken() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                                .user(Option.of(createTestStaffUserForUid(UID)))
                                .permanent(Option.empty())
                                .staffOnly(Option.empty())
                                .externalMeeting(Option.empty())
                                .eventId(Option.empty()).build()
                );
        ConferenceUriData conferenceUriData = conferenceService.getYaTeamConferenceUriId(conference.getUri());
        ConferenceUriData authorizedUriData = conferenceService
                .getConferenceUriId(conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID), conferenceUriData));
        Option<UserTokenDto> userTokenDtoO = userTokenService
                .getActivatedOrActivateToken(conference.getDbId(), UID, authorizedUriData.getUserToken().get());
        Assert.assertTrue(userTokenDtoO.isPresent());
        Assert.assertEquals(UserTokenStatus.ACTIVATED.name(), userTokenDtoO.get().getStatus());
    }

    @Test
    public void testFailActivationToken() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.empty())
                        .staffOnly(Option.empty())
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceService.getYaTeamConferenceUriId(conference.getUri());
        Option<UserTokenDto> userTokenDtoO = userTokenService
                .getActivatedOrActivateToken(conference.getDbId(), UID, "fake");
        ConferenceUriData authorizedUriData = conferenceService
                .getConferenceUriId(conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID), conferenceUriData));
        Assert.assertFalse(userTokenDtoO.isPresent());
        Assert.assertFalse(userTokenService
                .getActivatedOrActivateToken(UUID.randomUUID(), UID, authorizedUriData.getUserToken().get()).isPresent());
    }

    @Test
    public void testExpiredActivationToken() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.empty())
                        .staffOnly(Option.empty())
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceService.getYaTeamConferenceUriId(conference.getUri());
        ConferenceUriData authorizedUriData = conferenceService
                .getConferenceUriId(conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID), conferenceUriData));
        expireToken(conference.getDbId(), authorizedUriData.getUserToken().get(), UserTokenService.DEFAULT_USER_TOKEN_TTL_SECS);
        Option<UserTokenDto> userTokenDtoO = userTokenService
                .getActivatedOrActivateToken(conference.getDbId(), UID, authorizedUriData.getUserToken().get());
        Assert.assertFalse(userTokenDtoO.isPresent());
    }

    @Test
    public void testActivatedToken() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.empty())
                        .staffOnly(Option.empty())
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceService.getYaTeamConferenceUriId(conference.getUri());
        ConferenceUriData authorizedUriData = conferenceService
                .getConferenceUriId(conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID), conferenceUriData));
        PassportOrYaTeamUid ownerUid = PassportOrYaTeamUid.passportUid(OWNER_UID);
        Option<UserTokenDto> userTokenDtoO = userTokenService
                .getActivatedOrActivateToken(conference.getDbId(), ownerUid, authorizedUriData.getUserToken().get());
        Assert.assertTrue(userTokenDtoO.isPresent());
        Assert.assertEquals(UserTokenStatus.ACTIVATED.name(), userTokenDtoO.get().getStatus());

        Option<UserTokenDto> userTokenDtoActivatedO = userTokenService
                .getActivatedOrActivateToken(conference.getDbId(), ownerUid, authorizedUriData.getUserToken().get());
        Assert.assertTrue(userTokenDtoActivatedO.isPresent());
        Assert.assertEquals(UserTokenStatus.ACTIVATED.name(), userTokenDtoActivatedO.get().getStatus());

        Option<UserTokenDto> userTokenDtoEmptyO = userTokenService
                .getActivatedOrActivateToken(conference.getDbId(), PassportOrYaTeamUid.passportUid(INVALID_UID), authorizedUriData.getUserToken().get());
        Assert.assertFalse(userTokenDtoEmptyO.isPresent());

    }
}
