package ru.yandex.test.search.backend;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class TestMailSearchBackend extends TestSearchBackend {
    private static final String CONFIG =
        System.getenv("ARCADIA_SOURCE_ROOT")
        + "/mail/search/mail/perseus_config/files/bp/search_backend.conf";

    static {
        System.setProperty("LUCENE_DROP_PASSWORD", "ugu");
        System.setProperty(
            "LUCENE_CONFIG_INCLUDE",
            System.getenv("ARCADIA_SOURCE_ROOT")
            + "/mail/search/mail/perseus_config/files/bp"
            + "/search_backend_thin.conf");
        System.setProperty(
            "LUCENE_FIELDS_CONFIG_DIR",
            System.getenv("ARCADIA_SOURCE_ROOT")
            + "/mail/search/mail/search_backend_mail_config/files");
        System.setProperty(
            "LUCENE_CONFIG_SSD",
            System.getenv("ARCADIA_SOURCE_ROOT")
            + "/mail/search/mail/perseus_config/files/perseus-nossd.conf");
        System.setProperty("INDEX_BASE", "/webcache/msearch");
        System.setProperty("INDEX_DIR", "/webcache/msearch");
    }

    public TestMailSearchBackend(
        final TestBase testBase,
        final boolean verbose)
        throws Exception
    {
        super(testBase, verbose, new File(CONFIG));
    }

    public TestMailSearchBackend(final TestBase testBase) throws Exception {
        super(testBase, new File(CONFIG));
    }

    public TestMailSearchBackend(
        final TestBase testBase,
        final Map<String, String> overrides) throws Exception
    {
        super(testBase, patchConfig(testBase, overrides));
    }

    private static IniConfig patchConfig(
        final TestBase testBase,
        final Map<String, String> overrides)
        throws Exception
    {
        IniConfig config = new IniConfig(new File(CONFIG));
        config = patchConfig(
            Files.createTempDirectory(testBase.testName.getMethodName()),
            config);
        for (Map.Entry<String, String> entry: overrides.entrySet()) {
            config.put(entry.getKey(), entry.getValue());
        }

        return config;
    }
}
