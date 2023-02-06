package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.banners.SaveMediaBannerRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.File;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 15.06.15
 */
public class SaveMediaBannerSteps extends DirectBackEndSteps {

    @Step("Сохраняем медийно-контекстный баннер")
    public DirectResponse saveMediaBanner(SaveMediaBannerRequestBean parameters) {
        return execute(getRequestBuilder().post(CMD.SAVE_MEDIA_BANNER, parameters));
    }

    public DirectResponse saveDefaultMediaBanner(String file, String campaignId, String login) {
        PropertyLoader<SaveMediaBannerRequestBean> saveMediaBannerLoader = new PropertyLoader<>(SaveMediaBannerRequestBean.class);
        SaveMediaBannerRequestBean saveMediaBannerRequestBean = saveMediaBannerLoader.getHttpBean("DefaultSaveMediaBannerRequestBean");
        saveMediaBannerRequestBean.setFilePicture(getFilePath(file));
        saveMediaBannerRequestBean.setCid(String.valueOf(campaignId));
        saveMediaBannerRequestBean.setUlogin(login);
        return saveMediaBanner(saveMediaBannerRequestBean);
    }
}
