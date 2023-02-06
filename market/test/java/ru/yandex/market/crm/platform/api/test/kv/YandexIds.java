package ru.yandex.market.crm.platform.api.test.kv;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

import ru.yandex.common.util.collections.CollectionUtils;

/**
 * @author apershukov
 */
public class YandexIds {

    private LongSet puids;
    private Set<String> yandexuids;
    private Set<String> uuids;
    private Set<String> deviceIds;

    public YandexIds() {
    }

    public YandexIds(YandexIds other) {
        this.puids = other.puids;
        this.yandexuids = other.yandexuids;
        this.uuids = other.uuids;
    }

    public YandexIds(LongSet puids, Set<String> yandexuids, Set<String> uuids) {
        this.puids = puids;
        this.yandexuids = yandexuids;
        this.uuids = uuids;
    }

    public LongSet getPuids() {
        return puids == null
                ? LongSets.EMPTY_SET
                : puids;
    }

    public void setPuids(LongSet puids) {
        this.puids = puids;
    }

    public Set<String> getYandexuids() {
        return yandexuids == null
                ? Collections.emptySet()
                : yandexuids;
    }

    public void setYandexuids(Set<String> yandexuids) {
        this.yandexuids = yandexuids;
    }

    public Set<String> getUuids() {
        return uuids == null
                ? Collections.emptySet()
                : uuids;
    }

    public void setUuids(Set<String> uuids) {
        this.uuids = uuids;
    }

    public Set<String> getDeviceIds() {
        return deviceIds == null
                ? Collections.emptySet()
                : deviceIds;
    }

    public void setDeviceIds(Set<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public YandexIds addPuid(long puid) {
        if (puids == null) {
            puids = new LongArraySet();
        }
        puids.add(puid);
        return this;
    }

    public YandexIds addYandexuid(String yandexuid) {
        if (yandexuids == null) {
            yandexuids = new HashSet<>();
        }
        yandexuids.add(yandexuid);
        return this;
    }

    public YandexIds addUuid(String uuid) {
        if (uuids == null) {
            uuids = new HashSet<>();
        }
        uuids.add(uuid);
        return this;
    }

    public YandexIds addDeviceIds(String deviceId) {
        if (deviceIds == null) {
            deviceIds = new HashSet<>();
        }
        deviceIds.add(deviceId);
        return this;
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(yandexuids)
                && CollectionUtils.isEmpty(uuids)
                && CollectionUtils.isEmpty(puids)
                && CollectionUtils.isEmpty(deviceIds);
    }
}
