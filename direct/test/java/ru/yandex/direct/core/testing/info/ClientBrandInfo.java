package ru.yandex.direct.core.testing.info;

import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.client.model.ClientBrand;
import ru.yandex.direct.dbutil.model.ClientId;

@ParametersAreNonnullByDefault
public class ClientBrandInfo {
    private ClientInfo clientInfo;
    private ClientInfo brandClientInfo;
    private LocalDateTime syncTime;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public ClientBrandInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public ClientInfo getBrandClientInfo() {
        return brandClientInfo;
    }

    public void setBrandClientInfo(ClientInfo brandClientInfo) {
        this.brandClientInfo = brandClientInfo;
    }

    public ClientBrandInfo withBrandClientInfo(ClientInfo brandClientInfo) {
        this.brandClientInfo = brandClientInfo;
        return this;
    }

    public Integer getShard() {
        return clientInfo.getShard();
    }

    public ClientId getClientId() {
        return clientInfo.getClientId();
    }

    public ClientId getBrandClientId() {
        return brandClientInfo.getClientId();
    }

    public LocalDateTime getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
    }

    public ClientBrandInfo withSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
        return this;
    }

    public ClientBrand getClientBrand() {
        return new ClientBrand()
                .withClientId(getClientId().asLong())
                .withBrandClientId(getBrandClientId().asLong())
                .withLastSync(syncTime);
    }
}
