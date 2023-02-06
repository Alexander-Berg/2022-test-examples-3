package ru.yandex.autotests.direct.cmd.steps.pagesize;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.pagesize.SetPageSizeRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class PageSizeSteps extends DirectBackEndSteps {
    @Step("Устанавливаем число показываемых групп на странице для кампании {1}, значение {2}")
    public RedirectResponse setGroupsOnShowCamp(String ulogin, Long cid, String value) {
        SetPageSizeRequest request = new SetPageSizeRequest()
                .withSubCmd(CMD.SHOW_CAMP)
                .withTab("all")
                .withValue(value)
                .withCid(cid)
                .withUlogin(ulogin);
        return post(CMD.SET_PAGE_SIZE, request, RedirectResponse.class);
    }

}
