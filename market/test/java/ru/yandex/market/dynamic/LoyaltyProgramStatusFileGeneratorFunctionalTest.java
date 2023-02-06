package ru.yandex.market.dynamic;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.loyalty.service.LoyaltyInformationService;

@DbUnitDataSet(before = "LoyaltyProgramStatusFileGeneratorTest.before.csv")
class LoyaltyProgramStatusFileGeneratorFunctionalTest extends FunctionalTest {
    private LoyaltyProgramStatusFileGenerator loyaltyProgramStatusFileGenerator;
    private static final Map<String, List<Long>> EXPECTED_CHANGED_STATUSES = Map.of(
            "ENABLED", Arrays.asList(1L, 3L),
            "DISABLED", Arrays.asList(2L, 4L)
    );

    @Autowired
    private LoyaltyInformationService loyaltyInformationService;

    @BeforeEach
    void init() {
        loyaltyProgramStatusFileGenerator = new LoyaltyProgramStatusFileGenerator(loyaltyInformationService);
    }

    @Test
    void generateLoyaltyProgramStatusDynamicFile() {
        File recentlyChangedFile = loyaltyProgramStatusFileGenerator.generate(4815162342L);
        Assertions.assertNotNull(recentlyChangedFile);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, List<Long>> changedStatuses =
                    objectMapper.readValue(recentlyChangedFile, new TypeReference<Map<String, List<Long>>>() {
                    });
            Assertions.assertEquals(EXPECTED_CHANGED_STATUSES.keySet(), changedStatuses.keySet());
            EXPECTED_CHANGED_STATUSES.forEach((key, value) ->
                    Assertions.assertEquals(EXPECTED_CHANGED_STATUSES.get(key), changedStatuses.get(key)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            FileUtils.deleteQuietly(recentlyChangedFile);
        }
    }
}
