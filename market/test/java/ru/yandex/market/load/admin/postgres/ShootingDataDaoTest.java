package ru.yandex.market.load.admin.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.dao.ShootingDataDao;

public class ShootingDataDaoTest extends AbstractFunctionalTest {
    @Autowired
    private ShootingDataDao dao;

    @Test
    public void findAllOk() {
        dao.findAll();
    }
}
