package ru.yandex.autotests.innerpochta.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assume.assumeTrue;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

/**
 * @author oleshko
 */

@Aspect
public class DoTestOnlyForEnvironmentAspect {

    private String environmentInfo = urlProps().getDeviceType();

    private String browserInfo = urlProps().getDriver();

    private HashMap<String, String[]> environmentMap = new HashMap<String, String[]>() {{
        put("Phone", new String[]{"Google Nexus 5", "Apple iPhone 6 Plus"});
        put("Tablet", new String[]{"Google Nexus 10", "Apple iPad Pro"});
        put("Android", new String[]{"Google Nexus 10", "Google Nexus 5"});
        put("iOS", new String[]{"Apple iPhone 6 Plus", "Apple iPad Pro"});
        put("Not Nexus 5", new String[]{"Apple iPhone 6 Plus", "Google Nexus 10", "Apple iPad Pro"});
        put("Not IE", new String[]{"chrome", "firefox"});
        put("Not FF", new String[]{"chrome", "internet explorer"});
        put("Apple iPad Pro", new String[]{"Apple iPad Pro"});
    }};

    @After("execution(* ru.yandex.autotests.innerpochta.rules.WatchRule.starting(..))")
    public void IgnoreIfInappropriateEnvironment(JoinPoint joinPoint) throws Throwable {
        Description descr = (Description) joinPoint.getArgs()[0];
        String environmentNeeded = "";
        try {
            environmentNeeded = descr.getAnnotation(DoTestOnlyForEnvironment.class).value();
        } catch (NullPointerException ignored) {
        }
        if (environmentNeeded.equals("")) {
            return;
        }
        if (environmentInfo == null) {
            environmentInfo = "not mentioned";
        }
        if (browserInfo == null) {
            browserInfo = "not mentioned";
        }
        assumeTrue(
            String.format("Тест не выполняется для окружения «%s»", environmentInfo),
            Arrays.asList(environmentMap.get(environmentNeeded)).contains(environmentInfo) ||
                Arrays.asList(environmentMap.get(environmentNeeded)).contains(browserInfo)
        );
    }
}

