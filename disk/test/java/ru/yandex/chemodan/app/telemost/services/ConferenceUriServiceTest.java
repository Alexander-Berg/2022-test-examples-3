package ru.yandex.chemodan.app.telemost.services;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.exceptions.BadUriTelemostException;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.inside.passport.PassportUid;

public class ConferenceUriServiceTest extends TelemostBaseContextTest {

    private static final Pattern URI_PATTERN =
            Pattern.compile("https:\\/\\/telemost\\.yandex\\.ru\\/j\\/\\d{14,38}");

    @Test
    public void testUriCreation() {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(1));
        userService.addUserIfNotExists(passportUser);

        String uri = conferenceService.generateConference(
                ConferenceClientParameters.builder()
                        .user(Option.of(createTestUserForUid(passportUser)))
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build()).getUri();
        Assert.assertTrue(URI_PATTERN.matcher(uri).matches());
    }

    @Test
    public void testUriParsing() {
        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData("https://telemost.yandex.ru/j/12345678901234");
        Assert.assertEquals("12345678901234", conferenceUriData.getShortUrlId());
    }

    @Test(expected = BadUriTelemostException.class)
    public void testIncorrectUriParsing() {
        conferenceUriService.getConferenceUriData("https://telemost-bad.yandex.ru/j/12345678901234");
    }
}
