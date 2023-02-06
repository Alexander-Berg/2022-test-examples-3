package ru.yandex.chemodan.app.telemost.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.exceptions.YaTeamTokenAccessDeniedTelemostException;
import ru.yandex.chemodan.app.telemost.repository.model.UserTokenDto;
import ru.yandex.chemodan.app.telemost.repository.model.UserTokenStatus;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.XMPPLimitType;
import ru.yandex.inside.passport.PassportUid;

public class ConferenceYaTeamServiceTest extends TelemostBaseContextTest {

    private static final PassportOrYaTeamUid UID = PassportOrYaTeamUid.yaTeamUid("yt:437891");

    private static final PassportOrYaTeamUid UID_2 = PassportOrYaTeamUid.yaTeamUid("yt:43783");

    private static final PassportUid PUID = PassportUid.cons(1234L);

    @Autowired
    private UserTokenService userTokenService;

    @Before
    public void init() {
        userService.addUserIfNotExists(UID);
        userService.addUserIfNotExists(UID_2);
        userService.addUserIfNotExists(PassportOrYaTeamUid.passportUid(PUID));
    }

    @Test
    public void testStaffLimit() {
        Conference conference =
                conferenceService.generateConference(
                        ConferenceClientParameters.builder()
                                .user(Option.of(createTestStaffUserForUid(UID)))
                                .permanent(Option.of(Boolean.FALSE))
                                .staffOnly(Option.of(Boolean.FALSE))
                                .externalMeeting(Option.empty())
                                .eventId(Option.empty()).build());
        Assert.assertEquals(XMPPLimitType.STAFF, conference.getLimitType());
    }

    @Test(expected = YaTeamTokenAccessDeniedTelemostException.class)
    public void testNonPermanentTokenExpiration() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceUriService.getYaTeamConferenceUriData(conference.getUri());
        String uri = conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID_2), conferenceUriData);
        ConferenceUriData authorizedUriData = conferenceUriService.getConferenceUriData(uri);
        expireToken(conference.getDbId(), authorizedUriData.getUserToken().get(), UserTokenService.DEFAULT_USER_TOKEN_TTL_SECS);
        conferenceService.joinConference(
                Option.of(createTestUserForUid(PassportOrYaTeamUid.passportUid(PUID))), conferenceUriData,
                authorizedUriData.getUserToken(), Option.empty());
    }

    @Test
    public void testSuccessfulAuthorization() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceUriService.getYaTeamConferenceUriData(conference.getUri());
        String uri = conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID_2), conferenceUriData);
        ConferenceUriData authorizedUriData = conferenceUriService.getConferenceUriData(uri);
        Assert.assertEquals(conferenceUriData.getShortUrlId(), authorizedUriData.getShortUrlId());
        Assert.assertTrue(authorizedUriData.getUserToken().isPresent());
    }

    @Test
    public void testEmptyAuthorization() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceUriService.getYaTeamConferenceUriData(conference.getUri());
        String uri = conferenceService.getYaTeamAuthorizationConferenceUri(Option.empty(), conferenceUriData);
        ConferenceUriData authorizedUriData = conferenceUriService.getConferenceUriData(uri);
        Assert.assertEquals(conferenceUriData.getShortUrlId(), authorizedUriData.getShortUrlId());
        Assert.assertFalse(authorizedUriData.getUserToken().isPresent());
    }

    @Test
    public void testTokenCheckWithoutActivation() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceUriService.getYaTeamConferenceUriData(conference.getUri());
        String uri = conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID_2), conferenceUriData);
        ConferenceUriData authorizedUriData = conferenceUriService.getConferenceUriData(uri);

        Conference conference1 = conferenceService.findOrCreateConferenceWithoutTokenActivating(
                Option.of(createTestStaffUserForUid(UID)), authorizedUriData, authorizedUriData.getUserToken());
        Assert.assertNotNull(conference1);
        Assert.assertEquals(conference.getConferenceId(), conference1.getConferenceId());

        Option<UserTokenDto> token =
                userTokenService.getCreatedOrActiveToken(conference.getDbId(), UID, authorizedUriData.getUserToken().get());
        Assert.assertTrue(token.isPresent());
        Assert.assertEquals(UserTokenStatus.CREATED.name(), token.get().getStatus());
    }

    @Test(expected = YaTeamTokenAccessDeniedTelemostException.class)
    public void testBadTokenCheckWithoutActivation() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceUriService.getYaTeamConferenceUriData(conference.getUri());
        String uri = conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID_2), conferenceUriData);
        ConferenceUriData authorizedUriData = conferenceUriService.getConferenceUriData(uri);
        conferenceService.findOrCreateConferenceWithoutTokenActivating(Option.of(createTestStaffUserForUid(UID)),
                new ConferenceUriData(
                        authorizedUriData.getShortUrlId(), authorizedUriData.getUserToken().map(token -> token + "-")),
                authorizedUriData.getUserToken());
    }

    @Test(expected = YaTeamTokenAccessDeniedTelemostException.class)
    public void testBadUidCheckWithoutActivation() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.of(createTestStaffUserForUid(UID)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        ConferenceUriData conferenceUriData = conferenceUriService.getYaTeamConferenceUriData(conference.getUri());
        String uri = conferenceService.getYaTeamAuthorizationConferenceUri(Option.of(UID_2), conferenceUriData);
        ConferenceUriData authorizedUriData = conferenceUriService.getConferenceUriData(uri);

        Conference conference1 = conferenceService.findOrCreateConference(
                Option.of(createTestStaffUserForUid(UID)), authorizedUriData, authorizedUriData.getUserToken());
        Assert.assertNotNull(conference1);

        conferenceService.findOrCreateConferenceWithoutTokenActivating(
                Option.of(createTestStaffUserForUid(UID_2)), authorizedUriData, authorizedUriData.getUserToken());
    }

}
