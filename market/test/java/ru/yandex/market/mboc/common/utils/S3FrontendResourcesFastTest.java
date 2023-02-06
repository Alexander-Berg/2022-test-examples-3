package ru.yandex.market.mboc.common.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class S3FrontendResourcesFastTest {

    @Test
    public void testVersionMatch() {
        // Don't fail
        S3FrontendResources.checkVersionFormat("master.2020-06-24T13-06:00");
    }

    @Test
    public void testVersionMismatch() {
        Assertions.assertThatThrownBy(() -> S3FrontendResources.checkVersionFormat("master/2020-06-24T13-06:00"))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
