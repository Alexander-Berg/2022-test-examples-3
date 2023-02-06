package ru.yandex.travel.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author kurau (Yuri Kalinin)
 */
@Getter
@Setter
@Accessors(chain = true)
public class Summary {

    private SearchParameters information;

    private TourInformation yaTour;

    private TourInformation slTour;

    private int success;

}
