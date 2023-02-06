package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlogbroker.mysql.MysqlUtil;

@ParametersAreNonnullByDefault
public class BinlogbrokerUtilTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void parsingTruncateTable() {
        List<String> truncateTables = Arrays.asList(
                "truncate table foobar",
                "TRUNCATE TABLE foobar",
                "TrUnCaTe TaBlE foobar",
                "   \nTRUNCATE /* some comment */TABLE/*other comment*//*comment*/ \n `foobar` /*comment*/",
                "TRUNCATE /* /* /* mwahaha */ table `foobar`",
                "truncate table/* haha */foobar;");
        for (String query : truncateTables) {
            softly.assertThat(MysqlUtil.looksLikeTruncateTable(query))
                    .describedAs(query)
                    .isEqualTo("foobar");
        }
        List<String> notTruncateTables = Arrays.asList(
                "drop table foobar",
                "/* truncate table foobar */ drop table foobar");
        for (String query : notTruncateTables) {
            softly.assertThat(MysqlUtil.looksLikeTruncateTable(query))
                    .describedAs(query)
                    .isNull();
        }
    }

    @Test
    public void parsingRenameTable() {
        List<String> renameQueries = Arrays.asList(
                "alter table foo rename bar;",
                "rename table foo to bar, bar to baz, baz to foo;",
                "rename table `ppc`.`banners` to `ppc`.`_banners_old`, `ppc`.`_banners_new` to `ppc`.`banners`;",
                "  \n RenaMe TABLE foo To bar /* dat comment */, bar to baz, /* baz to foo */ \n `baz` to `foo` \n"
        );
        List<List<Pair<String, String>>> results = Arrays.asList(
                Collections.singletonList(Pair.of("foo", "bar")),
                Arrays.asList(
                        Pair.of("foo", "bar"),
                        Pair.of("bar", "baz"),
                        Pair.of("baz", "foo")),
                Arrays.asList(
                        Pair.of("banners", "_banners_old"),
                        Pair.of("_banners_new", "banners")),
                Arrays.asList(
                        Pair.of("foo", "bar"),
                        Pair.of("bar", "baz"),
                        Pair.of("baz", "foo")));

        Preconditions.checkState(renameQueries.size() == results.size());
        for (int ind = 0; ind < renameQueries.size(); ++ind) {
            String query = renameQueries.get(ind);
            softly.assertThat(MysqlUtil.looksLikeRenameTable(query))
                    .describedAs(query)
                    .isEqualTo(results.get(ind));
        }

        List<String> notRenameQueries = Arrays.asList(
                "drop table foobar",
                "/* rename table foo to bar */ drop table foobar");
        for (String query : notRenameQueries) {
            softly.assertThat(MysqlUtil.looksLikeRenameTable(query))
                    .describedAs(query)
                    .isNull();
        }
    }

    @Test
    public void parsingRenameColumn() {
        List<String> renameQueries = Arrays.asList(
                "alter table camp_options change column banners_per_page banners_per_page int(10) unsigned NOT NULL default 0;",
                "alter table geo_regions add column type tinyint not null after parent_id, change column eng_name ename varchar(100) not null;",
                "alter table campaigns change column sum_spent_units sum_spent_units bigint(20) not null, change column sum_units sum_units bigint(20) not null",
                "ALTER tAblE warnings_60_3 chanGe Column type old_place tinyint unsigned NOT NULL",
                "alter table campaigns change column timeTarget timeTarget varchar(400) default NULL",
                "alter table camp_options change column budget_strategy budget_strategy enum('distributed', 'fast', 'easy_week_bundle') default 'distributed'",
                "alter table bids_phraseid_history change column bsIdHistory phraseIdHistory varchar(100) default null",
                "alter table mediaplan_bids change column phraseIdHistory phraseIdHistory text default null",
                "alter table bids_phraseid_history change column phraseIdHistory phraseIdHistory text default null",
                "alter table campaigns change column statusYacobotDeleted statusYacobotDeleted_to_delete enum('Yes','No') NOT NULL DEFAULT 'Yes', algorithm=inplace",
                "alter table inc_daas_brief_id change column daas_task_id daas_brief_id bigint(20) not null auto_increment, rename column a to b"
        );
        List<List<Pair<String, String>>> results = Arrays.asList(
                null,
                Collections.singletonList(
                        Pair.of("eng_name", "ename")),
                null,
                Collections.singletonList(
                        Pair.of("type", "old_place")),
                null,
                null,
                Collections.singletonList(
                        Pair.of("bsIdHistory", "phraseIdHistory")),
                null,
                null,
                Collections.singletonList(
                        Pair.of("statusYacobotDeleted", "statusYacobotDeleted_to_delete")),
                Arrays.asList(
                        Pair.of("a", "b"),
                        Pair.of("daas_task_id", "daas_brief_id")
                )
        );

        Preconditions.checkState(renameQueries.size() == results.size());
        for (int ind = 0; ind < renameQueries.size(); ++ind) {
            String query = renameQueries.get(ind);
            softly.assertThat(MysqlUtil.looksLikeRenameColumn(query))
                    .describedAs(query)
                    .isEqualTo(results.get(ind));
        }
    }
}
