package ru.yandex.chemodan.app.notes.dao.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;

import static ru.yandex.chemodan.app.notes.dao.test.ActivateNotesEmbeddedPg.NOTES_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author vpronto
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ActiveProfiles({NOTES_EMBEDDED_PG, EMBEDDED_PG})
public @interface ActivateNotesEmbeddedPg {
    String NOTES_EMBEDDED_PG = "notes-embedded-pg";
}
