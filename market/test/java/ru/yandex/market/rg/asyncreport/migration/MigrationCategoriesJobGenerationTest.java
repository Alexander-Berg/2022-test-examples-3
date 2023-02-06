package ru.yandex.market.rg.asyncreport.migration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.SyncAPI.SyncCategory;
import Market.DataCamp.SyncAPI.SyncCategory.PartnerCategoriesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.rg.asyncreport.migration.MigrationCategoriesJobGeneration.CategoryComparatorMode;
import ru.yandex.market.rg.config.FunctionalTest;

import static Market.DataCamp.PartnerCategoryOuterClass.PartnerCategoriesBatch;
import static Market.DataCamp.PartnerCategoryOuterClass.PartnerCategory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;

/**
 * Тесты для {@link MigrationCategoriesJobGeneration}.
 */
@DbUnitDataSet(before = "csv/MigrationCategoriesJobGenerationTest.before.csv")
class MigrationCategoriesJobGenerationTest extends FunctionalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("dataCampMigrationClient")
    private DataCampClient dataCampMigrationClient;

    @Autowired
    private MboMappingsService mboMappingsService;

    @Autowired
    private MigrationCategoriesJobGeneration migrationCategoriesJobGeneration;

    @Test
    @DisplayName("Отчет в статусе SUCCESS")
    public void fullMigrationJobTest() throws IOException {
        //given
        willAnswer(invocation -> generateResponse(generateCategories(15, 1L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(1L));
        willAnswer(invocation -> generateResponse(generateCategories(10, 2L)))
                .willAnswer(invocation -> generateResponse(generateCategories(15, 2L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(2L));
        willAnswer(invocation -> generateResponse(generateCategories(5, 2L)))
                .given(dataCampMigrationClient)
                .addNewCategoriesToBusiness(any(SyncCategory.UpdatePartnerCategories.class), anyLong());
        //when
        MigrationCategoriesParams params = objectMapper.readValue("{\"entityId\":10435983,\"partnerId\":10435983," +
                "\"sourceBusinessId\":\"1\",\"targetBusinessId\":\"2\"}", MigrationCategoriesParams.class);
        ReportResult result = migrationCategoriesJobGeneration.generate("123", params);
        //then
        ReportState state = result.getNewState();
        Assertions.assertThat(state)
                .isEqualTo(ReportState.DONE);
    }

    @Test
    @DisplayName("Переносим категории из МБО в ЕОХ. Успех")
    public void testMboSuccess() {
        MboMappings.SearchMappingsResponse mboResponse = ProtoTestUtil.getProtoMessageByJson(
                MboMappings.SearchMappingsResponse.class,
                "proto/MigrationCategoriesJobGenerationTest.testMboSuccess.proto.json",
                getClass()
        );
        Mockito.when(mboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(mboResponse);

        // В ответе из МБО категории:      2, 5
        // В ЕОХ до миграции категории:    0, 1, 2, 3, 4
        // В ЕОХ после миграции категории: 0, 1, 2, 3, 4, 5
        Mockito.when(dataCampMigrationClient.getPartnerCategories(eq(2L)))
                .thenReturn(generateResponse(generateCategories(5, 2L, false)))
                .thenReturn(generateResponse(generateCategories(6, 2L, false)));

        //when
        MigrationCategoriesParams params = new MigrationCategoriesParams(1L, 2L, true, 1001L);
        ReportResult result = migrationCategoriesJobGeneration.generate("123", params);

        //then
        ReportState state = result.getNewState();
        Assertions.assertThat(state)
                .isEqualTo(ReportState.DONE);
    }

    @Test
    @DisplayName("Отчет в статусе FAIL если не получили один из исходных списков категорий")
    public void fullMigrationJobTestfail1() {
        //given
        willThrow(UncheckedIOException.class)
                .given(dataCampMigrationClient).getPartnerCategories(eq(1L));
        willAnswer(invocation -> generateResponse(generateCategories(10, 2L)))
                .willAnswer(invocation -> generateResponse(generateCategories(15, 2L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(2L));
        willAnswer(invocation -> generateResponse(generateCategories(5, 2L)))
                .given(dataCampMigrationClient)
                .addNewCategoriesToBusiness(any(), anyLong());
        //when
        MigrationCategoriesParams params = new MigrationCategoriesParams(1L, 2L, null, null);
        //then
        Assertions.assertThatThrownBy(() -> migrationCategoriesJobGeneration.generate("123", params))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Отчет в статусе FAIL если добавление упало с ошибкой")
    public void fullMigrationJobTest2() {
        //given
        willAnswer(invocation -> generateResponse(generateCategories(15, 1L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(1L));
        willAnswer(invocation -> generateResponse(generateCategories(10, 2L)))
                .willAnswer(invocation -> generateResponse(generateCategories(15, 2L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(2L));
        willThrow(UncheckedIOException.class)
                .given(dataCampMigrationClient)
                .addNewCategoriesToBusiness(any(), anyLong());
        //when
        MigrationCategoriesParams params = new MigrationCategoriesParams(1L, 2L, null, null);
        //then
        Assertions.assertThatThrownBy(() -> migrationCategoriesJobGeneration.generate("123", params))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Отчет в статусе FAIL если после добавления не все категории есть в таргете")
    public void fullMigrationJobTest3() {
        //given
        willAnswer(invocation -> generateResponse(generateCategories(15, 1L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(1L));
        willAnswer(invocation -> generateResponse(generateCategories(10, 2L)))
                .willAnswer(invocation -> generateResponse(generateCategories(13, 2L)))
                .given(dataCampMigrationClient).getPartnerCategories(eq(2L));
        willAnswer(invocation -> generateResponse(generateCategories(5, 2L)))
                .given(dataCampMigrationClient)
                .addNewCategoriesToBusiness(any(), anyLong());
        //when
        MigrationCategoriesParams params = new MigrationCategoriesParams(1L, 2L, null, null);
        ReportResult result = migrationCategoriesJobGeneration.generate("123", params);
        //then
        ReportState state = result.getNewState();
        Assertions.assertThat(state)
                .isEqualTo(ReportState.FAILED);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Ищем отличия в категориях")
    @MethodSource("testDifferenceData")
    void testDifference(String name, CategoryComparatorMode mode, List<PartnerCategory> src, Set<Object> dst, List<PartnerCategory> expected) {
        Set<Object> keys = new HashSet<>(dst);
        List<PartnerCategory> actual = migrationCategoriesJobGeneration.findDifference(src, keys, mode);
        Assertions.assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Stream<Arguments> testDifferenceData() {
        return Stream.of(
                Arguments.of(
                        "По id. Нет отличий в категориях",
                        CategoryComparatorMode.BY_ID,
                        generateCategories(10, 1L),
                        Set.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                        List.of()
                ),
                Arguments.of(
                        "По id. Цели не хватает каких-то категорий",
                        CategoryComparatorMode.BY_ID,
                        generateCategories(15, 1L),
                        Set.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L),
                        generateCategories(15, 1L).stream()
                                .skip(10)
                                .collect(Collectors.toList())
                ),
                Arguments.of(
                        "По id. Цель содержит все необходимые категории + свои",
                        CategoryComparatorMode.BY_ID,
                        generateCategories(10, 1L),
                        Set.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L),
                        List.of()
                ),
                Arguments.of(
                        "По названию. Нет отличий в категориях",
                        CategoryComparatorMode.BY_NAME,
                        generateCategories(10, 1L, false),
                        Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                        List.of()
                ),
                Arguments.of(
                        "По названию. Цели не хватает каких-то категорий",
                        CategoryComparatorMode.BY_NAME,
                        generateCategories(15, 1L, false),
                        Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                        generateCategories(15, 1L, false).stream()
                                .skip(10)
                                .collect(Collectors.toList())
                ),
                Arguments.of(
                        "По названию. Цель содержит все необходимые категории + свои",
                        CategoryComparatorMode.BY_NAME,
                        generateCategories(10, 1L, false),
                        Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"),
                        List.of()
                )
        );
    }

    @Test
    @DisplayName("Корректно-ли строим категорию для добавления")
    public void correctBuildCategoryForTarget() {
        Long targetBusinessId = 100L;
        PartnerCategory categoryToChange = generateCategories(1, 1L).get(0);
        PartnerCategory expected = categoryToChange.toBuilder()
                .setBusinessId(targetBusinessId.intValue())
                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                        .setSource(DataCampOfferMeta.DataSource.MARKET_MBI_MIGRATOR)
                        .build())
                .build();
        PartnerCategory actual
                = migrationCategoriesJobGeneration.buildCategoryForTarget(categoryToChange, targetBusinessId);
        ProtoTestUtil.assertThat(actual)
                .ignoringFieldsMatchingRegexes(".*timestamp.*")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Генерируем отчет в котором есть ошибки")
    public void generateReportWithErrors() {
        long targetBusinessId = 100;
        List<PartnerCategory> src = generateCategories(10, 1L);
        Set<Object> dst = generateCategories(15, targetBusinessId).stream()
                .skip(5)
                .map(PartnerCategory::getId)
                .collect(Collectors.toSet());

        File reportFile = TempFileUtils.createTempFile();
        boolean errors = migrationCategoriesJobGeneration.generateReport(targetBusinessId, src, dst, CategoryComparatorMode.BY_ID, reportFile);
        Assertions.assertThat(errors)
                .isTrue();
    }

    @Test
    @DisplayName("Генерируем отчет без ошибок")
    public void generateReportWithoutErrors() {
        long targetBusinessId = 100;
        List<PartnerCategory> src = generateCategories(10, 1L);
        Set<Object> dst = generateCategories(15, targetBusinessId).stream()
                .map(PartnerCategory::getId)
                .collect(Collectors.toSet());

        File reportFile = TempFileUtils.createTempFile();
        boolean errors = migrationCategoriesJobGeneration.generateReport(targetBusinessId, src, dst, CategoryComparatorMode.BY_ID, reportFile);
        Assertions.assertThat(errors)
                .isFalse();
    }

    private static List<PartnerCategory> generateCategories(int count, Long businessId) {
        return generateCategories(count, businessId, true);
    }

    private static List<PartnerCategory> generateCategories(int count, Long businessId, boolean withIds) {
        List<PartnerCategory> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PartnerCategory.Builder builder = PartnerCategory.newBuilder()
                    .setBusinessId(businessId.intValue())
                    .setName(String.valueOf(i));
            if (withIds) {
                builder.setId(i);
            }

            result.add(builder.build());
        }
        return result;
    }

    private PartnerCategoriesResponse generateResponse(List<PartnerCategory> categories) {
        PartnerCategoriesBatch batch = PartnerCategoriesBatch.newBuilder().addAllCategories(categories).build();
        return PartnerCategoriesResponse.newBuilder().setCategories(batch).build();
    }
}
