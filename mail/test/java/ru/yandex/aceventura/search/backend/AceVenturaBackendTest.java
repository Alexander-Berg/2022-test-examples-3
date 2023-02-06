package ru.yandex.aceventura.search.backend;

import java.io.File;

import org.junit.Test;

import ru.yandex.ace.ventura.AceVenturaPrefix;
import ru.yandex.ace.ventura.UserType;
import ru.yandex.devtools.test.Paths;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class AceVenturaBackendTest extends TestBase {
    // CSOFF: MultipleStringLiterals

    private static final String LUCENE_CONFIG =
        Paths.getSourcePath(
            "mail/search/aceventura/aceventura_backend/files"
                + "/aceventura_search_backend.conf");

    @Test
    public void testDepartmentDp() throws Exception {

        System.setProperty("TVM_API_HOST", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("TVM_ALLOWED_SRCS", "");
        System.setProperty("SECRET", "");
        System.setProperty("SERVER_NAME", "");
        System.setProperty("JKS_PASSWORD", "");
        System.setProperty("INDEX_PATH", "");
        System.setProperty("INDEX_THREADS", "2");
        System.setProperty("MERGE_THREADS", "1");
        System.setProperty("SEARCH_THREADS", "6");
        System.setProperty("LIMIT_SEARCH_REQUESTS", "5");

        try (TestSearchBackend searchBackend =
            new TestSearchBackend(this, new File(LUCENE_CONFIG).toPath()))
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(2, UserType.CONNECT_ORGANIZATION);
            searchBackend.add(prefix,
                " \"av_email\": \"dpotapov@yandex-team.ru\",\n" +
                    "\"av_corp_position_name\": \"team leader\",\n" +
                    "\"av_corp_position_type\": \"head_department\",\n" +
                    "\"av_corp_department_level\": \"7\",\n" +
                    "\"av_corp_department_id\": \"79\",\n" +
                    "\"av_corp_departments\": \"962\\n62131\\n88996\\n1563\\n8630\\n78\\n79\",\n" +
                    "\"av_corp_messengers_nicks\": \"\",\n" +
                    "\"av_corp_dismissed\": \"false\",\n" +
                    "\"id\": \"ace_dpotapov_doc_id\"");

            String baseUri = "/search?prefix=" + prefix + "&get=dp_score&text=av_email:dpotapov*";
            QueryConstructor qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments,962 dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{\"dp_score\":\"6\"}]}"));

            qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments,962;62131;88996;1563;8630;78;79 dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{\"dp_score\":\"0\"}]}"));

            qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments,962;62131;88996;1563;8630; dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{\"dp_score\":\"2\"}]}"));

            qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments,962;62131;88996;1563;863; dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{\"dp_score\":\"4\"}]}"));

            qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments,96;546; dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{\"dp_score\":\"9\"}]}"));

            qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments, dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker("{\"hitsCount\":1,\"hitsArray\":[{\"dp_score\":\"100\"}]}"));

            searchBackend.add(prefix,
                " \"av_email\": \"dpotapov@yandex-team.ru\",\n" +
                    "\"av_corp_position_name\": \"team leader\",\n" +
                    "\"av_corp_position_type\": \"head_department\",\n" +
                    "\"av_corp_department_level\": \"7\",\n" +
                    "\"av_corp_department_id\": \"79\",\n" +
                    "\"av_corp_departments\": \"8174\\n24700\",\n" +
                    "\"av_corp_messengers_nicks\": \"\",\n" +
                    "\"av_corp_dismissed\": \"false\",\n" +
                    "\"id\": \"ace_dpotapov_doc_id_2\"");

            // test robots department
            qc = new QueryConstructor(baseUri);
            qc.append("dp", "dep_dist(av_corp_departments,962;247;88996; dp_score)");
            searchBackend.checkSearch(
                qc.toString(),
                new JsonChecker(
                    "{\"hitsCount\":2,\"hitsArray\":[{\"dp_score\":\"8\"}, {\"dp_score\":\"16\"}]}"));
        }
    }
}
