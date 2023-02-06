package ru.yandex.market.tsum.pipelines.common.jobs.delivery.arcadia;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

@ParametersAreNonnullByDefault
public class ValidateModulePathTest {
    @Test
    public void correctPath() {
        GenerateArcadiaChangelogJob.validateModulePath("market/infra/tsum");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullValue() {
        GenerateArcadiaChangelogJob.validateModulePath(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void empty() {
        GenerateArcadiaChangelogJob.validateModulePath("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void blank() {
        GenerateArcadiaChangelogJob.validateModulePath("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void url() {
        GenerateArcadiaChangelogJob.validateModulePath("https://a.yandex-team.ru/arc/trunk/arcadia/market/infra/tsum/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void httpUrl() {
        GenerateArcadiaChangelogJob.validateModulePath("http://a.yandex-team.ru/arc/trunk/arcadia/market/infra/tsum/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void startsWithSlash() {
        GenerateArcadiaChangelogJob.validateModulePath("/market/infra/tsum");
    }

    @Test(expected = IllegalArgumentException.class)
    public void startsWithMultipleSlashes() {
        GenerateArcadiaChangelogJob.validateModulePath("///market/infra/tsum");
    }

    @Test(expected = IllegalArgumentException.class)
    public void onlyMultipleSlashes() {
        GenerateArcadiaChangelogJob.validateModulePath("///////");
    }

}
