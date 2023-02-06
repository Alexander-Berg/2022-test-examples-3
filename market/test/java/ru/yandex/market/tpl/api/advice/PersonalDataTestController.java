package ru.yandex.market.tpl.api.advice;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Profile("tests")
@RestController
public class PersonalDataTestController {

    @GetMapping("/test/pd/enrich")
    public HasPersonalDataTestImpl getPersonalDataDto(@RequestBody HasPersonalDataTestImpl dataToReturn) {
        return dataToReturn;
    }

    @GetMapping("/test/pd/enrich/indented")
    public HasPersonalDataIndented getPersonalIndented(@RequestBody HasPersonalDataIndented dataToReturn) {
        return dataToReturn;
    }

    @GetMapping("/test/pd/enrich/nopd")
    public HasNoPersonalDataImpl getNonPersonal(@RequestBody HasNoPersonalDataImpl dataToReturn) {
        return dataToReturn;
    }

    @GetMapping("/test/pd/enrich/abstract")
    public AbstractClassForPdReturn getNonPersonal(@RequestBody HasPersonalDataTestImpl dataToReturn) {
        return dataToReturn;
    }
}
