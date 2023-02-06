package ru.yandex.market.mboc.app.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class GenericScenario<STATE> {
    private final List<ScenarioStep> steps = new ArrayList<>();
    private final BiConsumer<String, STATE> defaultAssertion;
    private final Function<STATE, STATE> stateCopier;

    public GenericScenario(BiConsumer<String, STATE> defaultAssertion,
                           Function<STATE, STATE> stateCopier) {
        this.defaultAssertion = defaultAssertion;
        this.stateCopier = stateCopier;
    }

    public void execute() {
        steps.forEach(ScenarioStep::executeAndAssert);
    }

    public STATE previousValidState() {
        return steps.size() == 0 ? null : steps.get(steps.size() - 1).getValidState();
    }

    public StepWithoutAction<STATE> step() {
        return new ScenarioStepBuilder(this);
    }

    public StepWithoutAction<STATE> step(String description) {
        ScenarioStepBuilder builder = new ScenarioStepBuilder(this);
        builder.description(description);
        return builder;
    }

    public interface StepWithoutAction<STATE> {
        PartlyConfiguredStep<STATE> action(Consumer<STATE> action);
    }

    public interface PartlyConfiguredStep<STATE> {
        PartlyConfiguredStep<STATE> description(String description);

        PartlyConfiguredStep<STATE> ignoreDefaultCheck();

        default PartlyConfiguredStep<STATE> addCheck(BiConsumer<String, STATE> check) {
            return addChecks(Collections.singletonList(check));
        }

        PartlyConfiguredStep<STATE> addChecks(List<BiConsumer<String, STATE>> checks);

        ConfiguredStep<STATE> expectedState(STATE expected);
    }

    public interface ConfiguredStep<STATE> extends PartlyConfiguredStep<STATE> {
        GenericScenario<STATE> endStep();
    }

    public class ScenarioStepBuilder implements StepWithoutAction<STATE>, ConfiguredStep<STATE> {
        private GenericScenario<STATE> scenario;
        private String description;
        private Consumer<STATE> action;
        private List<BiConsumer<String, STATE>> checks = new ArrayList<>();
        private boolean ignoreDefaultCheck = false;
        private STATE expected;

        private ScenarioStepBuilder(GenericScenario<STATE> scenario) {
            this.scenario = scenario;
        }

        public PartlyConfiguredStep<STATE> action(Consumer<STATE> action) {
            this.action = action;
            return this;
        }

        @Override
        public PartlyConfiguredStep<STATE> description(String description) {
            this.description = description;
            return this;
        }

        public PartlyConfiguredStep<STATE> ignoreDefaultCheck() {
            ignoreDefaultCheck = true;
            return this;
        }

        public final PartlyConfiguredStep<STATE> addChecks(List<BiConsumer<String, STATE>> checks) {
            this.checks.addAll(checks);
            return this;
        }

        public ConfiguredStep<STATE> expectedState(STATE expected) {
            this.expected = stateCopier.apply(expected);
            return this;
        }

        public GenericScenario<STATE> endStep() {
            List<BiConsumer<String, STATE>> resultChecks = ignoreDefaultCheck ? checks
                : Stream.concat(Stream.of(defaultAssertion), checks.stream()).collect(Collectors.toList());

            steps.add(
                new ScenarioStep(
                    StringUtils.isBlank(description) ? Integer.toString(steps.size() + 1) : description,
                    previousValidState(), expected, action,
                    (description, state) -> resultChecks.forEach(a -> a.accept(description, state))
                )
            );
            return scenario;
        }
    }

    public class ScenarioStep {
        private final String description;
        private final STATE before;
        private final STATE after;
        private final Consumer<STATE> execution;
        private final BiConsumer<String, STATE> assertion;

        public ScenarioStep(String description,
                            @Nullable STATE before, STATE after,
                            Consumer<STATE> execution,
                            BiConsumer<String, STATE> assertion) {
            this.description = description;
            this.before = before == null ? null : stateCopier.apply(before);
            this.after = stateCopier.apply(after);
            this.execution = execution;
            this.assertion = assertion;
        }

        void executeAndAssert() {
            try {
                execution.accept(before);
                assertion.accept(description, after);
            } catch (Throwable e) {
                throw new AssertionError("Step '" + description + "' failed.", e);
            }
        }

        public @Nullable
        STATE getValidState() {
            return stateCopier.apply(after);
        }
    }
}
