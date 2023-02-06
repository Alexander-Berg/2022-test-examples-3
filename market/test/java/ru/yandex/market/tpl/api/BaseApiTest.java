package ru.yandex.market.tpl.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaClient;
import ru.yandex.market.tpl.core.external.avatarnica.model.AvatarnicaUploadResponse;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.external.cms.CmsTemplatorClient;
import ru.yandex.market.tpl.core.external.cms.MboCmsApiClient;
import ru.yandex.market.tpl.core.external.lifepos.LifePosFacade;

@ApiTest
public abstract class BaseApiTest {
    @MockBean
    protected AvatarnicaClient avatarnicaClient;

    @MockBean
    protected LifePosFacade lifePosFacade;

    @MockBean
    protected CmsTemplatorClient cmsTemplatorClient;

    @MockBean
    protected MboCmsApiClient mboCmsApiClient;

    @MockBean
    protected LockerApi lockerApi;

    @Autowired
    protected TestUserHelper userHelper;

    protected AvatarnicaUploadResponse createAvatarnicaUploadResponse() {
        AvatarnicaUploadResponse response = new AvatarnicaUploadResponse();
        response.setGroupId(3L);
        response.setImagename("imagename-1");
        return response;
    }
}
