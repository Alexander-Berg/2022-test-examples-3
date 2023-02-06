package ru.yandex.autotests.direct.cmd.steps.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.qatools.allure.annotations.Step;

public class StopResumeBannerSteps extends DirectBackEndSteps {

    @Step("Остановка баннера (cmd_stopBanner, bid = {1}, retpath = {2}")
    public RedirectResponse postStopBanner(Long cid, Long bid, String retPath) {
        BasicDirectRequest request = new BasicDirectRequest() {
            @SerializeKey("cid")
            private Long _cid = cid;
            @SerializeKey("bid")
            private Long _bid = bid;
        };
        request.setRetPath(retPath);
        return post(CMD.STOP_BANNER, request, RedirectResponse.class);
    }
}
