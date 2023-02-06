package ru.yandex.direct.ytwrapper.chooser;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.YtUtils;

/**
 * Для запуска на дев-сервере или разработческом ноутбуке:
 * <ul>
 * <li>Выполнить всё по инструкции https://github.yandex-team.ru/yt/docker.git</li>
 * <li>Добавить переменную окружения <code>{@literal YT_PROXY=<host>:<port>}</code></li>
 * </ul>
 * <p>
 * Для запуска на CI достаточно подключить рецепт YT local
 * https://wiki.yandex-team.ru/exprmntr/recipes/#receptdljalocalyt
 */
@ParametersAreNonnullByDefault
public class JunitLocalYt extends ExternalResource {
    private final YPath rootPath;
    private Yt yt;
    private YtConfiguration ytConfiguration;
    private String testClassName;
    private String testMethodName;
    private YPath testPath;

    public JunitLocalYt() {
        this(YPath.cypressRoot()
                .child("home")
                .child("direct")
                .child("tmp")
                .child(System.getProperty("user.name")));
    }

    public JunitLocalYt(YPath rootPath) {
        this.rootPath = rootPath;
    }

    @Nonnull
    public Yt getYt() {
        return Objects.requireNonNull(yt);
    }

    public YtConfiguration getYtConfiguration() {
        return Objects.requireNonNull(ytConfiguration);
    }

    /**
     * Уникальный автоудаляемый путь для теста:
     * //{home}/{testClassName}__{testMethodName}
     */
    @Nonnull
    public YPath getTestPath() {
        return Objects.requireNonNull(testPath);
    }

    @Nonnull
    public YtClusterConfig getYtClusterConfig() {
        return new TestYtClusterConfig();
    }


    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                JunitLocalYt.this.testClassName = description.getTestClass().getSimpleName();
                JunitLocalYt.this.testMethodName = description.getMethodName();
                JunitLocalYt.super.apply(base, description).evaluate();
            }
        };
    }

    @Override
    protected void before() {
        String javaBinary = System.getProperty("java.home") + "/bin/java";
        String ytProxy = System.getenv("YT_PROXY");
        TestUtils.assumeThat(s -> s.assertThat(ytProxy)
                .describedAs("Environment variable YT_PROXY must be set")
                .isNotEmpty());
        ytConfiguration = YtUtils.getDefaultConfigurationBuilder(ytProxy, "")
                .withJavaBinary(javaBinary)
                .withSimpleCommandsRetries(2)
                .withHeavyCommandsRetries(2)
                .build();
        yt = YtUtils.http(ytConfiguration);

        testPath = rootPath.child(testClassName + "__" + testMethodName);
        // Если получаешь в этом месте 404 от YT, проверь, что в YT_PROXY указан порт api-сервера, а не веб-интерфейса.
        yt.cypress().remove(Optional.empty(), false, testPath, true, true);
        yt.cypress().create(testPath, CypressNodeType.MAP, true);
    }

    @Override
    protected void after() {
        yt.cypress().remove(Optional.empty(), false, testPath, true, true);
    }

    private class TestYtClusterConfig implements YtClusterConfig {

        @Override
        public String getProxy() {
            return getYtConfiguration().getApiHost();
        }

        @Override
        public String getToken() {
            return getYtConfiguration().getToken(); // is empty string
        }

        @Override
        public String getYqlToken() {
            return "";
        }

        @Override
        public String getHome() {
            return getTestPath().toString();
        }

        @Override
        public String getUser() {
            return System.getProperty("user.name");
        }

        @Override
        public YtCluster getCluster() {
            return YtCluster.YT_LOCAL;
        }
    }
}
