package ru.yandex.direct.grid.processing.service.operator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class OperatorAccessServiceCanReadTest extends OperatorAccessServiceBaseTest {
    private static final Long TEST_USER_ID = RandomNumberUtils.nextPositiveLong();

    @Test
    public void getCanRead_true() {
        doReturn(true).when(rbacService).canRead(eq(TEST_USER_ID), eq(TEST_USER_ID));
        assertThat(operatorAccessService.operatorCanRead(TEST_USER_ID, TEST_USER_ID)).isTrue();
    }

    @Test
    public void getCanRead_false() {
        doReturn(false).when(rbacService).canRead(eq(TEST_USER_ID), eq(TEST_USER_ID));
        assertThat(operatorAccessService.operatorCanRead(TEST_USER_ID, TEST_USER_ID)).isFalse();
    }
}
