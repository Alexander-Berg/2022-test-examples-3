package ru.yandex.market.tsum.test_utils;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.idx.IdxUtils;

import static org.hamcrest.Matchers.equalTo;

public class IdxUtilsTest {

    @Test
    public void compareVersionsTest() {
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1.1.1.1"), equalTo(0));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1.1.1.2"), equalTo(-1));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.2", "1.1.1.1"), equalTo(1));

        Assert.assertThat(IdxUtils.versionCompare("1.1.1", "1.1.1.1"), equalTo(-1));
        Assert.assertThat(IdxUtils.versionCompare("1.1", "1.1.1.1"), equalTo(-1));
        Assert.assertThat(IdxUtils.versionCompare("1", "1.1.1.1"), equalTo(-1));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1.1.1"), equalTo(1));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1.1"), equalTo(1));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1"), equalTo(1));

        Assert.assertThat(IdxUtils.versionCompare("1.1.2.1", "1.1.1.1"), equalTo(1));
        Assert.assertThat(IdxUtils.versionCompare("1.2.1.1", "1.1.1.1"), equalTo(1));
        Assert.assertThat(IdxUtils.versionCompare("2.1.1.1", "1.1.1.1"), equalTo(1));

        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1.1.2.1"), equalTo(-1));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "1.2.1.1"), equalTo(-1));
        Assert.assertThat(IdxUtils.versionCompare("1.1.1.1", "2.1.1.1"), equalTo(-1));
    }
}
