package ru.yandex.market.notification.safe.model.vo;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.result.NotificationResult;
import ru.yandex.market.notification.model.transport.result.NotificationResultErrorInfo;
import ru.yandex.market.notification.simple.model.result.NotificationResultImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
        var info = SendResultInfo.empty();

        assertThat(info.getErrorMessages(), empty());
        assertThat(info.getResults(), empty());
        assertThat(info.getAllErrors(), empty());
        assertThat(info.hasError(), equalTo(false));
    }

    @Test
    public void testMessage() {
        var info = SendResultInfo.message(ERROR_MESSAGE);

        checkTextErrorMessage(info);
    }

    @Test
    public void testException() {
        var ex = new RuntimeException(ERROR_MESSAGE);
        var info = SendResultInfo.exception(ex);

        checkTextErrorMessage(info);
    }

    @Test
    public void testResult() {
        var info = createdMockedResult();

        assertThat(info.getErrorMessages(), empty());
        assertThat(info.getResults(), hasSize(1));
        assertThat(info.getAllErrors(), hasSize(1));
        assertThat(info.hasError(), equalTo(true));
    }

    @Test
    public void testMerge() {
        var info1 = SendResultInfo.exception(new RuntimeException(ERROR_MESSAGE));
        var info2 = SendResultInfo.message(ERROR_MESSAGE);
        var info3 = createdMockedResult();
        var info = SendResultInfo.merge(List.of(info1, info2, info3));

        assertThat(info.getErrorMessages(), hasSize(2));
        assertThat(info.getResults(), hasSize(1));
        assertThat(info.getAllErrors(), hasSize(3));
        assertThat(info.hasError(), equalTo(true));
    }


    private void checkTextErrorMessage(SendResultInfo info) {
        assertThat(info.getErrorMessages(), hasSize(1));
        assertThat(info.getResults(), empty());
        assertThat(info.getAllErrors(), hasSize(1));
        assertThat(info.hasError(), equalTo(true));
    }

    private SendResultInfo createdMockedResult() {
        NotificationResult result = new NotificationResultImpl(
            Collections.emptyMap(),
            Collections.singletonMap(
                mock(NotificationAddress.class),
                mock(NotificationResultErrorInfo.class)
            )
        );
        return SendResultInfo.result(result);
    }

}
