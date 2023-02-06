package ru.yandex.market.mbo.gwt.server.remote;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.tarif.CategoryOperationId;
import ru.yandex.market.mbo.billing.tarif.ExternalTariffDAOMock;
import ru.yandex.market.mbo.billing.tarif.Tarif;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.billing.tarif.TarifProviderImpl;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.client.models.billing.TariffPriceConverter;
import ru.yandex.market.mbo.gwt.models.GwtPair;
import ru.yandex.market.mbo.gwt.models.billing.ExternalTariff;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 29.06.18
 */
@SuppressWarnings("checkstyle:lineLength")
public class ReportServiceImplTest {

    private static final long CATEGORY_GURU = 10;
    private static final long CATEGORY_HID = 100;

    private ReportServiceImpl reportService;
    private ParameterLoaderServiceStub parameterLoaderService;
    private GuruCategoryService guruCategoryService;

    @Before
    public void setUp() {
        TarifManager tarifManager = Mockito.mock(TarifManager.class);
        guruCategoryService = Mockito.mock(GuruCategoryService.class);
        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryEntities(new CategoryEntities(CATEGORY_HID, Collections.emptyList()));

        CategoryMappingServiceMock categoryMappingService = new CategoryMappingServiceMock();
        categoryMappingService.addMapping(CATEGORY_HID, CATEGORY_GURU);

        reportService = Mockito.spy(new ReportServiceImpl());
        ReflectionTestUtils.setField(reportService, "tarifManager", tarifManager);
        ReflectionTestUtils.setField(reportService, "externalTariffDAO", new ExternalTariffDAOMock());
        ReflectionTestUtils.setField(reportService, "tarifMultiplicatorService",
            new TarifMultiplicatorService(guruCategoryService, parameterLoaderService));
        ReflectionTestUtils.setField(reportService, "categoryMappingService", categoryMappingService);

        Mockito.doAnswer(invocation -> {
            Calendar time = invocation.getArgument(0);
            Tarif tarif = new Tarif(new BigDecimal("0.1"), time);

            Map<Integer, List<Tarif>> operationTarifs = Collections.singletonMap(
                PaidAction.FILL_MODEL_PARAMETER.getId(), Collections.singletonList(tarif));
            Map<CategoryOperationId, List<Tarif>> categoryOperationTarifs = Collections.emptyMap();
            return new TarifProviderImpl(time, operationTarifs, categoryOperationTarifs);
        })
            .when(tarifManager).loadTarifs(Mockito.any());

        Mockito.doReturn(Arrays.stream(PaidAction.values())
            .collect(Collectors.toMap(PaidAction::getId, Enum::toString)))
            .when(reportService).getPaidOperations();
    }

    @Test
    public void saveAndLoadExternalTariffs() {
        long id = reportService.saveExternalTariff(new ExternalTariff(null, "Операция 1", new BigDecimal("1.25")));

        List<ExternalTariff> actualExternalTariffs = reportService.loadExternalTariffs();
        Assertions.assertThat(actualExternalTariffs)
            .containsExactlyInAnyOrder(new ExternalTariff(id, "Операция 1", new BigDecimal("1.25")));
    }

    @Test
    public void deleteExternalTariff() {
        long id = reportService.saveExternalTariff(new ExternalTariff(null, "Операция 1", new BigDecimal("1.25")));
        reportService.deleteExternalTariff(id);

        List<ExternalTariff> actualExternalTariffs = reportService.loadExternalTariffs();
        Assertions.assertThat(actualExternalTariffs).isEmpty();
    }

    @Test
    public void getOperationListContainsExternalTariffWhenCategoryIsNotSelected() {
        reportService.saveExternalTariff(new ExternalTariff(null, "Внешняя операция 1", new BigDecimal("1.25")));

        Map<String, BigDecimal> allOperations = getOperationMap(reportService.getOperationList());
        Assertions.assertThat(allOperations)
            .containsEntry("Внешняя операция 1", TariffPriceConverter.toChip(new BigDecimal("1.25")));
    }

    @Test
    public void getOperationListNotContainsExternalTariffWhenCategoryIsSelected() {
        reportService.saveExternalTariff(new ExternalTariff(null, "Внешняя операция 1", new BigDecimal("1.25")));

        Map<String, BigDecimal> allOperations = getOperationMap(reportService.getOperationList(CATEGORY_HID));
        Assertions.assertThat(allOperations)
            .doesNotContainKey("Внешняя операция 1");
    }

    @Test
    public void testShowTarifWithSearchInfoMultiplicator() {
        Map<Integer, String> paidOperations = reportService.getPaidOperations();

        // mock search info
        Mockito.doReturn(Optional.of(new BigDecimal("2")))
            .when(guruCategoryService).getSearchInfoDifficulty(CATEGORY_HID);

        Map<String, BigDecimal> allOperations = getOperationMap(reportService.getOperationList(CATEGORY_HID));
        String fillModelParamTarif = paidOperations.get(PaidAction.FILL_MODEL_PARAMETER.getId());
        Assertions.assertThat(filterTarifs(allOperations, fillModelParamTarif))
            .containsExactlyEntriesOf(ImmutableMap.of(
                fillModelParamTarif, new BigDecimal("0.2")
            ));
    }

    @Test
    public void testShowTarifWithFillDifficultyMultiplicator() {
        Map<Integer, String> paidOperations = reportService.getPaidOperations();

        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(1, "", CATEGORY_HID)
            .setName("Параметр 1")
            .setFillDifficulty(new BigDecimal("3"))
            .build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(2, "", CATEGORY_HID)
            .setName("Параметр 2")
            .build());

        Map<String, BigDecimal> allOperations = getOperationMap(reportService.getOperationList(CATEGORY_HID));
        String fillModelParamTarif = paidOperations.get(PaidAction.FILL_MODEL_PARAMETER.getId());
        Assertions.assertThat(filterTarifs(allOperations, fillModelParamTarif))
            .containsExactlyEntriesOf(ImmutableMap.of(
                fillModelParamTarif + " [Параметр 1]", new BigDecimal("0.3")
            ));
    }

    @Test
    public void testShowTarifWithSearchInfoAndFillDifficultyMultiplicator() {
        Map<Integer, String> paidOperations = reportService.getPaidOperations();

        Mockito.doReturn(Optional.of(new BigDecimal("2")))
            .when(guruCategoryService).getSearchInfoDifficulty(CATEGORY_HID);

        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(1, "", CATEGORY_HID)
            .setName("Параметр 1")
            .setFillDifficulty(new BigDecimal("3"))
            .build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(2, "", CATEGORY_HID)
            .setName("Параметр 2")
            .build());

        Map<String, BigDecimal> allOperations = getOperationMap(reportService.getOperationList(CATEGORY_HID));
        String fillModelParamTarif = paidOperations.get(PaidAction.FILL_MODEL_PARAMETER.getId());
        Assertions.assertThat(filterTarifs(allOperations, fillModelParamTarif))
            .containsExactlyEntriesOf(ImmutableMap.of(
                fillModelParamTarif, new BigDecimal("0.2"),
                fillModelParamTarif + " [Параметр 1]", new BigDecimal("0.6")
            ));
    }

    @Test
    public void testFilterBySystemParameter() {
        Map<Integer, String> paidOperations = reportService.getPaidOperations();

        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(1, "", CATEGORY_HID)
            .setName("Параметр 1")
            .setFillDifficulty(new BigDecimal("3"))
            .build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(2, "", CATEGORY_HID)
            .setName("Параметр 2")
            .setFillDifficulty(new BigDecimal("3"))
            .setService(true)
            .build());

        Map<String, BigDecimal> allOperations = getOperationMap(reportService.getOperationList(CATEGORY_HID));
        String fillModelParamTarif = paidOperations.get(PaidAction.FILL_MODEL_PARAMETER.getId());
        Assertions.assertThat(filterTarifs(allOperations, fillModelParamTarif))
            .containsExactlyEntriesOf(ImmutableMap.of(
                fillModelParamTarif + " [Параметр 1]", new BigDecimal("0.3")
            ));
    }

    private static Map<String, BigDecimal> getOperationMap(List<GwtPair<String, BigDecimal>> list) {
        return list.stream()
            .collect(Collectors.toMap(GwtPair::getFirst, GwtPair::getSecond));
    }

    private Map<String, BigDecimal> filterTarifs(Map<String, BigDecimal> allOperations, String startsWith) {
        return allOperations.entrySet().stream()
            .filter(e -> e.getKey().startsWith(startsWith))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
