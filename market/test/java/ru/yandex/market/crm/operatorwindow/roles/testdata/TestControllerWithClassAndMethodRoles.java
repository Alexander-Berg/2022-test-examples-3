package ru.yandex.market.crm.operatorwindow.roles.testdata;

import ru.yandex.market.crm.operatorwindow.http.security.roles.OperatorRole;
import ru.yandex.market.crm.operatorwindow.http.security.roles.SupervisorRole;

@SupervisorRole
public class TestControllerWithClassAndMethodRoles {

    @OperatorRole
    public void method() {
    }
}
