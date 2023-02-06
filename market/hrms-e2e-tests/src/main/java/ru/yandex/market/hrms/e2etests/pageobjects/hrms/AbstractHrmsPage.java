package ru.yandex.market.hrms.e2etests.pageobjects.hrms;

import com.codeborne.selenide.Selenide;

import ru.yandex.market.hrms.e2etests.pageobjects.AbstractPage;
import ru.yandex.market.hrms.e2etests.pageobjects.hrms.leftmenu.LeftMenu;

public abstract class AbstractHrmsPage extends AbstractPage {
    private final LeftMenu leftMenu = Selenide.page(LeftMenu.class);

    public LeftMenu leftMenu() {
        return this.leftMenu;
    }
}
