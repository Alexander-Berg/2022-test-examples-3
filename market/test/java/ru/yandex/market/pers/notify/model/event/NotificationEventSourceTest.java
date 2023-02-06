package ru.yandex.market.pers.notify.model.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.notify.model.NotificationSubtype;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         07.03.17
 */
public class NotificationEventSourceTest {
    @Test
    public void testSerialize() throws Exception {
        NotificationEventSource source = NotificationEventSource
            .fromMbiAddress("valter@yandex-team.ru", NotificationSubtype.ADVERTISING_1).build();
        assertNotNull(source.getMbiAddress());
        ObjectMapper mapper = new ObjectMapper();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            mapper.writeValue(out, source);
            String s = new String(out.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(s.matches(".*mbiAddress.*"));

            NotificationEventSource actualSource = mapper.readValue(s, NotificationEventSource.class);
            assertEquals(source.getMbiAddress(), actualSource.getMbiAddress());

            s = s.replaceAll("mbiAddress", "address");
            actualSource = mapper.readValue(s, NotificationEventSource.class);
            assertEquals(source.getMbiAddress(), actualSource.getMbiAddress());
        }
    }
}
