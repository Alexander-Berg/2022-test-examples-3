package ru.yandex.direct.core.entity.brandlift.repository;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BrandSurveyRepositoryTest {

    private static final CompareStrategy STRATEGY = allFields();

    @Autowired
    private BrandSurveyRepository brandSurveyRepository;

    private int shard = 1;

    @Test
    public void add_OneBrandSurvey_DataIsSavedCorrectly() {
        var brandSurvey = new BrandSurvey()
                .withClientId(123L)
                .withRetargetingConditionId(65432L)
                .withBrandSurveyId("qwertyu12345")
                .withName("Brand-lift 123")
                .withIsBrandLiftHidden(false)
                .withExperimentId(1L)
                .withSegmentId(1L);

        brandSurveyRepository.addBrandSurvey(shard, brandSurvey);

        var result = brandSurveyRepository.getBrandSurvey(shard, brandSurvey.getBrandSurveyId()).get(0);


        assertThat("сохраненные данные brand-lift соответствуют ожидаемым",
                result, beanDiffer(brandSurvey).useCompareStrategy(STRATEGY));
    }

    @Test
    public void rename_OneBrandSurvey_renameCorrect() {
        var newName = "Brand-lift 123456";
        var brandSurvey = new BrandSurvey()
                .withClientId(345L)
                .withRetargetingConditionId(65432L)
                .withBrandSurveyId("qasdfg754")
                .withName("Old-name 12345")
                .withIsBrandLiftHidden(false)
                .withExperimentId(1L)
                .withSegmentId(1L);

        brandSurveyRepository.addBrandSurvey(shard, brandSurvey);

        brandSurveyRepository.renameBrandSurvey(shard, brandSurvey.getBrandSurveyId(), newName);

        var result = brandSurveyRepository.getBrandSurvey(shard, brandSurvey.getBrandSurveyId()).get(0);

        brandSurvey.setName(newName);
        assertThat("сохраненные данные brand-lift соответствуют ожидаемым",
                result, beanDiffer(brandSurvey).useCompareStrategy(STRATEGY));
    }

    @Test
    public void get_ClientBrandSurveys_getCorrect() {
        long clientId = 123L;
        List<BrandSurvey> goodBrandSurveys = List.of(
                new BrandSurvey()
                        .withClientId(clientId)
                        .withRetargetingConditionId(65432L)
                        .withBrandSurveyId("1qwerty")
                        .withName("Brand-lift 123")
                        .withIsBrandLiftHidden(false)
                        .withExperimentId(1L)
                        .withSegmentId(1L),
                new BrandSurvey()
                        .withClientId(clientId)
                        .withRetargetingConditionId(65432L)
                        .withBrandSurveyId("2qwerty")
                        .withName("Brand-lift 123")
                        .withIsBrandLiftHidden(false)
                        .withExperimentId(1L)
                        .withSegmentId(1L)
        );

        var thirdBrandSurvey = new BrandSurvey()
                .withClientId(987L)
                .withRetargetingConditionId(65432L)
                .withBrandSurveyId("qwerty88678")
                .withName("Brand-lift 123")
                .withIsBrandLiftHidden(false)
                .withExperimentId(1L)
                .withSegmentId(1L);


        brandSurveyRepository.addBrandSurvey(shard, goodBrandSurveys.get(0));
        brandSurveyRepository.addBrandSurvey(shard, goodBrandSurveys.get(1));
        brandSurveyRepository.addBrandSurvey(shard, thirdBrandSurvey);

        var result = brandSurveyRepository.getClientBrandSurveys(shard, clientId);

        assertThat(result.size(), is(2));

        assertThat("Вернулись правильные опросы", result, beanDiffer(goodBrandSurveys).useCompareStrategy(STRATEGY));
    }
}
