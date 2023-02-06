package ru.yandex.market.pers.qa.mock;

import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Vendor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public final class ReportServiceMockUtils {
    private ReportServiceMockUtils() {
    }

    /**
     * Мока reportService, возвращающая рандомную модель по любому idшнику
     */
    public static void mockReportService(ReportService reportService) {
        when(reportService.getModelsByIds(anyList())).then(invocation -> {
            List<Long> modelIds = invocation.getArgument(0);
            return modelIds.stream()
                .collect(Collectors.toMap(Function.identity(),
                    ReportServiceMockUtils::generateModel));
        });
    }


    /**
     * мокируем ru.yandex.market.report.ReportService#getModelsByIds(java.util.List) согласно:
     *
     * @param reportService
     * @param modelWithCategory - модель с категорией
     * @param modelWithoutCategory - модель без категории
     * @param category - категория для модели modelWithCategory
     */
    public static void mockReportServiceByModelIdCategories(ReportService reportService,
                                                            long modelWithCategory,
                                                            long modelWithoutCategory,
                                                            Category category) {
        when(reportService.getModelsByIds(anyList())).then(invocation -> {
            List<Long> modelIds = invocation.getArgument(0);
            return modelIds.stream()
                .filter(id -> id == modelWithCategory || id == modelWithoutCategory)
                .collect(Collectors.toMap(Function.identity(),
                    (modelId) -> {
                        if (modelId == modelWithCategory) {
                            Model model = generateModel(modelId);
                            model.setCategory(category);
                            return model;
                        } else {
                            return generateModel(modelId);
                        }
                    }));
        });
    }

    /**
     * мокируем ru.yandex.market.report.ReportService#getModelsByIds(java.util.List) согласно:
     *
     * @param reportService
     * @param modelVendor - модель с вендором
     * @param modelNoVendor - модель без вендора
     * @param modelVendorWithTransition - модель c вендором и переездом на (modelVendorWithTransition + 1)
     * @param vendor - категория для модели modelVendor
     */
    public static void mockReportServiceByModelIdVendor(ReportService reportService,
                                                        long modelVendor,
                                                        long modelNoVendor,
                                                        long modelVendorWithTransition,
                                                        Vendor vendor) {
        when(reportService.getModelsByIds(anyList())).then(invocation -> {
            List<Long> modelIds = invocation.getArgument(0);
            return modelIds.stream()
                .filter(id -> id == modelVendor || id == modelNoVendor || id == modelVendorWithTransition)
                .map((modelId) -> {
                    if (modelId == modelVendor) {
                        Model model = generateModel(modelId);
                        model.setVendor(vendor);
                        return model;
                    } else if (modelId == modelVendorWithTransition) {
                        Model model = generateModel(modelId + 1);
                        model.setVendor(vendor);
                        return model;
                    } else {
                        return generateModel(modelId);
                    }
                })
                .collect(Collectors.toMap(Model::getId, Function.identity()));
        });
    }


    /**
     * мокируем ru.yandex.market.report.ReportService#getModelsByIds(java.util.List) согласно:
     *
     * @param module1 - если modelId % module1 == 0 то моделька не отдаётся репортом
     * @param module2 - если modelId % module2 == 0 то моделька отдаётся репортом, но без имени и картинки
     */
    public static void mockReportServiceByModelIdModules(ReportService reportService, int module1, int module2) {
        when(reportService.getModelsByIds(anyList())).then(invocation -> {
            List<Long> modelIds = invocation.getArgument(0);
            return modelIds.stream()
                .filter(modelId -> modelId % module1 != 0)
                .collect(Collectors.toMap(Function.identity(),
                    (modelId) -> generateModelByModelId(modelId, module2)));
        });
    }

    /**
     * мокируем ru.yandex.market.report.ReportService#getModelById(long), чтобы одавалась нужная категория
     *
     * @param reportService что мокируем
     * @param modelId для какой модельки
     * @param category отдаваемая категория
     */
    public static void mockReportServiceCategoryInGetModelByModelId(ReportService reportService, long modelId,
                                                                    Category category) {
        when(reportService.getModelById(eq(modelId))).then(invocation -> {
            Model model = new Model();
            model.setCategory(category);
            return Optional.of(model);
        });
    }

    public static void mockReportPrime(ReportService reportService, long hid, long targetModelId) {
        when(reportService.getModelsPrime(eq(hid), anyLong()))
            .thenReturn(Collections.singletonList(generateModel(targetModelId)));
    }

    /**
     * @param module2 если modelId % module2 == 0 то моделька отдаётся репортом, но без имени и картинки
     */
    private static Model generateModelByModelId(long modelId, int module2) {
        if (modelId % module2 == 0) {
            return new Model();
        } else {
            return generateModel(modelId);
        }
    }

    /**
     * генерируем случайную модель по modelId
     *
     * @param modelId
     * @return
     */
    private static Model generateModel(long modelId) {
        Model model = new Model();
        model.setId(modelId);
        model.setPictureUrl("yandex.ru");
        model.setName("model" + modelId);
        return model;
    }


}
