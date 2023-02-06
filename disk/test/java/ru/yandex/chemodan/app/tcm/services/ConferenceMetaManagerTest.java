package ru.yandex.chemodan.app.tcm.services;

import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.chemodan.app.tcm.TcmTestContextConfiguration;
import ru.yandex.chemodan.app.tcm.utils.ConferenceTestUtils;
import ru.yandex.chemodan.app.tcm.db.ConferenceMeta;
import ru.yandex.chemodan.app.tcm.exceptions.ConferenceAlreadyExist;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.misc.test.Assert;

/**
 * @author friendlyevil
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TcmTestContextConfiguration.class,
})
@ImportZkEmbeddedConfiguration
public class ConferenceMetaManagerTest extends AbstractTest {
    @Autowired
    private ConferenceMetaManager conferenceMetaManager;

    @Test
    public void testDoubleCreate() {
        ConferenceMeta conferenceMeta = ConferenceTestUtils.generateConferenceMeta();
        conferenceMetaManager.createConferenceMeta(conferenceMeta);
        conferenceMetaManager.createConferenceMeta(conferenceMeta);
    }

    @Test
    public void testCreateConferenceWithDublicateConferenceIdAfterLongTime() {
        ConferenceMeta conferenceMeta = ConferenceTestUtils.generateConferenceMeta();
        conferenceMetaManager.createConferenceMeta(conferenceMeta);
        DateTimeUtils.setCurrentMillisOffset(1000L * 60 * 60);
        Assert.assertThrows(() -> conferenceMetaManager.createConferenceMeta(conferenceMeta), ConferenceAlreadyExist.class);
    }
}
