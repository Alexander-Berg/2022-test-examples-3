package ru.yandex.msearch;

import java.io.File;

public class PrimaryKeyYesCacheTest extends PrimaryKeyTestBase {
    @Override
    public Config config(final File root, final String suffix)
        throws Exception
    {
        return SearchBackendTestBase.config(
            root,
            suffix + "\nprimary_key_part_cache_documents = true\n");
    }
}
