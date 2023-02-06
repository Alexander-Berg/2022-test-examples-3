package ru.yandex.calendar;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import ru.yandex.calendar.test.Developer;

@RunWith(Cucumber.class)
@CucumberOptions(strict = true, features = "classpath:/")
public class IntegrationTest {
    @BeforeClass
    public static void beforeFeature() {
        Developer.beforeTest();
    }

    @AfterClass
    public static void afterFeature() {
        Developer.afterTest();
    }
}
