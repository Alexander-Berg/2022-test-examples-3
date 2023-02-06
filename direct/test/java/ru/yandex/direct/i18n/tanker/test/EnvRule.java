package ru.yandex.direct.i18n.tanker.test;

import java.util.function.BiFunction;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import ru.yandex.direct.i18n.tanker.Tanker;
import ru.yandex.direct.utils.io.FileUtils;

public class EnvRule implements TestRule {
    private BiFunction<Statement, Description, Env> envFactory;
    private Env env;

    public EnvRule(BiFunction<Statement, Description, Env> envFactory) {
        this.envFactory = envFactory;
        this.env = null;
    }

    public Env getEnv() {
        return env;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        EnvRule rule = this;

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (Env env = envFactory.apply(base, description)) {
                    rule.env = env;
                    try {
                        base.evaluate();
                    } finally {
                        rule.env = null;
                    }
                }
            }
        };
    }

    static EnvRule getEnvRule() {
        return new EnvRule((base, description) -> new TmpBranchEnv(testTanker()));
    }

    static Tanker testTanker() {
        return Tanker.forTokenFile(
                Tanker.TEST_TANKER_URL,
                FileUtils.expandHome("~/.ssh/tanker-direct-java-token"),
                "direct-java",
                false
        );
    }
}
