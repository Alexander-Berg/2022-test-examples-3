package ru.yandex.market.logistics.lms.client.utils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.params.provider.Arguments;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class LmsLomClientsChainTestCases {

    public static final List<LmsLomClientsChainTestCase> TEST_CASES = buildTestCases();

    @Nonnull
    private List<LmsLomClientsChainTestCase> buildTestCases() {
        List<LmsLomClientsChainTestCase> cases = new ArrayList<>(16);

        for (int i = 0; i < 16; i++) {
            LmsLomClientsChainTestCase flagCase = new LmsLomClientsChainTestCase()
                .setDataExistsInRedis((i & 1) != 0)
                .setDataExistsInYt((i & 2) != 0)
                .setFetchingFromYtEnabled((i & 4) != 0)
                .setDataExistsInLms((i & 8) != 0);

            flagCase.displayName = String.join(
                ", ",
                flagCase.dataExistsInRedis ? "Данные есть в Redis" : "Данных нет в Redis",
                flagCase.dataExistsInYt ? "данные есть в YT" : "данных нет в YT",
                flagCase.fetchingFromYtEnabled ? "получение из YT включено" : "получение из YT выключено",
                flagCase.dataExistsInLms ? "данные есть в LMS" : "данных нет в LMS"
            );
            cases.add(flagCase);
        }

        return cases;
    }

    @Data
    @Accessors(chain = true)
    public static class LmsLomClientsChainTestCase {
        private String displayName;
        private boolean dataExistsInRedis;
        private boolean dataExistsInYt;
        private boolean fetchingFromYtEnabled;
        private boolean dataExistsInLms;

        @Nonnull
        public Arguments getArguments(String testName) {
            return Arguments.of(
                testName + ": " + displayName,
                dataExistsInRedis,
                dataExistsInYt,
                fetchingFromYtEnabled,
                dataExistsInLms
            );
        }
    }
}
