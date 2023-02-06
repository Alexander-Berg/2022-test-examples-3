package ru.yandex.market.ir.ui.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import ru.yandex.market.ir.http.Classifier;
import ru.yandex.market.ir.http.ClassifierService;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.shared.models.robot.data.ClassifierRequest;
import ru.yandex.market.robot.shared.models.robot.data.ProbableCategory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ClassifierServiceProxyTest {
    private static final int CATEGORY_ID = 23840;
    private static final String CATEGORY_NAME = "Какая-нибудь категория";

    private ClassifierService classifierService;
    private ClassifierService trainerClassifierService;
    private CategoryParametersService categoryParametersService;
    private ClassifierServiceProxy classifierServiceProxy;

    private final ProbableCategory.ProbableCategoryType type;

    public ClassifierServiceProxyTest(ProbableCategory.ProbableCategoryType type) {
        this.type = type;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][]{
                {ProbableCategory.ProbableCategoryType.MARKET},
                {ProbableCategory.ProbableCategoryType.USED_BY_TRAINER}
            }
        );
    }

    @Before
    public void setup() {
        classifierService = Mockito.mock(ClassifierService.class);
        mockResponse(classifierService);
        trainerClassifierService = Mockito.mock(ClassifierService.class);
        mockResponse(trainerClassifierService);
        categoryParametersService = Mockito.mock(CategoryParametersService.class);
        CategoryInfoProviderService categoryInfoProviderService = new CategoryInfoProviderService(categoryParametersService);
        Mockito.when(categoryParametersService.getParameters(Mockito.any()))
            .thenReturn(MboParameters.GetCategoryParametersResponse.newBuilder()
                .setCategoryParameters(MboParameters.Category.newBuilder()
                    .addName(MboParameters.Word.newBuilder().setName(CATEGORY_NAME).build())
                    .build())
                .build());
        classifierServiceProxy = new ClassifierServiceProxy(
            classifierService, trainerClassifierService, categoryInfoProviderService);
    }

    @Test
    public void testAllNull() {
        ClassifierRequest request = new ClassifierRequest();
        request.setType(type);
        List<ProbableCategory> classificationResult = classifierServiceProxy.classify(request);
        verifyClassifierServiceCalled(request);
        verifyCategoryParametersServiceCalled();
        ProbableCategory resultProbableCategory = classificationResult.get(0);
        Assert.assertEquals(CATEGORY_ID, resultProbableCategory.getCategoryId());
        Assert.assertEquals(CATEGORY_NAME, resultProbableCategory.getName());
    }

    @Test
    public void testOnlyTitle() {
        ClassifierRequest request = new ClassifierRequest();
        request.setType(type);
        request.setText("Some title");
        List<ProbableCategory> classificationResult = classifierServiceProxy.classify(request);
        verifyClassifierServiceCalled(request);
        verifyCategoryParametersServiceCalled();
        ProbableCategory resultProbableCategory = classificationResult.get(0);
        Assert.assertEquals(CATEGORY_ID, resultProbableCategory.getCategoryId());
        Assert.assertEquals(CATEGORY_NAME, resultProbableCategory.getName());
    }

    private void mockResponse(ClassifierService classifierService) {
        Classifier.ClassifiedOffer offer = Classifier.ClassifiedOffer.newBuilder()
            .addCategory(Classifier.ProbableCategory.newBuilder().setCategoryId(CATEGORY_ID).build())
            .build();
        Mockito.when(classifierService.classify(Mockito.any()))
            .thenReturn(Classifier.ClassificationResponse.newBuilder()
                .addOffer(offer)
                .build());
    }

    private void verifyClassifierServiceCalled(ClassifierRequest request) {
        Classifier.ClassificationRequest classificationRequest = classifierServiceProxy
            .buildClassificationRequest(request);
        if (type == ProbableCategory.ProbableCategoryType.MARKET) {
            Mockito.verify(classifierService).classify(classificationRequest);
        } else {
            Mockito.verify(trainerClassifierService).classify(classificationRequest);
        }
    }

    private void verifyCategoryParametersServiceCalled() {
        Mockito.verify(categoryParametersService).getParameters(
            MboParameters.GetCategoryParametersRequest.newBuilder()
                .setCategoryId(CATEGORY_ID)
                .setTimestamp(-1L)
                .build()
        );
    }
}
