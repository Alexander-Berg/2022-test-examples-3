package ru.yandex.chemodan.app.psbilling.core.texts;

import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTextsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.inside.tanker.TankerClient;

public class TextsManagerTest extends AbstractPsBillingCoreTest {

    @Autowired
    private TextsManager textsManager;
    @Autowired
    private TankerKeyDao tankerKeyDao;
    @Autowired
    private TankerClient tankerClient;

    @Test
    public void testCaching() throws InterruptedException {
        TankerKeyEntity tankerKey = tankerKeyDao.create(
                TankerKeyDao.InsertData.builder().project(PsBillingTextsFactory.TEST_PROJECT)
                        .keySet(PsBillingTextsFactory.TEST_KEYSET)
                        .key(PsBillingTextsFactory.TEST_KEY)
                        .build());
        textsManager.updateTranslations();

        TextsManager subj = new TextsManager(tankerClient, tankerKeyDao, Duration.millis(100));
        subj.start();
        subj.awaitInitialization();

        TankerTranslation translation = subj.findTranslation(tankerKey.getId());
        Assert.assertEquals(4, translation.getAllLanguageTexts().size());

        TankerKeyEntity tankerKey2 = tankerKeyDao.create(
                TankerKeyDao.InsertData.builder().project(PsBillingTextsFactory.TEST_PROJECT)
                        .keySet(PsBillingTextsFactory.TEST_KEYSET)
                        .key(PsBillingTextsFactory.TEST_KEY2)
                        .build());
        textsManager.updateTranslations();

        Thread.sleep(1000);
        translation = subj.findTranslation(tankerKey2.getId());
        Assert.assertEquals(4, translation.getAllLanguageTexts().size());
    }

    @Test
    public void testFindTranslations() {
        TankerKeyEntity tankerKey = tankerKeyDao.create(
                TankerKeyDao.InsertData.builder().project(PsBillingTextsFactory.TEST_PROJECT)
                        .keySet(PsBillingTextsFactory.TEST_KEYSET)
                        .key(PsBillingTextsFactory.TEST_KEY)
                        .build());

        TankerTranslation translations = textsManager.findTranslation(tankerKey.getId());
        Assert.assertEquals(0, translations.getAllLanguageTexts().size());
    }

    @Test
    public void testUpdateTranslations() {
        TankerKeyEntity tankerKey = tankerKeyDao.create(
                TankerKeyDao.InsertData.builder().project(PsBillingTextsFactory.TEST_PROJECT)
                        .keySet(PsBillingTextsFactory.TEST_KEYSET)
                        .key(PsBillingTextsFactory.TEST_KEY)
                        .build());

        textsManager.updateTranslations();
        TankerTranslation translations = textsManager.findTranslation(tankerKey.getId());
        Assert.assertEquals(4, translations.getAllLanguageTexts().size());
    }

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }
}
