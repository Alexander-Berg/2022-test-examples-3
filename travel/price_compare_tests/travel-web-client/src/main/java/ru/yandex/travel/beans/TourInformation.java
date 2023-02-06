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
public class TourInformation {

    private String url;

    private String request;

    private String operator;

    private String screenUrl;

    private int price;

}
