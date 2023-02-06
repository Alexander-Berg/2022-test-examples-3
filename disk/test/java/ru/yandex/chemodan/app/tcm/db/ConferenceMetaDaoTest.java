package ru.yandex.chemodan.app.tcm.db;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.tcm.utils.ConferenceTestUtils;
import ru.yandex.chemodan.util.postgres.PgErrorUtils;
import ru.yandex.chemodan.util.test.AbstractTest;

/**
 * @author friendlyevil
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TcmDaoContextConfiguration.class
})
public class ConferenceMetaDaoTest extends AbstractTest {

    @Autowired
    private ConferenceMetaDao conferenceMetaDao;

    @Test
    public void testCreateConferenceMeta() {
        ConferenceMeta conferenceMeta = ConferenceTestUtils.generateConferenceMeta();
        ConferenceMeta createdConference = conferenceMetaDao.create(conferenceMeta);
        Assert.assertNotNull(createdConference.getCreatedAt());
    }

    @Test
    public void testFindByConferenceId() {
        ConferenceMeta conferenceMeta = ConferenceTestUtils.generateConferenceMeta();
        conferenceMetaDao.create(conferenceMeta);

        Option<ConferenceMeta> byConferenceId = conferenceMetaDao.findByConferenceId(conferenceMeta.getConferenceId());
        Assert.assertTrue(byConferenceId.isPresent());
        ConferenceTestUtils.assertConferenceMetaEquals(conferenceMeta, byConferenceId.get());
    }

    @Test
    public void testFindByShortUrlId() {
        ConferenceMeta conferenceMeta = ConferenceTestUtils.generateConferenceMeta();
        conferenceMetaDao.create(conferenceMeta);

        Option<ConferenceMeta> byShortUrlId = conferenceMetaDao.findByShortUrlId(conferenceMeta.getShortUrlId());
        Assert.assertTrue(byShortUrlId.isPresent());
        ConferenceTestUtils.assertConferenceMetaEquals(conferenceMeta, byShortUrlId.get());
    }

    @Test
    public void testCreateConferenceWithOneShortUrlId() {
        String shorUrlId = ConferenceTestUtils.generateShorUrlId();
        ConferenceMeta first = new ConferenceMeta(null, shorUrlId,
                ConferenceTestUtils.generateConferenceId(), 1, Option.empty(), Instant.now());
        ConferenceMeta second = new ConferenceMeta(null, shorUrlId,
                ConferenceTestUtils.generateConferenceId(), 1, Option.empty(), Instant.now());

        conferenceMetaDao.create(first);
        conferenceMetaDao.create(second);
    }

    @Test
    public void testUpdateConferenceMeta() {
        ConferenceMeta conferenceMeta = ConferenceTestUtils.generateConferenceMeta();
        conferenceMetaDao.create(conferenceMeta);

        ConferenceMeta updatedMeta = conferenceMeta.withExpiredAt(Option.of(Instant.now()));
        conferenceMetaDao.update(updatedMeta);
    }
}
