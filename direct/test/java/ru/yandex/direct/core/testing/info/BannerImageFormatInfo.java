package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.core.testing.data.TestBanners.regularImageFormat;

public class BannerImageFormatInfo {
    private ClientInfo clientInfo = new ClientInfo();
    private BannerImageFormat bannerImageFormat = regularImageFormat(null);

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public BannerImageFormatInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public BannerImageFormat getBannerImageFormat() {
        return bannerImageFormat;
    }

    public BannerImageFormatInfo withBannerImageFormat(BannerImageFormat bannerImageFormat) {
        this.bannerImageFormat = bannerImageFormat;
        return this;
    }

    public String getImageHash() {
        return bannerImageFormat.getImageHash();
    }

    public Long getUid() {
        return getClientInfo().getUid();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }
}
