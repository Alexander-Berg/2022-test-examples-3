package ru.yandex.market.wms.db.validate;

import java.util.Map;

public interface VaultClient {
    Map<String, String> getSecretEntries(String secretId);
}
