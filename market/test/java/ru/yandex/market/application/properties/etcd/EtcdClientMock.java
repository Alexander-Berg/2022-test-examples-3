package ru.yandex.market.application.properties.etcd;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class EtcdClientMock implements EtcdClient {

    private Map<String, String> fullKeyToValue = new HashMap<>();

    public void addKeyValue(String etcdPath, String key, String value) {
        etcdPath = etcdPath.endsWith("/") ? etcdPath : etcdPath + "/";
        this.fullKeyToValue.put(etcdPath + key, value);
    }

    @Override
    public Map<String, String> getProperties(String etcdKey) {
        String key = etcdKey.endsWith("/") ? etcdKey : etcdKey + "/";
        return fullKeyToValue.entrySet().stream()
            .filter(kv -> kv.getKey().startsWith(etcdKey))
            .collect(Collectors.toMap(kv -> kv.getKey().replace(key, ""), kv -> kv.getValue()));
    }
}
