package ru.yandex.direct.i18n.tanker.test;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.direct.i18n.tanker.KeyTranslationStatus;
import ru.yandex.direct.i18n.tanker.ProjectTranslations;
import ru.yandex.direct.i18n.tanker.migration.Migration;
import ru.yandex.direct.i18n.tanker.migration.MigrationSource;

import static org.junit.Assert.assertEquals;

public class MigrationTest {
    @Test
    public void testStraight() throws IOException {
        ProjectTranslations ours = fromJson(get("ours-straight.tjson"));
        new Migration(new MigrationSource(fromJson(get("theirs.tjson"))))
                .migrate(ours, KeyTranslationStatus.TRANSLATED);

        assertEquals(toJson(fromJson(get("ours-migrated-straight.tjson"))), toJson(ours));
    }

    @Test
    public void testSuffix() throws IOException {
        ProjectTranslations ours = fromJson(get("ours-suffix.tjson"));
        new Migration(new MigrationSource(fromJson(get("theirs.tjson"))))
                .migrate(ours, KeyTranslationStatus.TRANSLATED);

        assertEquals(toJson(fromJson(get("ours-migrated-suffix.tjson"))), toJson(ours));
    }

    @Test
    public void testSuffix2() throws IOException {
        ProjectTranslations ours = fromJson(get("ours-suffix2.tjson"));
        new Migration(new MigrationSource(fromJson(get("theirs.tjson"))))
                .migrate(ours, KeyTranslationStatus.TRANSLATED);

        assertEquals(toJson(fromJson(get("ours-migrated-suffix2.tjson"))), toJson(ours));
    }

    @Test
    public void testWordReplace() throws IOException {
        ProjectTranslations ours = fromJson(get("ours-word-replace.tjson"));
        new Migration(new MigrationSource(fromJson(get("theirs-word-replace.tjson"))))
                .migrate(ours, KeyTranslationStatus.TRANSLATED);

        assertEquals(toJson(fromJson(get("ours-migrated-word-replace.tjson"))), toJson(ours));
    }

    @Test
    public void testLanguageMismatch() throws IOException {
        ProjectTranslations ours = fromJson(get("ours-straight.tjson"));
        new Migration(new MigrationSource(fromJson(get("theirs-lang-mismatch.tjson"))))
                .migrate(ours, KeyTranslationStatus.TRANSLATED);

        assertEquals(toJson(fromJson(get("ours-straight.tjson"))), toJson(ours));
    }

    @Test
    public void testPlural() throws IOException {
        ProjectTranslations ours = fromJson(get("ours-plural.tjson"));
        new Migration(new MigrationSource(fromJson(get("theirs-plural.tjson"))))
                .migrate(ours, KeyTranslationStatus.TRANSLATED);
        assertEquals(toJson(fromJson(get("ours-migrated-plural.tjson"))), toJson(ours));
    }

    private static URL get(String resource) {
        return MigrationTest.class.getResource(resource);
    }

    private static String toJson(ProjectTranslations translations) throws JsonProcessingException {
        return new ObjectMapper().writerFor(ProjectTranslations.class)
                .withDefaultPrettyPrinter()
                .writeValueAsString(translations);
    }

    private static ProjectTranslations fromJson(URL resource) throws IOException {
        return new ObjectMapper().readerFor(ProjectTranslations.class).readValue(resource);
    }
}
