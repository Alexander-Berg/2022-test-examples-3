package ru.yandex.direct.common.spring;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@ParametersAreNonnullByDefault
public class AbstractSpringTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule stringMethodRule = new SpringMethodRule();

}
