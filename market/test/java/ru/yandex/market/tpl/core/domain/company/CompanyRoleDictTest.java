package ru.yandex.market.tpl.core.domain.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.core.test.AbstractDbDictTest;

class CompanyRoleDictTest extends AbstractDbDictTest {

    @Autowired
    public CompanyRoleDictTest(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, CompanyRoleEnum.class, "company_roles", "name");
    }

}
