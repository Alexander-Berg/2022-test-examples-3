package ru.yandex.market.antifraud.orders.tanking.headers;


import ru.yandex.market.antifraud.orders.tanking.TankAmmo;

/**
 * @author dzvyagin
 */
public interface HeaderFactory {

    default void init(){}

    String getHeaderName();

    String getHeader(TankAmmo tankAmmo);


}
