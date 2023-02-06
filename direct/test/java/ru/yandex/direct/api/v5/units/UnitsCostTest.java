package ru.yandex.direct.api.v5.units;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import ru.yandex.direct.api.v5.ApiConstants;
import ru.yandex.direct.api.v5.ws.annotation.ApiMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.units.ApiUnitsService.UNITS_COSTS_CONF;
import static ru.yandex.direct.core.units.OperationCosts.CALL_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.MIN_DAILY_LIMIT_KEY;
import static ru.yandex.direct.core.units.OperationCosts.OBJECT_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.OBJECT_ERROR_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.REQUEST_ERROR_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.SPECIAL_PROCESSING_OBJECT_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.SPENT_GROUP_SIZE_KEY;

@RunWith(Parameterized.class)
public class UnitsCostTest {
    private static final String[] EXPECTED_COSTS =
            {CALL_COST_KEY, OBJECT_COST_KEY, OBJECT_ERROR_COST_KEY, REQUEST_ERROR_COST_KEY};

    private Config config;
    private String serviceName;
    private String operationName;

    public UnitsCostTest(String serviceName, String operationName) {
        this.serviceName = serviceName;
        this.operationName = operationName;
    }

    @Parameterized.Parameters(name = "{index}: service[{0}].operation[{1}]")
    public static Collection<Object[]> data() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(ApiConstants.class)))
                .setScanners(new MethodAnnotationsScanner()));
        Set<Method> methods = reflections.getMethodsAnnotatedWith(ApiMethod.class);

        Collection<Object[]> result = new ArrayList<>();
        for (Method method : methods) {
            ApiMethod apiMethod = method.getAnnotation(ApiMethod.class);
            result.add(new Object[]{apiMethod.service(), apiMethod.operation()});
        }
        return result;
    }

    @Before
    public void before() {
        config = ConfigFactory.load(UNITS_COSTS_CONF);
    }

    @Test
    public void costIsSet() {
        Config costs = config.getConfig("costs");
        Config serviceCosts = costs.getConfig(serviceName + "." + operationName);
        Collection<String> actualCosts =
                StreamEx.of(serviceCosts.root().keySet())
                        .remove(MIN_DAILY_LIMIT_KEY::equals)
                        .remove(SPENT_GROUP_SIZE_KEY::equals)
                        .remove(SPECIAL_PROCESSING_OBJECT_COST_KEY::equals)
                        .toList();

        assertThat(actualCosts).containsExactlyInAnyOrder(EXPECTED_COSTS);
    }

}
