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
public class SletatSearch {

    private String name;

    private String price;
}
