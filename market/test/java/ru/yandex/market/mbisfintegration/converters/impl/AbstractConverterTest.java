package ru.yandex.market.mbisfintegration.converters.impl;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbisfintegration.entity.Entity;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.importer.ImportConfiguration;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 11.02.2022
 */
public class AbstractConverterTest {

    @Test
    void cloneTest(){
        AbstractConverter converter = new AbstractConverter() {
            @Override
            public Entity convert(Map<String, Object> dto, Entity oldEntity, ImportConfiguration importConfiguration) {
                return null;
            }

        };

        Account account = new Account();
        account.setShopIDC(123.0);
        Account newAccount = converter.clone(account);
        Assertions.assertNotSame(account, newAccount);
        Assertions.assertEquals(account.getShopIDC(), newAccount.getShopIDC());
    }

    //TODO add tests for getAs... methods optionally
}
