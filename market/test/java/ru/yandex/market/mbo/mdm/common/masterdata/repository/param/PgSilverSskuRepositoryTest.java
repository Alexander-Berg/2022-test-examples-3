package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import org.springframework.beans.factory.annotation.Autowired;

public class PgSilverSskuRepositoryTest extends SilverSskuRepositoryTest {
    @Autowired
    private SilverSskuRepository pgRepository;

    @Override
    protected SilverSskuRepository repository() {
        return pgRepository;
    }
}
