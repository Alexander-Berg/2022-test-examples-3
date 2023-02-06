package ru.yandex.msearch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.packed.PackedInts;

import ru.yandex.devtools.test.Paths;
import ru.yandex.msearch.util.JavaAllocator;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.string.StringUtils;

public class MergeIndexTest extends TestBase {
    private static final String PREFIX = "__prefix";
    private static final int TEST_DOC_COUNT = 100;
    private static final JavaAllocator ALLOCATOR =
        JavaAllocator.get("MergeIndexTest");

    @Test
    public void testGroupByPrefixSingleSegmentAfterFlush() throws Exception {
        Path path = Files.createTempDirectory(testName.getMethodName());
        IniConfig config =
            new IniConfig(
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail")));
        config.put("yandex_codec.group_field", PREFIX);
        try (TestSearchBackend lucene = new TestSearchBackend(
                path,
                false,
                TestSearchBackend.patchConfig(path, false, config)))
        {
            final int docCount = TEST_DOC_COUNT;
            LongPrefix prefixes[] = new LongPrefix[2];
            prefixes[0] = new LongPrefix(0);
            prefixes[1] = new LongPrefix(10);
            for (int i = 0; i < docCount; i++) {
                final LongPrefix prefix = prefixes[i & 0x1];
                System.err.println("Prefix: " + prefix);
                lucene.add(prefix,
                    "\"fact_uid\": " + prefix
                    + ", \"fact_mid\": " + i
                    + ", \"url\": \"" + prefix + '_' + i + '\"');
            }
            lucene.flush();

            try (
                FSDirectory dir =
                    FSDirectory.open(
                        new File(lucene.lucene().defaultDatabase().config().indexPath(), "0"));
                IndexInput in = dir.openInput("_0.fdx");
                IndexReader reader =
                    IndexReader.open(
                        dir,
                        true,
                        lucene.lucene().index().codecProvider());)
            {
                // skip 1 int before CODEC_MAGIC
                in.readInt();
                PackedInts.Reader indexReader =
                    PackedInts.getReader(in, ALLOCATOR);
                //check all docs with prefix0 are layed out before prefix10 docs
                long prefix0MaxPos = 0;
                long prefix10MinPos = -1;
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document doc = reader.document(i);
                    long prefix = Long.parseLong(doc.get("__prefix"));
                    long pos = indexReader.get(i);
                    if (prefix == 0) {
                        if (prefix0MaxPos < pos) {
                            prefix0MaxPos = pos;
                        }
                    } else {
                        if (prefix10MinPos > pos || prefix10MinPos == -1) {
                            prefix10MinPos = pos;
                        }
                    }
                    System.err.println("Doc[" + i + "].pos[" + pos + "]: "
                        + docToString(doc));
                }
                YandexAssert.assertLess(prefix10MinPos, prefix0MaxPos);
            }
        }
    }

    @Test
    public void testMultiGroupSingleSegmentAfterFlush() throws Exception {
        Path path = Files.createTempDirectory(testName.getMethodName());
        IniConfig config =
            new IniConfig(
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail")));
        config.put("yandex_codec.group_fields", "__prefix, fact_uid");
        try (TestSearchBackend lucene = new TestSearchBackend(
                path,
                false,
                TestSearchBackend.patchConfig(path, false, config)))
        {
            final int docCount = TEST_DOC_COUNT;
            LongPrefix singlePrefix = new LongPrefix(0);
            for (int i = 0; i < docCount; i++) {
                System.err.println("Prefix: " + singlePrefix);
                lucene.add(singlePrefix,
                    "\"fact_uid\": " + (i & 0x1)
                    + ", \"fact_mid\": " + i
                    + ", \"url\": \"" + singlePrefix + '_' + i + '\"');
            }
            lucene.flush();

            try (
                FSDirectory dir =
                    FSDirectory.open(
                        new File(lucene.lucene().defaultDatabase().config().indexPath(), "0"));

                IndexInput in = dir.openInput("_0.fdx");
                IndexReader reader =
                    IndexReader.open(
                        dir,
                        true,
                        lucene.lucene().index().codecProvider());)
            {
                // skip 1 int before CODEC_MAGIC
                in.readInt();
                PackedInts.Reader indexReader =
                    PackedInts.getReader(in, ALLOCATOR);
                //check all docs with fact_uid:0 are layed out
                //before fact_uid:10 docs
                long uid0MaxPos = 0;
                long uid1MinPos = -1;
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document doc = reader.document(i);
                    long factUid = Long.parseLong(doc.get("fact_uid"));
                    long pos = indexReader.get(i);
                    if (factUid == 0) {
                        if (uid0MaxPos < pos) {
                            uid0MaxPos = pos;
                        }
                    } else {
                        if (uid1MinPos > pos || uid1MinPos == -1) {
                            uid1MinPos = pos;
                        }
                    }
                    System.err.println("Doc[" + i + "].pos[" + pos + "]: "
                        + docToString(doc));
                }
                YandexAssert.assertLess(uid1MinPos, uid0MaxPos);
            }
        }
    }

    @Test
    public void testMultiGroupMultiSegment() throws Exception {
        Path path = Files.createTempDirectory(testName.getMethodName());
        IniConfig config =
            new IniConfig(
                new File(
                    Paths.getSourcePath(
                        "mail/search/mail/search_backend_mail_config/files"
                        + "/search_backend_test.conf_mail")));
        config.put("yandex_codec.group_fields", "__prefix, fact_uid, abook_suid");
        try (TestSearchBackend lucene = new TestSearchBackend(
                path,
                false,
                TestSearchBackend.patchConfig(path, false, config)))
        {
            final int docCount = TEST_DOC_COUNT;
            LongPrefix singlePrefix = new LongPrefix(0);
            //first segment
            for (int i = 0; i < docCount >> 1; i++) {
                System.err.println("Prefix: " + singlePrefix);
                lucene.add(singlePrefix,
                    "\"fact_uid\": " + (i & 0x1)
                    + ", \"fact_mid\": " + i
                    + ", \"url\": \"" + singlePrefix + '_' + i + '\"');
            }
            lucene.flush();

            //second segment
            for (int i = docCount >> 1; i < docCount; i++) {
                System.err.println("Prefix: " + singlePrefix);
                lucene.add(singlePrefix,
                    "\"fact_uid\": " + (i & 0x1)
                    + ", \"fact_mid\": " + i
                    + ", \"url\": \"" + singlePrefix + '_' + i + '\"');
            }
            lucene.flush();
            lucene.optimize(1);
            //will prune old segments
            lucene.flush();

            try (
                FSDirectory dir =
                    FSDirectory.open(
                        new File(lucene.lucene().defaultDatabase().config().indexPath(), "0"));
                IndexInput in = dir.openInput("_2.fdx");
                IndexReader reader =
                    IndexReader.open(
                        dir,
                        true,
                        lucene.lucene().index().codecProvider());)
            {
                // skip 1 int before CODEC_MAGIC
                in.readInt();
                PackedInts.Reader indexReader =
                    PackedInts.getReader(in, ALLOCATOR);
                //check all docs with fact_uid:0 are layed out
                //before fact_uid:10 docs
                long uid0MaxPos = 0;
                long uid1MinPos = -1;
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document doc = reader.document(i);
                    long factUid = Long.parseLong(doc.get("fact_uid"));
                    long pos = indexReader.get(i);
                    if (factUid == 0) {
                        if (uid0MaxPos < pos) {
                            uid0MaxPos = pos;
                        }
                    } else {
                        if (uid1MinPos > pos || uid1MinPos == -1) {
                            uid1MinPos = pos;
                        }
                    }
                    System.err.println("Doc[" + i + "].pos[" + pos + "]: "
                        + docToString(doc));
                }
                YandexAssert.assertLess(uid1MinPos, uid0MaxPos);
            }
        }
    }

    public void assertLess(final DocWithId d1, final DocWithId d2) {
        if (d1.pos >= d2.pos) {
            throw new AssertionError("doc positions is nont in order: d1.pos="
                + d1.pos + " >= d2.pos=" + d2.pos
                + ": prefix1=" + d1.doc.get("__prefix")
                + ", prefix2=" + d2.doc.get("__prefix")
                + ", d1.id=" + d1.docId
                + ", d2.id=" + d2.docId);
        }
    }

    private static class DocWithId {
        public int docId;
        public long pos;
        public Document doc;
        public DocWithId(final int docId, final Document doc, final long pos) {
            this.docId = docId;
            this.doc = doc;
            this.pos = pos;
        }
    }

    private String docToString(final Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (Fieldable f : doc.getFields()) {
            sb.append(f.name());
            sb.append(':');
            sb.append(f.stringValue());
            sb.append(';');
        }
        sb.append('}');
        return new String(sb);
    }
}

