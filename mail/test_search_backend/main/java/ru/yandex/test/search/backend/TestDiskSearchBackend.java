package ru.yandex.test.search.backend;

import java.io.File;

import ru.yandex.test.util.TestBase;

public class TestDiskSearchBackend extends TestSearchBackend {
    private static final String CONFIG =
        System.getenv("ARCADIA_SOURCE_ROOT")
        + "/mail/search/disk/search_backend_disk_config/files"
        + "/search_backend.conf";

    static {
        System.setProperty(
            "LUCENE_DISK_CONFIG_INCLUDE",
            "search_backend_thin.conf");
        System.setProperty(
            "LUCENE_DISK_CONFIG_SSD",
            "bacchus-nossd.conf");
    }

    public TestDiskSearchBackend(
        final TestBase testBase,
        final boolean verbose)
        throws Exception
    {
        super(testBase, verbose, new File(CONFIG));
    }

    public TestDiskSearchBackend(final TestBase testBase) throws Exception {
        super(testBase, new File(CONFIG));
    }
}
