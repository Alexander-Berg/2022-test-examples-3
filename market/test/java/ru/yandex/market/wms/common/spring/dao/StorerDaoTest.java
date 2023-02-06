package ru.yandex.market.wms.common.spring.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.StorerType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Storer;
import ru.yandex.market.wms.common.spring.dao.implementation.StorerDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class StorerDaoTest extends IntegrationTest {
    @Autowired
    private StorerDao storerDao;

    private final String storerKey = "000001234";
    private final StorerType storerType = StorerType.SUPPLIER;

    @Test
    @DatabaseSetup("/db/dao/storer/before.xml")
    @ExpectedDatabase(value = "/db/dao/storer/before.xml", assertionMode = NON_STRICT)
    public void findWhenExists() {
        Storer storer = storerDao.findStorer(storerKey, storerType).orElse(null);
        assertions.assertThat(storer).isNotNull();
        assertions.assertThat(storer.getStorerKey()).isEqualTo(storerKey);
        assertions.assertThat(storer.getType()).isEqualTo(storerType);
        assertions.assertThat(storer.getCompany()).isEqualTo("Acme");
    }
}

