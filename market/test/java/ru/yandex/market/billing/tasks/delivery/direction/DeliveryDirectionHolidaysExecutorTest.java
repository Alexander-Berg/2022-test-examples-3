package ru.yandex.market.billing.tasks.delivery.direction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.billing.config.BillingMdsS3Config;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.util.JsonTestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link DeliveryDirectionHolidaysExecutor}.
 * Проверяются, в том числе, классы:
 * {@link DeliveryDirectionHolidaysWriter},
 * {@link DeliveryDirectionSchedulesProviderHardcoded}.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryDirectionHolidaysExecutorTest {

    private static final int DAY_COUNT = 10;
    private static final int DAY_OFFSET = -10;
    private static final LocalDate START_DATE = LocalDate.parse("2018-03-04");

    private static final List<DeliveryDirectionSchedule> CUSTOM_SCHEDULES = ImmutableList.of(
            new DeliveryDirectionSchedule(1111L, 2, 5, "246"),
            new DeliveryDirectionSchedule(2222L, 2, 5, "135")
    );

    @Mock
    private NamedHistoryMdsS3Client historyMdsS3Client;
    @Mock
    private DeliveryDirectionSchedulesProvider emptyProvider;
    @Mock
    private DeliveryDirectionSchedulesProvider customProvider;

    private DeliveryDirectionSchedulesProvider hardcodedProvider;

    private DeliveryDirectionHolidaysWriter writer;

    @BeforeEach
    void setUp() {
        doReturn(Collections.emptyList()).when(emptyProvider).provideSchedules();
        doReturn(CUSTOM_SCHEDULES).when(customProvider).provideSchedules();
        hardcodedProvider = new DeliveryDirectionSchedulesProviderHardcoded();

        DeliveryDirectionHolidaysWriter realWriter = new DeliveryDirectionHolidaysWriter(DAY_COUNT, DAY_OFFSET);
        writer = spy(realWriter);
        doReturn(START_DATE).when(writer).getStartDate();
    }

    private void mockCheckResult(String jsonFileName) {
        when(historyMdsS3Client.upload(eq(BillingMdsS3Config.DELIVERY_DIRECTION_HOLIDAYS_RESOURCE), any()))
                .then(invocation -> {
                    ContentProvider contentProvider = invocation.getArgument(1);
                    JsonTestUtil.compareJson(contentProvider.getInputStream(), this.getClass(), jsonFileName);
                    return null;
                });
    }

    @Test
    void testHardcoded() throws IOException, JSONException {
        mockCheckResult("delivery-direction-holidays-hardcoded.json");

        DeliveryDirectionHolidaysExecutor executor = new DeliveryDirectionHolidaysExecutor(
                ImmutableList.of(emptyProvider, hardcodedProvider), writer, historyMdsS3Client);

        executor.doJob(null);

        verify(historyMdsS3Client).upload(eq(BillingMdsS3Config.DELIVERY_DIRECTION_HOLIDAYS_RESOURCE), any());
    }

    @Test
    void testCustom() throws IOException, JSONException {
        mockCheckResult("delivery-direction-holidays-custom.json");

        DeliveryDirectionHolidaysExecutor executor = new DeliveryDirectionHolidaysExecutor(
                ImmutableList.of(emptyProvider, customProvider), writer, historyMdsS3Client);

        executor.doJob(null);

        verify(historyMdsS3Client).upload(eq(BillingMdsS3Config.DELIVERY_DIRECTION_HOLIDAYS_RESOURCE), any());
    }

    @Test
    void testMix() throws IOException, JSONException {
        mockCheckResult("delivery-direction-holidays-mix.json");

        DeliveryDirectionHolidaysExecutor executor = new DeliveryDirectionHolidaysExecutor(
                ImmutableList.of(emptyProvider, customProvider, hardcodedProvider), writer, historyMdsS3Client);

        executor.doJob(null);

        verify(historyMdsS3Client).upload(eq(BillingMdsS3Config.DELIVERY_DIRECTION_HOLIDAYS_RESOURCE), any());
    }

}
