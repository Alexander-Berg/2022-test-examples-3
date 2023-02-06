package ru.yandex.direct.intapi.entity.emailvalidation.controller;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.utils.JsonUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@RunWith(Parameterized.class)
@IntApiTest
public class EmailValidationControllerTest {
    private static final String VALIDATE_URL = "/emailvalidation";

    private static List<String> emails;

    @Autowired
    private EmailValidationController controller;

    private Map<String, Boolean> mapResponse;
    private String email;
    private Boolean isValid;

    public EmailValidationControllerTest(String email, Boolean isValid) {
        this.email = email;
        this.isValid = isValid;
    }

    @Parameterized.Parameters(name = "Email = {0}, Is valid = {1}")
    public static Iterable<Object[]> params() {
        List<Object[]> emailsToExpectedResults =
                Arrays.asList(
                        new Object[]{"a@b.bar", true},
                        new Object[]{"a@yandex.xn--ru-", false},
                        new Object[]{"a@yandex.xn--ru", false},
                        new Object[]{"a@yandex.comm", false},
                        new Object[]{"a@yandex.com", true},
                        new Object[]{"a@mail.ry", false},
                        new Object[]{"a@gmail.cov", false},
                        new Object[]{"a@gmail.com", true},
                        new Object[]{"a@ukr.xn--net-hdd4af7dtd", false},
                        new Object[]{"cv3rd@yandex.xn--c1an", false},
                        new Object[]{"aadmin@xn----7sbavve0ahjeog0d1d.xn--p1ai", true},
                        new Object[]{"a@яндекс.рф", true},
                        new Object[]{"INFO@ПИЛОМАТЕРИАЛЫ.travel", true}
                );
        emails = emailsToExpectedResults.stream()
                .map(a -> (String) a[0])
                .collect(Collectors.toList());
        return emailsToExpectedResults;
    }

    @Before
    public void before() throws Exception {
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
        String stringResponse = mockMvc
                .perform(MockMvcRequestBuilders.post(VALIDATE_URL)
                        .content(JsonUtils.toJson(emails))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        mapResponse =
                JsonUtils.fromJson(stringResponse, new TypeReference<HashMap<String, Boolean>>() {
                });
        assumeThat("Вернулось правильное количество результатов", mapResponse.size(),
                is(equalTo(emails.size())));
    }

    @Test
    public void testControllerReturnsExpectedResponse() {
        String assertReason =
                String.format("Для email '%s' должен быть ответ '%s'", email, isValid);
        assertThat(assertReason, mapResponse.get(email), is(equalTo(isValid)));
    }
}
