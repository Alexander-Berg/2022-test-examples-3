package ru.yandex.market.volva.tank.headers;


import ru.yandex.market.volva.tank.TankAmmo;

/**
 * @author dzvyagin
 */
public interface HeaderFactory {

    default void init(){}

    String getHeaderName();

    String getHeader(TankAmmo tankAmmo);


}
