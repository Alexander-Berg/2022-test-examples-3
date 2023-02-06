package ru.yandex.market.gutgin.tms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

@Ignore
public class MoveCacheToYt {

    static final String DB_URL = "jdbc:postgresql://man-xeurx099myf9ts5u.db.yandex.net:6432,sas-asqjlknmyp34589d.db" +
            ".yandex.net:6432,vla-prdz66ai6ebfwzm1.db.yandex" +
            ".net:6432/market_partner_content_prod?&targetServerType=slave&ssl=true&prepareThreshold=0" +
            "&preparedStatementCacheQueries=0";
    static final String USER = "market_partner_content_prod";
    static final String PASS = "https://yav.yandex-team.ru/secret/sec-01dbjhv1dg54pptmgnsrtn0nnk/explore/versions";
    static final long BATCH = 5000000L;


    private static final String QUERY = "select gc_clean_web_image_validation.id          as validation_id,\n" +
            "       case\n" +
            "           when gc_clean_web_image_validation.need_async_response = true\n" +
            "               then gc_clean_web_image_validation.async_response_data ->> 'totalVerdict'\n" +
            "           else gc_clean_web_image_validation.response_data ->> 'totalVerdict'\n" +
            "           end                                   as verdicts,\n" +
            "       gc_clean_web_image_validation.finish_date as date,\n" +
            "       dcp_partner_picture.gc_sku_ticket_id      as gc_sku_ticket_id,\n" +
            "       dcp_partner_picture.idx_avatar_url        as idx_avatar_url,\n" +
            "       dcp_partner_picture.mbo_picture ->> 'url' as mbo_url,\n" +
            "       case\n" +
            "           when dppcs.status is not null then\n" +
            "               dppcs.status\n" +
            "           else dcp_partner_picture.is_cw_validation_ok\n" +
            "           end                                   as is_ok, \n" +
            "gc_clean_web_image_validation.request_mode as request_mode \n" +
            "from partner_content.gc_clean_web_image_validation\n" +
            "         join partner_content.dcp_partner_picture on dcp_partner_picture.id = " +
            "gc_clean_web_image_validation.mbo_picture_id\n" +
            "         left join partner_content.dcp_partner_picture_cw_status dppcs on dcp_partner_picture.id = dppcs" +
            ".picture_id\n" +
            "where gc_clean_web_image_validation.request_mode in\n" +
            "      ('moderation=shoes', 'moderation=fashion', 'moderation=accessories_any',\n" +
            "       'moderation=accessories_jewelry')\n" +
            "  and (dppcs.request_mode in ('moderation=shoes', 'moderation=fashion', 'moderation=accessories_any',\n" +
            "                              'moderation=accessories_jewelry') or dppcs.request_mode is null)\n" +
            " and gc_clean_web_image_validation.id > ?::bigint and gc_clean_web_image_validation.id <= ?::bigint" +
            ";";

    @Ignore
    @Test
    public void test() throws SQLException {

        String ytHttpProxy = "hahn.yt.yandex.net";
        String ytToken = "//https://oauth.yt.yandex.net/";
        Yt yt = YtUtils.http(ytHttpProxy, ytToken);
        String tablePath = "//home/market/users/peretyatko/cw_fashion_export2";
        YPath yPath = YPath.simple(tablePath);

        Connection prod = DriverManager.getConnection(DB_URL, USER, PASS);

        long startId = getMinId(prod) - 1L;
        long batches = 0L;

        if (yt.cypress().exists(yPath)) {
            yt.cypress().remove(yPath);
        }
        creatYtTable(yt, yPath);

        while (true) {
            PreparedStatement stmt = prod.prepareStatement(QUERY);
            stmt.setLong(1, startId);
            startId += BATCH;
            stmt.setLong(2, startId);
            ResultSet rs = stmt.executeQuery();
            List<YTreeMapNode> toYtList = new ArrayList<>();
            while (rs.next()) {
                long validation_id = rs.getLong("validation_id");
                String verdictString = rs.getString("verdicts");
                if (verdictString == null) {
                    continue;
                }
                List<String> verdicts = Arrays.stream(rs.getString("verdicts").split(","))
                        .map(s -> s.replaceAll("\"", "")
                                .replaceAll("\\[", "")
                                .replaceAll("\\]", "")
                                .replaceAll(" ", "")
                        )
                        .collect(Collectors.toList());
                Timestamp date = rs.getTimestamp("date");
                long gc_sku_ticket_id = rs.getLong("gc_sku_ticket_id");
                String idx_avatar_url = rs.getString("idx_avatar_url");
                String mbo_url = rs.getString("mbo_url");
                boolean is_ok = rs.getBoolean("is_ok");
                String request_mode = rs.getString("request_mode").replaceAll("moderation=", "");
                toYtList.add(toYtRow(validation_id, verdicts, date, gc_sku_ticket_id, idx_avatar_url, mbo_url, is_ok, request_mode));
            }
            batches++;

            System.out.println("Received " + toYtList.size() + " rows from db. Batch " + batches);

            if (!toYtList.isEmpty()) {
                System.out.println("Write " + toYtList.size() + " rows to yt");
                yt.tables().write(
                        yPath.append(true),
                        YTableEntryTypes.YSON,
                        Cf.wrap(toYtList)
                );
            }

            if (startId > getMaxId(prod)) {
                System.out.println("Filter is more than max id");
                break;
            }
        }


        yt.operations().merge(new MergeSpec(List.of(yPath), yPath).combineChunks(true));


    }

    private long getMinId(Connection prod) throws SQLException {
        ResultSet resultSet = prod.createStatement().executeQuery("select min(id) from\n" +
                "partner_content.gc_clean_web_image_validation;");
        if (resultSet.next()) {
            return resultSet.getLong(1);
        }
        throw new IllegalStateException("No id from db");
    }

    private long getMaxId(Connection prod) throws SQLException {
        ResultSet resultSet = prod.createStatement().executeQuery("select max(id) from\n" +
                "partner_content.gc_clean_web_image_validation;");
        if (resultSet.next()) {
            return resultSet.getLong(1);
        }
        throw new IllegalStateException("No id from db");
    }

    private YTreeMapNode toYtRow(long validation_id, List<String> verdicts, Timestamp date, long gc_sku_ticket_id,
                                 String idx_avatar_url, String mbo_url, boolean is_ok, String request_mode) {
        YTreeBuilder builder = YTree.mapBuilder();

        builder.key("validation_id").value(validation_id)
                .key("verdicts").value(verdicts)
                .key("create_date").value(date.toInstant().toString())
                .key("gc_sku_ticket_id").value(gc_sku_ticket_id)
                .key("idx_avatar_url").value(idx_avatar_url)
                .key("mbo_url").value(mbo_url)
                .key("is_ok").value(is_ok)
                .key("request_mode").value(request_mode)
        ;
        return builder.buildMap();
    }

    private void creatYtTable(Yt yt, YPath tablePath) {
        YTreeBuilder tableSchema = YTree.builder()
                .beginAttributes()
                .endAttributes();

        tableSchema.beginList();

        tableSchema.beginMap()
                .key("name").value("validation_id")
                .key("type").value("int64")
//                .key("sort_order").value("ascending")
                .endMap();

        tableSchema.beginMap()
                .key("name").value("verdicts")
                .key("type_v3").value(
                        new HashMap<>() {{
                            put("type_name", "list");
                            put("item", "string");
                        }}
                )
                .endMap();

        tableSchema.beginMap()
                .key("name").value("create_date")
                .key("type").value("string")
                .endMap();

        tableSchema.beginMap()
                .key("name").value("gc_sku_ticket_id")
                .key("type").value("int64")
                .endMap();

        tableSchema.beginMap()
                .key("name").value("idx_avatar_url")
                .key("type").value("string")
                .endMap();

        tableSchema.beginMap()
                .key("name").value("mbo_url")
                .key("type").value("string")
                .endMap();

        tableSchema.beginMap()
                .key("name").value("is_ok")
                .key("type").value("boolean")
                .endMap();

        tableSchema.beginMap()
                .key("name").value("request_mode")
                .key("type").value("string")
                .endMap();


        tableSchema.endList();

        MapF<String, YTreeNode> tableAttributes = Cf.<String, YTreeNode>hashMap()
                .plus1("schema", tableSchema.build())
                .plus1("optimize_for", YTree.stringNode("scan"));

        yt.cypress().create(tablePath, CypressNodeType.TABLE, tableAttributes);
    }

}
