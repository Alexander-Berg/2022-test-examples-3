package ru.yandex.market.mbo.audit.clickhouse;

import java.util.HashMap;
import java.util.Map;

public class ZookeeperStateCheckerMock extends ZookeeperStateChecker {
    private Map<String, ClickhouseConnectionState> hostToState;

    public ZookeeperStateCheckerMock(String zkHosts, String zkPath) {
        super(zkHosts, zkPath);
        hostToState = new HashMap<>();
    }

    @Override
    public Map<String, ClickhouseConnectionState> getHostStates() {
        return hostToState;
    }

    @Override
    public void setState(String host, ClickhouseConnectionState state) {
        hostToState.put(host, state);
    }
}
