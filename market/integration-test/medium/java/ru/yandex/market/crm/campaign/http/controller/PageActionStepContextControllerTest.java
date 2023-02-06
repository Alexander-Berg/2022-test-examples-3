package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.domain.notifications.Note;
import ru.yandex.market.crm.campaign.domain.notifications.NoteType;
import ru.yandex.market.crm.campaign.domain.page.PageContext;
import ru.yandex.market.crm.campaign.services.notifications.NotificationDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class PageActionStepContextControllerTest extends AbstractControllerMediumTest {

    @Inject
    private NotificationDAO notificationDAO;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Test
    public void testShowPermanentNote() throws Exception {
        String author = "apershukov";

        Note note = new Note()
                .setType(NoteType.INFO)
                .setSubject("Note subject")
                .setAuthor(author);

        note = notificationDAO.save(note);

        PageContext context = requestContext();

        List<Note> notes = context.getNotes();
        Assertions.assertEquals(1, notes.size());
        Assertions.assertEquals(note.getId(), notes.get(0).getId());
        Assertions.assertEquals(note.getSubject(), notes.get(0).getSubject());
        Assertions.assertEquals(author, notes.get(0).getAuthor());
    }

    @Test
    public void testShowOnlyNotesReachedTheirTime() throws Exception {
        Note note1 = new Note()
                .setType(NoteType.INFO)
                .setSubject("Subject 1")
                .setFrom(LocalDateTime.now().minusDays(1));
        note1 = notificationDAO.save(note1);

        Note note2 = new Note()
                .setType(NoteType.INFO)
                .setSubject("Subject 2")
                .setFrom(LocalDateTime.now().plusDays(1));
        notificationDAO.save(note2);

        PageContext context = requestContext();

        List<Note> notes = context.getNotes();
        Assertions.assertEquals(1, notes.size());
        Assertions.assertEquals(note1.getId(), notes.get(0).getId());
    }

    @Test
    public void testHideNotesPassedTheirTime() throws Exception {
        Note note1 = new Note()
                .setType(NoteType.INFO)
                .setSubject("Subject 1")
                .setFrom(LocalDateTime.now().minusDays(1))
                .setTo(LocalDateTime.now().plusMinutes(1));
        note1 = notificationDAO.save(note1);

        Note note2 = new Note()
                .setType(NoteType.INFO)
                .setSubject("Subject 2")
                .setTo(LocalDateTime.now().minusMinutes(1));
        notificationDAO.save(note2);

        PageContext context = requestContext();

        List<Note> notes = context.getNotes();
        Assertions.assertEquals(1, notes.size());
        Assertions.assertEquals(note1.getId(), notes.get(0).getId());
    }

    private PageContext requestContext() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/page/context")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                PageContext.class,
                result.getResponse().getContentAsByteArray()
        );
    }
}
