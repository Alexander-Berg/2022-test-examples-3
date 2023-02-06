package ru.yandex.market.tpl.api.advice;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HasNoPersonalDataImpl {

    private List<HasPersonalFieldTestImpl> personaTestFields;
}
