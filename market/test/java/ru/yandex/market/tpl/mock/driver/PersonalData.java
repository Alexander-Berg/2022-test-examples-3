package ru.yandex.market.tpl.mock.driver;

import lombok.Data;

@Data
public class PersonalData {

    private String name;
    private String phone;
    private String email;
    private String telegram;
    private PassportData passportData;

}
