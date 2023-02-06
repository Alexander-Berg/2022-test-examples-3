package ru.yandex.msearch;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;

import ru.yandex.devtools.test.Paths;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.util.filesystem.DeletingFileVisitor;

public final class SearchBackendTestBase {
    private SearchBackendTestBase() {
    }

    public static final int SHARDS = 10;

    public static void removeDirectory(final File directory) throws Exception {
        Files.walkFileTree(directory.toPath(), DeletingFileVisitor.INSTANCE);
    }

    public static Config config(final File root) throws Exception {
        return config(root, "");
    }

    public static Config config(final File root, final String suffix)
    	throws Exception {
        return config(root, suffix, SHARDS);
    }

    public static Config config(final File root, final String suffix, final int shards)
        throws Exception
    {
        File indexDir = new File(root, "index");
        return new Config(
            new IniConfig(
                new StringReader(
                    config(
                        indexDir.getCanonicalPath(),
                        suffix,
                        shards))));
    }

    public static String config(
        final String indexPath,
        final String suffix,
        final int shards)
    {
        return
            "http.port = 0\n"
            + "http.timeout = 10000\n"
            + "http.connections = 1000\n"
            + "http.workers.min = 20\n"
            + "search.port = 0\n"
            + "search.timeout = 10000\n"
            + "search.connections = 1000\n"
            + "search.workers.min = 20\n"
            + "indexer.port = 0\n"
            + "indexer.timeout = 10000\n"
            + "indexer.connections = 1000\n"
            + "indexer.workers.min = 20\n"
            + "drop-password= ugu\n"
            + "index_threads = 20\n"
            + "shards = " + shards
            + "\nuse_journal = 1\n"
            + "index_path = " + indexPath + '\n'
            + "xurls_regex_file = "
            + Paths.getSourcePath(
                "mail/search/mail/search_backend_mail_config/files"
                + "/search_backend_xurls_patterns")
            + "\nfull_log.level.min = all\n"
            + "yandex_codec.terms_writer_block_size = 8192\n"
            + "yandex_codec.group_field = __prefix\n"
            + "yandex_codec.fields_writer_buffer_size = 6144\n"
            + "field.keyword.tokenizer = keyword\n"
            + "field.keyword.store = true\n"
            + "field.keyword.prefixed = true\n"
            + "field.keyword.analyze = true\n"
            + "field.property.index = false\n"
            + "field.property.store = true\n"
            + "field.attribute.tokenizer = whitespace\n"
            + "field.attribute.filters = lowercase\n"
            + "field.attribute.prefixed = false\n"
            + "field.attribute.attribute = true\n"
            + "field.attribute.analyze = true\n"
            + "field.attribute.store = true\n"
            + "field.text.tokenizer = letter\n"
            + "field.text.filters = lowercase|yo|lemmer\n"
            + "field.text.prefixed = true\n"
            + "field.text.analyze = true\n"
            + "field.boolean.tokenizer = boolean\n"
            + "field.boolean.prefixed = true\n"
            + "field.boolean.analyze = true\n"
            + "field.boolean.attribute = true\n"
            + "field.boolean.store = true\n"
            + suffix
            + "\n[field.received_date]\n"
            + "tokenizer = keyword\n"
            + "filters = padding:10\n"
            + "prefixed = true\n"
            + "store = true\n"
            + "analyze = true\n"
            + "attribute = true\n"
            + "type = integer\n";
    }
}

