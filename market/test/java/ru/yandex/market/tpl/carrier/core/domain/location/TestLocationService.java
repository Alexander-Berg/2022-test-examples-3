package ru.yandex.market.tpl.carrier.core.domain.location;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.LocationData;

@RequiredArgsConstructor
@TestComponent
public class TestLocationService {
    private final UserLocationCommandService userLocationCommandService;

    public UserLocation saveLocation(User user, LocationData locationData) {
        return userLocationCommandService.create(UserLocationCommand.CreateFromDriver.builder()
                .userId(user.getId())
                .longitude(locationData.getLongitude())
                .latitude(locationData.getLatitude())
                .deviceId(locationData.getDeviceId())
                .userShiftId(locationData.getUserShiftId())
                .build());
    }
}
