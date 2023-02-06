package ru.yandex.direct.teststeps;


import org.slf4j.Logger;

import ru.yandex.direct.common.jetty.JettyConfig;
import ru.yandex.direct.common.jetty.JettyLauncher;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.jcommander.ParserWithHelp;
import ru.yandex.direct.logging.LoggingInitializer;
import ru.yandex.direct.logging.LoggingInitializerParams;

public class TestStepsApp {
    private static final Logger logger = LoggingInitializer.getLogger(TestStepsApp.class);

    private TestStepsApp() {
    }

    public static void main(String[] args) throws Exception {
        LoggingInitializerParams loggingParams = new LoggingInitializerParams();
        ParserWithHelp.parse(TestStepsApp.class.getCanonicalName(), args, loggingParams);
        LoggingInitializer.initialize(loggingParams, "direct.test-steps");

        DirectConfig conf = DirectConfigFactory.getConfig();
        JettyConfig jettyConfig = new JettyConfig(conf.getBranch("jetty"));

        logger.info("start jetty server with config: {}", jettyConfig);

        JettyLauncher.server(jettyConfig)
                .withDefaultWebApp(TestStepsApp.class.getClassLoader(), "/")
                .run();
    }
}
