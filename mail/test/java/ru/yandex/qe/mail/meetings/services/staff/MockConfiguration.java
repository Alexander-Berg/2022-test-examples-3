package ru.yandex.qe.mail.meetings.services.staff;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.services.staff.dto.Response;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Profile("test")
@Configuration
public class MockConfiguration {
    static final int TOTAL = StaffClient.DEFAULT_LIMIT + 1;

    @Bean
    public StaffApiV3 staffApi() {
        StaffApiV3 api = mock(StaffApiV3.class);
        when(api.persons(anyString(), anyInt(), anyInt()))
                .thenAnswer(
                        input -> {
                            Integer limit = (Integer) input.getArguments()[1];
                            int page = (Integer) input.getArguments()[2] - 1;
                            List<Person> persons = Stream.iterate(limit * page, n -> n++)
                                    .limit(TOTAL / limit > page ? limit : TOTAL % limit)
                                    .map(i -> new Person("uid" + i, "login" + i, null, null, null, null, null, null, null))
                                    .collect(Collectors.toList());
                            return new Response<>(page + 1, limit, TOTAL, 2, persons);
                        });
        return api;
    }

    @Bean
    public StaffClient staffClient(StaffApiV3 staffApi) {
        return new StaffClient(staffApi);
    }
}
