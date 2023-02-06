package ru.yandex.market.notifier.jobs.zk;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.NoteType;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.ResolutionSubtype;
import ru.yandex.market.checkout.entity.ResolutionType;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.core.MarketOXMAliases;
import ru.yandex.market.notifier.core.OXMHelper;
import ru.yandex.market.notifier.jobs.tms.RefereeImportWorkerJob;
import ru.yandex.market.notifier.jobs.zk.impl.LocalNote;
import ru.yandex.market.notifier.log.NoteMeta;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 20.02.17
 */
public class RefereeImportWorkerJobTest extends AbstractServicesTestBase {

    @Autowired
    private RefereeImportWorkerJob refereeImportTmsJob;

    @Test
    public void testNoteXmlData() {
        System.out.println("Empty LocalNote: ");
        LocalNote no = new LocalNote();
        OXMHelper oxm = new OXMHelper();
        System.out.println(oxm.toXmlStringDoNotEscapePayload(no, MarketOXMAliases.MARKETPLACE_OXM_ALIASES));

        System.out.println();
        System.out.println("LocalNote from Note: ");
        Note note = getNote();
        String localNoteXml = refereeImportTmsJob.getNoteXmlData(note, false);
        System.out.println(localNoteXml);
        LocalNote localNote = oxm.toObject(localNoteXml, LocalNote.class, MarketOXMAliases.MARKETPLACE_OXM_ALIASES);
        note.setWarningTs(null);
        // payload не парсится, но есть в equals
        note.setPayload(null);
        localNote.getNote().setWarningTs(null);
        localNote.getNote().setPayload(null);
        assertEquals(note, localNote.getNote());
    }

    @Test
    public void testRefereeRefundRequestNotification() {
        RequestContextHolder.createNewContext();
        Note note = getNote();
        NoteMeta meta = new NoteMeta(System.currentTimeMillis());
        assertEquals(1, refereeImportTmsJob.sendNotification(note, meta));
    }

    private Note getNote() {
        Note note = new Note();
        note.setAuthorRole(RefereeRole.USER);
        note.setConversationId(1L);
        note.setConvStatusAfter(ConversationStatus.ARBITRAGE);
        note.setConvStatusBefore(ConversationStatus.ARBITRAGE);
        note.setInquiryDocs(true);
        note.setId(2L);
        note.setOrderId(3L);
        note.setPayload(
                "<refund><refund-type>REFUND</refund-type>\n" +
                        "<attachment-group>2419</attachment-group>\n" +
                        "<attachment-id>2776</attachment-id>\n" +
                        "<order-id>-1</order-id>\n" +
                        "<items><item><feed-id>1</feed-id>\n" +
                        "<offer-id>1</offer-id>\n" +
                        "</item></items></refund>");
        note.setResolutionSubtype(ResolutionSubtype.BAD_PRODUCT);
        note.setResolutionType(ResolutionType.NO_ACTION);
        note.setShopId(4L);
        note.setShopOrderId("5");
        note.setType(NoteType.NOTIFY_SHOP);
        note.setWarningTs(new Date());
        note.setUserName("6");
        note.setUserEmail("valetr@yandex.ru");
        return note;
    }
}
