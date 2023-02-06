package ru.yandex.travel;

import org.aeonbits.owner.Config;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SletatPriceComparisonConfig extends Config {

    @Key("parameters.countForHotels")
    @DefaultValue("20")
    int countForHotels();

    @Key("parameters.countForDestinations")
    @DefaultValue("20")
    int countForDestinations();
}
