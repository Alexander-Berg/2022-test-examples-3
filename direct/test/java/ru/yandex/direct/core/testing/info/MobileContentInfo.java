package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class MobileContentInfo {
    private ClientInfo clientInfo = new ClientInfo();
    private MobileContent mobileContent;

    public ClientId getClientId() {
        return clientInfo.getClientId();
    }

    public Long getMobileContentId() {
        return ifNotNull(mobileContent, MobileContent::getId);
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public MobileContentInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public MobileContent getMobileContent() {
        return mobileContent;
    }

    public void setMobileContent(MobileContent mobileContent) {
        this.mobileContent = mobileContent;
    }

    public MobileContentInfo withMobileContent(MobileContent mobileContent) {
        this.mobileContent = mobileContent;
        return this;
    }

    public Integer getShard() {
        return clientInfo.getShard();
    }

    public String getStoreContentId() {
        return mobileContent.getStoreContentId();
    }
}
