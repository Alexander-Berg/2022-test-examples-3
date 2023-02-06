package ru.yandex.market.tsum.pipelines.common.jobs.health.util;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class ResourceUtilsTest {

    @Test
    public void readResource() {
        String content = ResourceUtils.INSTANCE.getResourceContent("sql/file.sql");
        Assert.assertTrue(content != null && !content.isEmpty());

        Assertions.assertThatCode(
                () -> ResourceUtils.INSTANCE.getResourceContent("sql/not-existfile.sql")
        ).hasMessageContaining("resource sql/not-existfile.sql not found");
    }
}
