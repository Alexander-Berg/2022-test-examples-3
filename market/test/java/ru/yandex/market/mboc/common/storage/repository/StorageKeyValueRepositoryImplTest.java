package ru.yandex.market.mboc.common.storage.repository;

import java.util.Comparator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.mbo.storage.StorageKeyValue;
import ru.yandex.market.mbo.storage.StorageKeyValueRepositoryImpl;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author prediger
 */
public class StorageKeyValueRepositoryImplTest extends BaseDbTestClass {

    public static final Comparator<StorageKeyValue> KEY_VALUE_COMPARATOR = (o1, o2) ->
        equalsIgnoringIdAndStatusTimestamp(o1, o2) ? 0 : 1;

    @Autowired
    private StorageKeyValueRepositoryImpl repository;
    private StorageKeyValue sample;

    public static boolean equalsIgnoringIdAndStatusTimestamp(StorageKeyValue storageKeyValue1,
                                                             StorageKeyValue storageKeyValue2) {
        return EqualsBuilder.reflectionEquals(storageKeyValue1, storageKeyValue2);
    }

    @Before
    public void setUp() throws Exception {
        sample = YamlTestUtil
            .readFromResources("storage/sample-key-value.yml", StorageKeyValue.class);
        repository.insertOrUpdate(sample);
    }

    @Test
    public void testUniqueness() {
        Assertions.assertThatThrownBy(() -> repository.insert(sample))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testInsertAndSelect() {
        StorageKeyValue storageKeyValue = new StorageKeyValue()
            .setKey("test_key")
            .setValue("test_value");
        repository.insert(storageKeyValue);

        Assertions.assertThat(repository.findById("test_key"))
            .usingComparator(KEY_VALUE_COMPARATOR)
            .isEqualTo(storageKeyValue);
    }

    @Test
    public void testUpdateAndSelect() {
        sample.setValue("new_value");
        repository.update(sample);

        Assertions.assertThat(repository.findById(sample.getKey()))
            .usingComparator(KEY_VALUE_COMPARATOR)
            .isEqualTo(sample);
    }

    @Test
    public void testSelectNotExistingKeyValue() {
        Assertions.assertThat(repository.findByIdOrNull("not_existing_key")).isNull();
    }
}
