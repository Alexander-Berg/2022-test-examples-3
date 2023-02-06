package ru.yandex.market.mboc.common.utils;

import java.util.HashMap;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class ErrorInfoTest {

    private static final long SEED = 5332L;
    private static final int TRIES = 100;

    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenCopyEqualsShouldReturnTrue() {
        for (int i = 0; i < TRIES; i++) {
            ErrorInfo errorInfo = random.nextObject(ErrorInfo.class);
            ErrorInfo copy = new ErrorInfo(errorInfo.getErrorCode(), errorInfo.getMessageTemplate(),
                errorInfo.getLevel(), new HashMap<>(errorInfo.getParams()));
            SoftAssertions.assertSoftly(s -> {
                s.assertThat(copy).isEqualTo(errorInfo);
                s.assertThat(copy.hashCode()).isEqualTo(errorInfo.hashCode());
            });
        }
    }

    @Test
    public void whenDiffernetErrorInfosEqualsShouldReturnFalse() {
        for (int i = 0; i < TRIES; i++) {
            ErrorInfo errorInfo = random.nextObject(ErrorInfo.class);
            ErrorInfo otherErrorInfo = random.nextObject(ErrorInfo.class);
            SoftAssertions.assertSoftly(s -> s.assertThat(errorInfo).isNotEqualTo(otherErrorInfo));
        }
    }

    @Test
    public void whenPassingIncorrectClassEqualsShouldReturnFalse() {
        Assertions.assertThat(random.nextObject(ErrorInfo.class)).isNotEqualTo("String");
    }
}
