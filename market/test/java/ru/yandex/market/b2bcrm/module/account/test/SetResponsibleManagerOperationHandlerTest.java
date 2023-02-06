package ru.yandex.market.b2bcrm.module.account.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.account.Shop;
import ru.yandex.market.b2bcrm.module.config.B2bAccountTests;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ticket.Employee;

import static org.assertj.core.api.Assertions.assertThat;

@B2bAccountTests
public class SetResponsibleManagerOperationHandlerTest {

    @Inject
    private BcpService bcpService;

    @Test
    @DisplayName("Ответственный сотрудник задаётся при создании")
    public void responsibleManagerIsSetOnCreate() {
        Employee employee = getEmployee();
        Shop shop = bcpService.create(Shop.FQN, Map.of(
                Shop.TITLE, "Test Shop",
                Shop.SHOP_ID, "111111",
                Shop.YA_MANAGER_STAFF_LOGIN, "wa5teed",
                Shop.CAMPAIGN_ID, "12345"
        ));
        assertThat(shop.getResponsibleManager().getStaffLogin()).isEqualTo(employee.getStaffLogin());
    }

    @Test
    @DisplayName("Ответственный сотрудник может быть изменён")
    public void responsibleManagerCouldBeChanged() {
        Employee employee = getEmployee();
        Shop shop = bcpService.create(Shop.FQN, Map.of(
                Shop.TITLE, "Test Shop",
                Shop.SHOP_ID, "111111",
                Shop.YA_MANAGER_STAFF_LOGIN, "wa5teed",
                Shop.RESPONSIBLE_MANAGER, employee,
                Shop.CAMPAIGN_ID, "12345"
        ));
        Employee newEmployee = bcpService.create(Employee.FQN_DEFAULT, Map.of(
                Employee.TITLE, "Title",
                Employee.STAFF_LOGIN, "change",
                Employee.OU, getOu()
        ));
        bcpService.edit(shop, Shop.RESPONSIBLE_MANAGER, newEmployee);
        assertThat(shop.getResponsibleManager().getStaffLogin()).isEqualTo(newEmployee.getStaffLogin());
    }

    private Employee getEmployee() {
        return bcpService.create(Employee.FQN_DEFAULT, Map.of(
                Employee.TITLE, "Title",
                Employee.STAFF_LOGIN, "wa5teed",
                Employee.OU, getOu()
        ));
    }

    private Ou getOu() {
        return bcpService.create(Ou.FQN_DEFAULT, Map.of(Employee.TITLE, "Default"));
    }
}
