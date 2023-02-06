package ru.yandex.travel.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author kurau (Yuri Kalinin)
 */
@Getter
@Setter
@Accessors(chain = true)
public class SearchParameters {

    private String fromCity;

    private String toCountry;

    private String resort;

    private String hotel;

    private String fromDate;

    private String toDate;

    private int minNights;

    private int maxNights;

    private int adults;

    private List<String> childs;

}
