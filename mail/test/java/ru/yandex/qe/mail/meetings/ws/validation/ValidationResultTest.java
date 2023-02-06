package ru.yandex.qe.mail.meetings.ws.validation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sergey Galyamichev
 */
public class ValidationResultTest {
    @Test
    public void merge() {
        ValidationResult calendarUrl = ValidationResult.error("calendarUrl");
        ValidationResult ttl = ValidationResult.error("ttl");
        ValidationResult merged = ValidationResult.merge(calendarUrl, ttl);
        assertEquals(ValidationResult.Status.ERROR, merged.getStatus());
        assertNotNull(merged.getErrors());
        assertEquals(2, merged.getErrors().size() );
    }
}
