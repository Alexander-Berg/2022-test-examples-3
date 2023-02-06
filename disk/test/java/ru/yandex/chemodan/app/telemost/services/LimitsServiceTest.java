package ru.yandex.chemodan.app.telemost.services;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceStateDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceDto;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.XMPPLimitType;

public class LimitsServiceTest extends TelemostBaseContextTest {

    @Autowired
    private LimitsService limitsService;

    @Autowired
    ConferenceStateDao conferenceStateDao;

    @Autowired
    ConferenceService conferenceService;

    @Test
    public void testDefaultLimitValue() {
        Assert.assertEquals(XMPPLimitType.STAFF, limitsService.getV1LimitType(XMPPLimitType.DEFAULT));
    }

    @Test
    public void testNonDefaultLimitType() {
        Assert.assertEquals(XMPPLimitType.UNLIM,  limitsService.getV1LimitType(XMPPLimitType.UNLIM));
    }

    @Test
    public void testLimitValues() {
        Conference conference = new Conference(conferenceService, ConferenceDto.create("", "", XMPPLimitType.DEFAULT.name(),
                new Instant(), "", Option.empty(), false, false, false,
                Option.empty()),
                "", conferenceStateDao);
        Assert.assertEquals(Integer.valueOf(35), limitsService.getParticipantsLimitValue(conference).get());
        Assert.assertFalse(limitsService.getParticipantsLimitValue(conference.withLimitType(XMPPLimitType.STAFF)).isPresent());
        Assert.assertFalse(limitsService.getParticipantsLimitValue(conference.withLimitType(XMPPLimitType.UNLIM)).isPresent());
    }
}
