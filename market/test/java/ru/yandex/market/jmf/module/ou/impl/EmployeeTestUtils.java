package ru.yandex.market.jmf.module.ou.impl;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class EmployeeTestUtils {
    private final BcpService bcpService;

    public EmployeeTestUtils(BcpService bcpService) {
        this.bcpService = bcpService;
    }

    public Employee createEmployee(Entity ou) {
        return createEmployee(Randoms.string(), ou);
    }

    public Employee createEmployee(Entity ou,
                                   Map<String, Object> additionalAttributes) {
        Map<String, Object> properties = Maps.of(
                Employee.TITLE, Randoms.string(),
                Employee.OU, ou
        );
        properties.putAll(additionalAttributes);
        return bcpService.create(Employee.FQN_DEFAULT, properties);
    }

    public Employee createEmployee(String title, Entity ou) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, title,
                Employee.OU, ou
        );
        return bcpService.create(Employee.FQN_DEFAULT, properties);
    }

    public Employee createEmployee(String title, Entity ou, Long uid) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                Employee.TITLE, title,
                Employee.OU, ou,
                Employee.UID, uid
        );
        return bcpService.create(Employee.FQN_DEFAULT, properties);
    }

}
