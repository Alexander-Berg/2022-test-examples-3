package ru.yandex.market.mbo.audit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

/**
 * @author yuramalinov
 * @created 21.12.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ZookeeperIdGeneratorTest {
    @Test
    public void testGenerateRange() {
        List<Long> ids = ZookeeperIdGenerator.generateRange(100, 1000);
        Assertions.assertThat(ids).hasSize(100);
        Assertions.assertThat(ids.get(99)).isEqualTo(1000);
    }
}
