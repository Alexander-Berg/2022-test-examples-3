package ru.yandex.market.antifraud.orders.tanking.generators;


import ru.yandex.market.antifraud.orders.tanking.TankAmmo;

/**
 * @author dzvyagin
 */
public interface RequestGenerator {

    TankAmmo generate();

}
