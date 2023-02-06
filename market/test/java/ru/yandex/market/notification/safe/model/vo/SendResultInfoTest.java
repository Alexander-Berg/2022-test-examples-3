package ru.yandex.market.notification.safe.model.vo;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.result.NotificationResult;
import ru.yandex.market.notification.model.transport.result.NotificationResultErrorInfo;
import ru.yandex.market.notification.simple.model.result.NotificationResultImpl;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link SendResultInfo}.
 *
 * @author Vladislav Bauer
 */
public class SendResultInfoTest {

    private static final String ERROR_MESSAGE = "Error message";


    @Test
    public void testEmpty() {
        final SendResultInfo info = SendResultInfo.empty();

        assertThat(info.getErrorMessages(), empty());
        assertThat(info.getResults(), empty());
        assertThat(info.getAllErrors(), empty());
        assertThat(info.hasError(), equalTo(false));
    }

    @Test
    public void testMessage() {
        final SendResultInfo info = SendResultInfo.message(ERROR_MESSAGE);

        checkTextErrorMessage(info);
    }

    @Test
    public void testException() {
        final RuntimeException ex = new RuntimeException(ERROR_MESSAGE);
        final SendResultInfo info = SendResultInfo.exception(ex);

        checkTextErrorMessage(info);
    }

    @Test
    public void testResult() {
        final SendResultInfo info = createdMockedResult();

        assertThat(info.getErrorMessages(), empty());
        assertThat(info.getResults(), hasSize(1));
        assertThat(info.getAllErrors(), hasSize(1));
        assertThat(info.hasError(), equalTo(true));
    }

    @Test
    public void testMerge() {
        final SendResultInfo info1 = SendResultInfo.exception(new RuntimeException(ERROR_MESSAGE));
        final SendResultInfo info2 = SendResultInfo.message(ERROR_MESSAGE);
        final SendResultInfo info3 = createdMockedResult();
        final SendResultInfo info = SendResultInfo.merge(asList(info1, info2, info3));

        assertThat(info.getErrorMessages(), hasSize(2));
        assertThat(info.getResults(), hasSize(1));
        assertThat(info.getAllErrors(), hasSize(3));
        assertThat(info.hasError(), equalTo(true));
    }


    private void checkTextErrorMessage(final SendResultInfo info) {
        assertThat(info.getErrorMessages(), hasSize(1));
        assertThat(info.getResults(), empty());
        assertThat(info.getAllErrors(), hasSize(1));
        assertThat(info.hasError(), equalTo(true));
    }

    private SendResultInfo createdMockedResult() {
        final NotificationResult result = new NotificationResultImpl(
            Collections.emptyMap(),
            Collections.singletonMap(
                mock(NotificationAddress.class),
                mock(NotificationResultErrorInfo.class)
            )
        );
        return SendResultInfo.result(result);
    }

}
