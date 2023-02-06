package ru.yandex.market.test.scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import ru.yandex.common.util.collections.Pair;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public class TestScenarioSet {
    private static final Logger log = LoggerFactory.getLogger(TestScenarioSet.class);

    private String name;
    private String description;

    private List<TestScenario> scenarios;

    private static void logStat(Stat stat) {
        log.debug("Test scenarios stat:\n");

        log.debug("Execution times:");
        for (Map.Entry<TestScenario, Long> scenarioTime : stat.executionTime.entrySet()) {
            log.debug("\t" + scenarioTime.getValue() + " for " + scenarioTime.getKey());
        }

        log.debug("Success: " + stat.success.size());
        for (TestScenario scenario : stat.success) {
            log.debug("\t" + scenario);
        }

        log.debug("Fail: " + stat.fail.size());
        for (Pair<TestScenarioStep, TestScenario> failedScenario : stat.fail) {
            log.debug("\t" + String.format("Scenario %s failed on step %s",
                    failedScenario.getSecond(), failedScenario.getFirst()));
        }
    }

    public boolean makeScenario(Set<String> scenarioNames, boolean stopOnFail) {
        List<TestScenario> filtered = scenarios.stream()
                .filter(s -> scenarioNames.contains(s.getName()))
                .collect(Collectors.toList());
        if (filtered.size() > 0) {
            return makeScenarios(filtered, stopOnFail);
        } else {
            log.debug("There's no scenarios with names " + scenarioNames + " in set " + this);
            return true;
        }
    }

    public boolean makeAllScenarios(boolean stopOnFail) {
        return makeScenarios(scenarios, stopOnFail);
    }

    private boolean makeScenarios(List<TestScenario> scenarios, boolean stopOnFail) {
        log.debug("Making test scenario set " + this + "\n");

        Stat stat = new Stat();

        long start = System.currentTimeMillis();
        for (TestScenario scenario : scenarios) {
            long startCurrent = System.currentTimeMillis();
            TestScenarioStep problemStep = scenario.make();
            long time = System.currentTimeMillis() - startCurrent;
            stat.executionTime.put(scenario, time);
            log.debug("Finished scenario in " + time + " ms: " + scenario);
            log.debug("====================================\n");

            if (problemStep == null) {
                stat.success.add(scenario);
            } else {
                stat.fail.add(new Pair<>(problemStep, scenario));
                if (stopOnFail) {
                    break;
                }
            }
        }

        log.debug("Finished test scenarios in " + (System.currentTimeMillis() - start) + " ms in set " + this + "\n");
        logStat(stat);
        log.debug("");
        log.debug("Finish test scenario set " + this);
        log.debug("========================================================================\n");

        return stat.fail.size() == 0;
    }

    @Required
    public void setScenarios(List<TestScenario> scenarios) {
        this.scenarios = scenarios;
    }

    public String getName() {
        return name;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestScenarioSet that = (TestScenarioSet) o;

        return name != null
                ? name.equals(that.name)
                : that.name == null
                && (description != null ? description.equals(that.description) : that.description == null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (name != null && description != null) {
            return "set{" + "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        } else {
            return "set{name='" + name + "'}";
        }
    }

    private static class Stat {
        List<TestScenario> success = new ArrayList<>();
        List<Pair<TestScenarioStep, TestScenario>> fail = new ArrayList<>();

        Map<TestScenario, Long> executionTime = new HashMap<>();
    }
}
