package ru.yandex.market.tpl.mock.driver;

import java.util.List;

import lombok.Data;

@Data
public class Driver {

    private String id;
    private String uid;
    private PersonalData personalData;
    private List<String> employerIds;
    private boolean blackListed;

}

