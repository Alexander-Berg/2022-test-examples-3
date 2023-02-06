package ru.yandex.direct.core.testing.info;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.dbutil.model.ClientId;

@ParametersAreNonnullByDefault
public class MobileAppInfo {
    private MobileApp mobileApp;
    private MobileContentInfo mobileContentInfo = new MobileContentInfo();

    public void setMobileApp(MobileApp mobileApp) {
        this.mobileApp = mobileApp;
    }

    public void setMobileContentInfo(MobileContentInfo mobileContentInfo) {
        this.mobileContentInfo = mobileContentInfo;
    }

    public MobileAppInfo withMobileApp(MobileApp mobileApp) {
        this.mobileApp = mobileApp;
        return this;
    }

    public MobileAppInfo withMobileContentInfo(MobileContentInfo mobileContentInfo) {
        this.mobileContentInfo = mobileContentInfo;
        return this;
    }

    public MobileAppInfo withClientInfo(ClientInfo clientInfo) {
        mobileContentInfo.withClientInfo(clientInfo);
        return this;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        mobileContentInfo.setClientInfo(clientInfo);
    }

    public ClientInfo getClientInfo() {
        return mobileContentInfo.getClientInfo();
    }

    public MobileApp getMobileApp() {
        return mobileApp;
    }

    public MobileContentInfo getMobileContentInfo() {
        return mobileContentInfo;
    }

    public Long getMobileContentId() {
        return mobileContentInfo.getMobileContentId();
    }

    public Long getMobileAppId() {
        return mobileApp.getId();
    }

    public ClientId getClientId() {
        return mobileContentInfo.getClientId();
    }
}
