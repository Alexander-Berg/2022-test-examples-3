package ru.yandex.market.crm.operatorwindow.roles.testdata;

import ru.yandex.market.crm.operatorwindow.http.security.roles.OperatorRole;

public class TestControllerOnlyMethodRoles {

    @OperatorRole
    public void method() {
    }
}
