package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;

public class PppKeyValueStoreDaoTest extends BaseDBTest {

    @Autowired
    PppKeyValueStoreDao pppKeyValueStoreDao;

    @Test
    public void testLongs() {
        String key = "some_key";
        long value = 12345L;
        // get non-existing
        Assertions.assertThat(pppKeyValueStoreDao.getLong(key)).isNull();

        // insert
        pppKeyValueStoreDao.putLong(key, value);
        Assertions.assertThat(pppKeyValueStoreDao.getLong(key)).isEqualTo(value);

        // update
        long newValue = value + 1;
        pppKeyValueStoreDao.putLong(key, newValue);
        Assertions.assertThat(pppKeyValueStoreDao.getLong(key)).isEqualTo(newValue);

        // set to null
        pppKeyValueStoreDao.putLong(key, null);
        Assertions.assertThat(pppKeyValueStoreDao.getLong(key)).isEqualTo(null);

        // set value again
        pppKeyValueStoreDao.putLong(key, newValue);
        Assertions.assertThat(pppKeyValueStoreDao.getLong(key)).isEqualTo(newValue);
    }
}
