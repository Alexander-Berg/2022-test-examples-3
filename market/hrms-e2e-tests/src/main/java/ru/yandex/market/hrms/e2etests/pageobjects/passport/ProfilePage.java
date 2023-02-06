package ru.yandex.market.hrms.e2etests.pageobjects.passport;

import ru.yandex.market.hrms.e2etests.pageobjects.AbstractPage;

public class ProfilePage extends AbstractPage {

    @Override
    protected void checkPageElements() {
    }

    @Override
    protected String urlCheckRegexp() {
        return "/profile";
    }
}
