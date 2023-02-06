package ru.yandex.market.tpl.integration.tests.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.user.UserDto;
import ru.yandex.market.tpl.api.model.user.UserListDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.CourierProperties;

@Component
@RequiredArgsConstructor
public class CourierPool {
    private static final Integer MAX_ACTIVE_COURIERS = 7;

    private final CourierProperties courierProperties;
    private final ManualApiClient manualApiClient;
    private Semaphore semaphore;

    @PostConstruct
    void init() {
        semaphore = new Semaphore(Math.min(MAX_ACTIVE_COURIERS, courierProperties.getList().size()));
    }

    @SneakyThrows
    public Courier resolveFreeCourier() {
        List<Courier> couriers = courierProperties.getList();
        if (couriers.isEmpty()) {
            throw new RuntimeException("There is no courier properties");
        }
        semaphore.acquire();
        for (Courier courier : couriers) {
            UserListDto userListDto = manualApiClient.findUserByEmail(courier.getEmail());
            if (userListDto.getUsers().isEmpty()) {
                return courier;
            } else if (isUserCreateMoreThan30MinutesAgo(userListDto.getUsers().iterator().next())) {
                manualApiClient.deleteCourier(null, courier.getEmail());
                return courier;
            }
        }
        throw new RuntimeException("There are no free couriers");
    }

    public void releaseCourier() {
        semaphore.release();
    }

    private boolean isUserCreateMoreThan30MinutesAgo(UserDto user) {
        return user.getCreatedAt().isBefore(Instant.now().minusSeconds(60 * 30));
    }
}
