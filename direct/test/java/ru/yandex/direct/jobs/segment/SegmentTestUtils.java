package ru.yandex.direct.jobs.segment;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import junitparams.converters.Nullable;
import org.apache.commons.beanutils.BeanUtils;

import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtSQLSyntaxVersion;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.segment.common.SegmentUtils.ROW_COUNT_ATTRIBUTE_NAME;

public class SegmentTestUtils {

    public static final Function<LocalDate, String> TEST_LOG_PATH_PROVIDER = date -> "/" + date.toString();

    public static YtTable createYtTable(LocalDate logDate) {
        return new YtTable(TEST_LOG_PATH_PROVIDER.apply(logDate));
    }

    public static YtOperator getDefaultYtOperatorMock(
            Supplier<Function<YtTable, Boolean>> mockedExistsMethodProvider) {
        YtOperator ytOperator = mock(YtOperator.class);

        when(ytOperator.exists(any())).thenAnswer(invocation -> {
            YtTable ytTable = invocation.getArgument(0);
            return mockedExistsMethodProvider.get().apply(ytTable);
        });

        when(ytOperator.readTableNumericAttribute(any(), eq(ROW_COUNT_ATTRIBUTE_NAME)))
                .thenReturn(100L);

        return ytOperator;
    }

    public static Function<YtTable, Boolean> getDefaultMockedYtOperatorExistsMethod(
            Supplier<LocalDate> startDateSupplier,
            Supplier<LocalDate> finishDateSupplier,
            @Nullable Supplier<Set<LocalDate>> missedDaysSupplier) {
        return ytTable -> {
            LocalDate tableDate = LocalDate.parse(ytTable.getPath().substring(2));
            if (missedDaysSupplier != null && missedDaysSupplier.get().contains(tableDate)) {
                return false;
            }
            return !tableDate.isAfter(finishDateSupplier.get()) &&
                    !tableDate.isBefore(startDateSupplier.get());
        };
    }

    public static YtOperator getDefaultYtOperatorMock(
            Supplier<LocalDate> startDateSupplier,
            Supplier<LocalDate> finishDateSupplier,
            @Nullable Supplier<Set<LocalDate>> missedDaysSupplier) {
        return getDefaultYtOperatorMock(
                () -> getDefaultMockedYtOperatorExistsMethod(
                        startDateSupplier, finishDateSupplier, missedDaysSupplier));
    }

    public static YtOperator getDefaultYtOperatorMock(
            Supplier<LocalDate> startDateSupplier,
            Supplier<LocalDate> finishDateSupplier) {
        return getDefaultYtOperatorMock(startDateSupplier, finishDateSupplier, null);
    }

    public static YtProvider getDefaultYtProviderMock(YtCluster ytCluster, YtOperator ytOperator) {
        YtProvider ytProvider = mock(YtProvider.class);

        when(ytProvider.getOperator(eq(ytCluster), eq(YtSQLSyntaxVersion.SQLv1))).thenReturn(ytOperator);

        return ytProvider;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cloneIt(T t) {
        try {
            return (T) BeanUtils.cloneBean(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
