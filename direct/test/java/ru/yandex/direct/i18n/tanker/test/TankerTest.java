package ru.yandex.direct.i18n.tanker.test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.i18n.dict.PluralEntry2Form;
import ru.yandex.direct.i18n.dict.SingularEntry;
import ru.yandex.direct.i18n.tanker.Downloader;
import ru.yandex.direct.i18n.tanker.KeyTranslationStatus;
import ru.yandex.direct.i18n.tanker.ProjectTranslations;
import ru.yandex.direct.i18n.tanker.TankerWithBranch;
import ru.yandex.direct.i18n.tanker.Uploader;
import ru.yandex.direct.test.utils.TestUtils;

public class TankerTest {
    private Set<Language> languages;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public LoggerContextRule loggerContextRule = new LoggerContextRule("Log4jTestConfig.xml");
    @Rule
    public EnvRule envRule = EnvRule.getEnvRule();

    public TankerTest() {
        this.languages = new HashSet<>(Arrays.asList(Language.RU, Language.EN));
    }

    @Ignore("test tanker instance is unstable")
    @Test
    public void test() throws Exception {
        TankerWithBranch tanker = envRule.getEnv().getTankerWithBranch();
        Uploader uploader = new Uploader(
                tanker,
                I18NBundle.makeSafeMethodInterpreter(),
                languages
        );

        uploader.uploadMergeBundle(TankerTestTranslations.class);

        // Переименовываем кейсет, для имитации обновлений в бандле
        tanker.updateKeyset(
                TankerTestTranslations.class.getCanonicalName(),
                tanker
                        .getKeyset(TankerTestTranslations.class.getCanonicalName())
                        .get()
                        .withName(TankerTestTranslations2.class.getCanonicalName())
        );

        // Делаем вид, что пришли переводчики и добавили переводов
        ProjectTranslations translations = tanker.getKeysetTranslations(
                TankerTestTranslations2.class.getCanonicalName(),
                languages
        );
        translations
                .getSingleKeyset()
                .getKeyTranslationsMap().get("yyy")
                .getTranslation(Language.EN)
                .ifPresent(translation -> translation.mergeDictionaryEntry(new SingularEntry("yiyiyi")));
        translations
                .getSingleKeyset()
                .getKeyTranslationsMap().get("campaignModerationFailed")
                .getTranslation(Language.EN)
                .ifPresent(translation -> translation.mergeDictionaryEntry(new PluralEntry2Form("a", "b")));
        tanker.merge(translations, languages);

        // Вливаем "новое"
        uploader.uploadMergeAll("ru.yandex.direct.i18n.tanker.test");

        // В логах сообщение об изменившейся заглушке
        List<LogEvent> log = loggerContextRule.getListAppender("List").getEvents();
        Assert.assertEquals(1, log.size());
        Assert.assertEquals(Level.WARN, log.get(0).getLevel());
        Assert.assertEquals(
                "Tanker translation differs from stub: "
                        + "keyset: ru.yandex.direct.i18n.tanker.test.TankerTestTranslations2, "
                        + "key: hello, "
                        + "tanker: SingularEntry{form='Привет, {0}'}, "
                        + "stub: SingularEntry{form='{0}, привет!'}",
                log.get(0).getMessage().getFormattedMessage()
        );

        // Сверяем результат с эталоном
        new Downloader(tanker, languages).downloadAll(
                "ru.yandex.direct.i18n.tanker.test",
                temporaryFolder.getRoot().toPath(),
                EnumSet.allOf(KeyTranslationStatus.class),
                true // allowIncompleteTranslations
        );
        TestUtils.assertEqualDirsWithDiff(
                Paths.get(TankerTest.class.getResource("dicts").toURI()),
                temporaryFolder.getRoot().toPath(),
                "utf-8"
        );
    }
}
