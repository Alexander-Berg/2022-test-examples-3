package ru.yandex.market.hrms.core.service.staff;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.hrms.core.config.api.StaffApiConfig;
import ru.yandex.market.hrms.core.service.staff.client.StaffPersonsClient;
import ru.yandex.market.hrms.core.service.staff.model.Person;

@Disabled
@TestPropertySource(properties = {
        "staff.api.url=https://staff-api.yandex-team.ru",
        "staff.gap.api.url=https://staff.yandex-team.ru",
        "market.hrms.tvm.self_id=2025822",
        "market.hrms.tvm.self_secret=${TVM_SECRET}",
        "staff.api.tvm_id=2001974",
})
@SpringJUnitConfig(StaffLoadRealIntegrationTest.Config.class)
@Slf4j
public class StaffLoadRealIntegrationTest {

    @Autowired
    private StaffPersonsClient staffPersonsApi;

    @Test
    void shouldLoadRealData() {
        List<Person> people = staffPersonsApi.loadEmployeesFromStaff(137406, 1000, false);

        MatcherAssert.assertThat(people, Matchers.not(Matchers.empty()));

        log.info("People: {}", people);
    }

    @Configuration
    @Import({StaffApiConfig.class})
    public static class Config {

    }


}
