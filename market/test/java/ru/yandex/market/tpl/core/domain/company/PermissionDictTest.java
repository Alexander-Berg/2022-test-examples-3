package ru.yandex.market.tpl.core.domain.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.api.model.company.PermissionEnum;
import ru.yandex.market.tpl.core.test.AbstractDbDictTest;

public class PermissionDictTest extends AbstractDbDictTest {

    @Autowired
    public PermissionDictTest(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, PermissionEnum.class, "permissions", "name");
    }

}
