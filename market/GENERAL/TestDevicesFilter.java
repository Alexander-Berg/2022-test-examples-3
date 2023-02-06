package ru.yandex.market.crm.campaign.services.messages.push;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import ru.yandex.market.crm.campaign.domain.sending.TestPushDevice;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;

import static ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType.ANDROID_PUSH_TOKEN;
import static ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType.APPLE_IDFA;
import static ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType.APPMETRICA_DEVICE_ID;
import static ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType.GOOGLE_AID;
import static ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType.IOS_PUSH_TOKEN;

/**
 * Фильтр по тестовым устройствам, для исключения не выбраных устройств
 * и устройств которым не предназначена рассылка
 */
public class TestDevicesFilter implements Predicate<TestPushDevice> {

    private static final Map<MobilePlatform, Set<DeviceIdType>> ID_TYPES = Map.of(
            MobilePlatform.ANDROID, Set.of(ANDROID_PUSH_TOKEN, GOOGLE_AID, APPMETRICA_DEVICE_ID),
            MobilePlatform.iOS, Set.of(IOS_PUSH_TOKEN, APPLE_IDFA, APPMETRICA_DEVICE_ID)
    );

    private final Set<DeviceIdType> enabledDeviceTypes = new HashSet<>();

    public TestDevicesFilter(Set<MobilePlatform> platforms) {
        for (var platform : platforms) {
            var idTypes = ID_TYPES.get(platform);
            if (idTypes == null) {
                throw new IllegalArgumentException("Unsupported platform " + platform);
            }
            enabledDeviceTypes.addAll(idTypes);
        }
    }

    @Override
    public boolean test(TestPushDevice device) {
        return device.isSelected() && enabledDeviceTypes.contains(device.getIdType());
    }
}
