package ru.yandex.market.passport.internal.idm.api.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.market.passport.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class LocalizedStringTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testShortSerialization() throws JsonProcessingException {
        LocalizedString localizedString = new LocalizedString("qwerty");
        String serialized = mapper.writer().writeValueAsString(localizedString);
        String expected = ResourceUtils.getJsonResourceAsString("idm/localized-string-short.json");
        assertEquals(expected, serialized);
    }

    @Test
    public void testFullSerialization() throws JsonProcessingException {
        LocalizedString localizedString = new LocalizedString();
        localizedString.setValue("ru", "Имя");
        localizedString.setValue("en", "Name");
        String serialized = mapper.writer().writeValueAsString(localizedString);
        String expected = ResourceUtils.getJsonResourceAsString("idm/localized-string-full.json");
        assertEquals(expected, serialized);
    }
}
