package ru.yandex.calendar.logic.contact.ml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.commune.json.jackson.bolts.BoltsModule;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;

@TestExecutionListeners
public class MlSimpleTest {
    @Test
    public void testParse() throws JsonProcessingException {
        String response = "{\n" +
                "   \"result\" : {\n" +
                "      \"ml-dev@yandex-team.ru\" : {\n" +
                "         \"is_open\" : true,\n" +
                "         \"is_internal\" : true,\n" +
                "         \"subscribers\" : [\n" +
                "            {\n" +
                "               \"email\" : \"ignition@yandex-team.ru\",\n" +
                "               \"imap\" : true,\n" +
                "               \"inbox\" : false,\n" +
                "               \"login\" : \"ignition\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"email\" : \"pierre@yandex-team.ru\",\n" +
                "               \"imap\" : true,\n" +
                "               \"inbox\" : false,\n" +
                "               \"login\" : \"pierre\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"email\" : \"shelkovin@yandex-team.ru\",\n" +
                "               \"imap\" : false,\n" +
                "               \"inbox\" : true,\n" +
                "               \"login\" : \"shelkovin\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"email\" : \"rubtsovdmv@yandex-team.ru\",\n" +
                "               \"imap\" : true,\n" +
                "               \"inbox\" : false,\n" +
                "               \"login\" : \"rubtsovdmv\"\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";
        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new BoltsModule());
        final MailLists mailLists = objectMapper.readValue(response, MailLists.class);
        assertThat(mailLists).isNotNull();
        final MapF<Email, MailList> mapF = mailLists.getLists();
        assertThat(mapF.size()).isEqualTo(1);
        final SetF<Email> correctEmails = Cf.hashSet(
                new Email("rubtsovdmv@yandex-team.ru"),
                new Email("shelkovin@yandex-team.ru"),
                new Email("pierre@yandex-team.ru"),
                new Email("ignition@yandex-team.ru")
        );
        for (Email email : mapF.keySet()) {
            assertThat(email).isEqualTo(new Email("ml-dev@yandex-team.ru"));
            final MailList mailList = mapF.getOrThrow(email);
            assertThat(mailList.getSubscribers().size()).isEqualTo(4);
            for (Subscriber subscriber : mailList.getSubscribers()) {
                assertThat(subscriber.getEmail()).isIn(correctEmails);
            }
        }
    }
}
