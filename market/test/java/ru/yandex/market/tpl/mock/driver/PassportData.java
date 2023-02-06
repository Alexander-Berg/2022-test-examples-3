package ru.yandex.market.tpl.mock.driver;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PassportData {

    private String firstName;
    private String lastName;
    private String patronymic;
    private String serialNumber;
    private String citizenship;
    private LocalDate birthDate;
    private LocalDate issueDate;
    private String issuer;

}
