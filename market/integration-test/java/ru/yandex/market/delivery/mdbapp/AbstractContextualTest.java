package ru.yandex.market.delivery.mdbapp;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.market.delivery.mdbapp.configuration.ClockConfig;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        MdbApplication.class,
        ClockConfig.class,
        LatchTaskListenerConfig.class,
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class AbstractContextualTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
}
