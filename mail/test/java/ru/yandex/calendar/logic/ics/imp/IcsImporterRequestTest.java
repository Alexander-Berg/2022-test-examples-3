package ru.yandex.calendar.logic.ics.imp;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;

public class IcsImporterRequestTest extends IcsImporterFromFileTestBase {

    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;

    @Test
    @Ignore
    public void externalYes() throws Exception {
        importIcsByUidWithStatCheck("request/request2.ics", TestManager.UID2, new long[] {1, 1});
        assertEquals(1, eventDao.findEventCount());
    }

    @Test
    @Ignore
    public void externalNo() throws Exception {
        importIcsByUidWithStatCheck("request/request2.ics", TestManager.UID2, new long[] {1, 1});
        assertEquals(0, eventDao.findEventCount());
    }


    @Test
    public void guestListImport() throws Exception {
        IcsImportStats stats = importIcsByUidWithStatCheck("request/request3.ics", TestManager.UID2, new long[] {1, 1});
        long eventId = stats.getProcessedEventIds().single();
        Assert.assertEquals(1, jdbcTemplate.queryForInt("SELECT COUNT(id) FROM event WHERE id = ?", eventId));
        Participants participants = eventInvitationManager.getParticipantsByEventId(eventId);
        SetF<ParticipantId> ids = Cf.toSet(participants.getAllAttendees().map(ParticipantInfo.getIdF()));
        ids = ids.plus(participants.getOrganizer().getId());
        Assert.A.hasSize(5, ids);

        // stepancheg: contact table is no longer needed to be filled,
        // because we do not use org_contact_id anymore.
        //AssertF.A.hasSize(1, contactDao.findContactsByUid(TestManager.UID2));
    }
}
