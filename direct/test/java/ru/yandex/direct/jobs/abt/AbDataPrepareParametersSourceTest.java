package ru.yandex.direct.jobs.abt;

import java.util.Comparator;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.jobs.abt.queryconf.AbPrepareQueryConf;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.ytwrapper.model.YtCluster.FREUD;
import static ru.yandex.direct.ytwrapper.model.YtCluster.ZENO;


class AbDataPrepareParametersSourceTest {

    @Test
    void getAllParamValuesTest() {
        var clusters = List.of(ZENO, FREUD);
        var queryConf1 = new TestQueryConf1();
        var queryConf2 = new TestQueryConf2();
        var queriesConfs = List.of(queryConf1, queryConf2);
        var abDataPrepareParametersSource = new AbDataPrepareParametersSource(clusters, queriesConfs);
        var gotParams = abDataPrepareParametersSource.getAllParamValues();
        var expectedParams = List.of(
                new AbDataPrepareParameter(ZENO, queryConf1),
                new AbDataPrepareParameter(FREUD, queryConf1),
                new AbDataPrepareParameter(ZENO, queryConf2),
                new AbDataPrepareParameter(FREUD, queryConf2)
        );
        assertThat(gotParams).hasSize(4);
        assertThat(gotParams)
                .usingComparatorForElementFieldsWithType(
                        Comparator.comparing(AbPrepareQueryConf::getQueryPath), AbPrepareQueryConf.class)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedParams.toArray(AbDataPrepareParameter[]::new));
    }

    @Test
    void getAllParamValues_EmptyQueryConfsTest() {
        var clusters = List.of(ZENO, FREUD);
        List<AbPrepareQueryConf> queriesConfs = List.of();
        var abDataPrepareParametersSource = new AbDataPrepareParametersSource(clusters, queriesConfs);
        var gotParams = abDataPrepareParametersSource.getAllParamValues();
        assertThat(gotParams).hasSize(0);
    }

    @Test
    void getAllParamValues_EmptyYtClustersTest() {
        List<YtCluster> clusters = List.of();
        var queryConf1 = new TestQueryConf1();
        var queryConf2 = new TestQueryConf2();
        var queriesConfs = List.of(queryConf1, queryConf2);
        var abDataPrepareParametersSource = new AbDataPrepareParametersSource(clusters, queriesConfs);
        var gotParams = abDataPrepareParametersSource.getAllParamValues();
        assertThat(gotParams).hasSize(0);
    }

    @Test
    void convertParamToStringTest() {
        var clusters = List.of(ZENO, FREUD);
        var queryConf1 = new TestQueryConf1();
        var queryConf2 = new TestQueryConf2();
        var queriesConfs = List.of(queryConf1, queryConf2);
        var abDataPrepareParametersSource = new AbDataPrepareParametersSource(clusters, queriesConfs);
        var expectedParams = List.of(
                new AbDataPrepareParameter(ZENO, queryConf1),
                new AbDataPrepareParameter(FREUD, queryConf1),
                new AbDataPrepareParameter(ZENO, queryConf2),
                new AbDataPrepareParameter(FREUD, queryConf2)
        );
        SoftAssertions softAssertions = new SoftAssertions();
        for (var expectedParam : expectedParams) {
            softAssertions.assertThat(abDataPrepareParametersSource.convertParamToString(expectedParam))
                    .isEqualTo(expectedParam.getYtCluster().name() + "---" + expectedParam.getQueryConfiguration().getQueryPath());
        }

        softAssertions.assertAll();

    }

    @Test
    void convertStringToParamTest() {
        var clusters = List.of(ZENO, FREUD);
        var queryConf1 = new TestQueryConf1();
        var queryConf2 = new TestQueryConf2();
        var queriesConfs = List.of(queryConf1, queryConf2);
        var abDataPrepareParametersSource = new AbDataPrepareParametersSource(clusters, queriesConfs);
        var expectedParams = List.of(
                new AbDataPrepareParameter(ZENO, queryConf1),
                new AbDataPrepareParameter(FREUD, queryConf1),
                new AbDataPrepareParameter(ZENO, queryConf2),
                new AbDataPrepareParameter(FREUD, queryConf2)
        );
        SoftAssertions softAssertions = new SoftAssertions();
        for (var expectedParam : expectedParams) {
            var paramString =
                    expectedParam.getYtCluster().name() + "---" + expectedParam.getQueryConfiguration().getQueryPath();
            softAssertions.assertThat(abDataPrepareParametersSource.convertStringToParam(paramString))
                    .usingComparatorForType(
                            Comparator.comparing(AbPrepareQueryConf::getQueryPath), AbPrepareQueryConf.class)
                    .isEqualToComparingFieldByField(expectedParam);
        }

        softAssertions.assertAll();

    }

    @Test
    void convertStringToParam_UnsupportedStringTest() {
        var clusters = List.of(ZENO, FREUD);
        var queryConf1 = new TestQueryConf1();
        var queryConf2 = new TestQueryConf2();
        var queriesConfs = List.of(queryConf1, queryConf2);
        var abDataPrepareParametersSource = new AbDataPrepareParametersSource(clusters, queriesConfs);
        var paramString =
                "ARNOLD---query_path1.sql";
        assertThatThrownBy(() -> abDataPrepareParametersSource.convertStringToParam(paramString))
                .isInstanceOf(IllegalStateException.class);
    }

    private static class TestQueryConf1 implements AbPrepareQueryConf {
        @Override
        public String getDestTable() {
            return "dest_table1";
        }

        @Override
        public String getQueryPath() {
            return "query_path1.sql";
        }
    }

    private static class TestQueryConf2 implements AbPrepareQueryConf {
        @Override
        public String getDestTable() {
            return "dest_table2";
        }

        @Override
        public String getQueryPath() {
            return "query_path2.sql";
        }
    }

}
