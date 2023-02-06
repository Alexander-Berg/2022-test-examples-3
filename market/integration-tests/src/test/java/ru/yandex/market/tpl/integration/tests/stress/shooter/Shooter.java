package ru.yandex.market.tpl.integration.tests.stress.shooter;

import java.util.List;

public interface Shooter<T> {
    T shoot(List<Runnable> actions);
}
