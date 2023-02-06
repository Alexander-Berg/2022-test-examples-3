package ru.yandex.chemodan.app.psbilling.core;

import lombok.AllArgsConstructor;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;

@AllArgsConstructor
public class PsBillingTextsFactory {
    public final static String TEST_PROJECT = "disk-ps-billing";
    public final static String TEST_KEYSET = "for_tests";
    public final static String TEST_KEY = "test_key";
    public final static String TEST_KEY2 = "test_key2";
    private TankerKeyDao tankerKeyDao;
    private TextsManager textsManager;

    public TankerKeyEntity create() {
        return create(TEST_KEY);
    }

    public TankerKeyEntity create2() {
        return create(TEST_KEY2);
    }

    public TankerKeyEntity create(String tankerKey) {
        Option<TankerKeyEntity> entityO =
                tankerKeyDao.findAllKeys().filter(x -> x.getKey().equals(tankerKey)).firstO();
        if (entityO.isPresent()) {
            return entityO.get();
        }

        TankerKeyEntity entity = tankerKeyDao
                .create(TankerKeyDao.InsertData.builder().project(TEST_PROJECT).keySet(TEST_KEYSET).key(tankerKey)
                        .build());
        loadTranslations();
        return entity;
    }

    public void loadTranslations() {
        textsManager.updateTranslations();
    }

}
