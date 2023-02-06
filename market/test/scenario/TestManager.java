package ru.yandex.market.test.scenario;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 23.05.16
 */
public class TestManager {
    private static final Logger log = LoggerFactory.getLogger(TestManager.class);

    @Autowired
    private ApplicationContext context;

    private Set<String> setNamesSet;
    private Set<String> scenarioNamesSet;
    private boolean stopOnFail = true;

    {
        String setNames = System.getProperty("test.set.names");
        if (setNames != null) {
            setNamesSet = new HashSet<>(Arrays.asList(setNames.split(",")));
        }

        String scenarioNames = System.getProperty("test.scenario.names");
        if (scenarioNames != null) {
            scenarioNamesSet = new HashSet<>(Arrays.asList(scenarioNames.split(",")));
        }

        String stopOnFailString = System.getProperty("test.stop.on.fail");
        if (stopOnFailString != null) {
            stopOnFail = Boolean.parseBoolean(stopOnFailString);
        }
    }

    @PostConstruct
    public void runTests() throws Exception {
        log.debug("Running tests with params:");
        log.debug("\tsets: " + (setNamesSet == null ? "all" : setNamesSet));
        log.debug("\tscenarios: " + (scenarioNamesSet == null ? "all" : scenarioNamesSet));
        log.debug("\n");

        Map<String, TestScenarioSet> beans = context.getBeansOfType(TestScenarioSet.class);

        beans.values().stream()
                .filter(set -> setNamesSet == null || setNamesSet.contains(set.getName()))
                .forEach(set -> {
                    if (scenarioNamesSet != null) {
                        set.makeScenario(scenarioNamesSet, stopOnFail);
                    } else {
                        set.makeAllScenarios(stopOnFail);
                    }
                });
    }
}
