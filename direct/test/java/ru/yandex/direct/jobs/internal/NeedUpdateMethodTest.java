package ru.yandex.direct.jobs.internal;

import java.time.LocalDate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
class NeedUpdateMethodTest {

    static Object[] parametrizedTestData() {
        long ytTableLastUpdateUnixTime = RandomNumberUtils.nextPositiveLong();

        return new Object[][]{
                {"property value is null", ytTableLastUpdateUnixTime, null, true},
                {"property value less than ytTableLastUpdateUnixTime", ytTableLastUpdateUnixTime,
                        ytTableLastUpdateUnixTime - 1, true},
                {"property value equal ytTableLastUpdateUnixTime", ytTableLastUpdateUnixTime, ytTableLastUpdateUnixTime,
                        false},
                {"property value greater than ytTableLastUpdateUnixTime", ytTableLastUpdateUnixTime,
                        ytTableLastUpdateUnixTime + 1, false},
        };
    }

    @Mock
    private PpcProperty<Long> lastUpdateUnixTimeProperty;

    @Mock
    private PpcProperty<LocalDate> lastImportDateProperty;

    @BeforeEach
    void initTestData() {
        MockitoAnnotations.initMocks(this);
    }


    @ParameterizedTest(name = "check needUpdate method for UpdateTemplatePlaceJob when: {0}")
    @MethodSource("parametrizedTestData")
    void checkNeedUpdateMethod_forUpdateTemplatePlaceJob(String testDescription,
                                                         long ytTableLastUpdateUnixTime,
                                                         @Nullable Long lastUpdateUnixTime,
                                                         boolean expectedResult) {
        doReturn(lastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();
        boolean result = UpdateTemplatePlaceJob.needUpdate(ytTableLastUpdateUnixTime, lastUpdateUnixTimeProperty);

        assertThat(result)
                .isEqualTo(expectedResult);
    }


    @ParameterizedTest(name = "check needUpdate method for UpdateTemplateResourceJob when: {0}")
    @MethodSource("parametrizedTestData")
    void checkNeedUpdateMethod_forUpdateTemplateResourceJob(String testDescription,
                                                            long ytTableLastUpdateUnixTime,
                                                            @Nullable Long lastUpdateUnixTime,
                                                            boolean expectedResult) {
        doReturn(lastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();
        boolean result = UpdateTemplateResourceJob.needUpdate(ytTableLastUpdateUnixTime, lastUpdateUnixTimeProperty);

        assertThat(result)
                .isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "check needUpdate method for UpdateTemplatePlaceJob when: {0}")
    @MethodSource("parametrizedTestData")
    void checkNeedUpdateMethod_forUpdatePlacesJob(String testDescription,
                                                  long ytTableLastUpdateUnixTime, @Nullable Long lastUpdateUnixTime,
                                                  boolean expectedResult) {
        doReturn(lastUpdateUnixTime)
                .when(lastUpdateUnixTimeProperty).get();
        boolean result = UpdatePlacesJob.needUpdate(ytTableLastUpdateUnixTime, lastUpdateUnixTimeProperty);

        assertThat(result)
                .isEqualTo(expectedResult);
    }

    static Object[] importCryptaSegmentsJobParametrizedTestData() {
        LocalDate ytTableGenerateDate = LocalDate.of(2020, 9, 3);

        return new Object[][]{
                {"property value is null", ytTableGenerateDate, null, true},
                {"property value less than ytTableGenerateDate", ytTableGenerateDate,
                        ytTableGenerateDate.minusDays(1), true},
                {"property value equal ytTableGenerateDate", ytTableGenerateDate, ytTableGenerateDate, false},
                {"property value greater than ytTableGenerateDate", ytTableGenerateDate,
                        ytTableGenerateDate.plusDays(1), false},
        };
    }

    @ParameterizedTest(name = "check needUpdate method for ImportCryptaSegmentsJob when: {0}")
    @MethodSource("importCryptaSegmentsJobParametrizedTestData")
    void checkNeedUpdateMethod_forImportCryptaSegmentsJob(String testDescription,
                                                          LocalDate ytTableGenerateDate,
                                                          @Nullable LocalDate lastImportDate,
                                                          boolean expectedResult) {
        doReturn(lastImportDate)
                .when(lastImportDateProperty).get();
        boolean result = ImportCryptaSegmentsJob.needImport(ytTableGenerateDate, lastImportDateProperty);

        assertThat(result)
                .isEqualTo(expectedResult);
    }

}
