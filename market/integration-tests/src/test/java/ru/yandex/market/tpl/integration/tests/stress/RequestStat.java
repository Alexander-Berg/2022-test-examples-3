package ru.yandex.market.tpl.integration.tests.stress;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResultItem;

@Getter
@ToString
@RequiredArgsConstructor
public class RequestStat extends ShootingResultItem {
    private final String endpoint;
    @Setter
    private int httpStatus;
}
