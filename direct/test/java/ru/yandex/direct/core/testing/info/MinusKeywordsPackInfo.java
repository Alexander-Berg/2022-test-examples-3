package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.dbutil.model.ClientId;

public class MinusKeywordsPackInfo {
    private ClientInfo clientInfo = new ClientInfo();
    private MinusKeywordsPack minusKeywordsPack;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public MinusKeywordsPackInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public MinusKeywordsPack getMinusKeywordsPack() {
        return minusKeywordsPack;
    }

    public MinusKeywordsPackInfo withMinusKeywordsPack(MinusKeywordsPack minusKeywordsPack) {
        this.minusKeywordsPack = minusKeywordsPack;
        return this;
    }

    public Long getMinusKeywordPackId() {
        return minusKeywordsPack.getId();
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
