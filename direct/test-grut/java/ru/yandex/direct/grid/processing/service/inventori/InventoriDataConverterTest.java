package ru.yandex.direct.grid.processing.service.inventori;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest;
import ru.yandex.direct.grid.processing.model.inventori.GdImpressionLimit;
import ru.yandex.direct.grid.processing.model.inventori.GdIncomeGrade;
import ru.yandex.direct.grid.processing.model.inventori.GdInventoriStrategyType;
import ru.yandex.direct.grid.processing.model.inventori.GdUacRecommendationRequest;
import ru.yandex.direct.grid.processing.model.inventori.GdUacStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.Brandsafety;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class InventoriDataConverterTest {

    private static final List<Long> GOALS_IDS = List.of(4294967297L, 4294967298L, 4294967299L, 4294967300L, 4294967301L, 4294967304L, 4294967312L, 4294967303L);
    @Autowired
    InventoriDataConverter inventoriDataConverter;

    @Autowired
    Steps steps;

    @Autowired
    CryptaSegmentRepository cryptaSegmentRepository;

    @Mock
    InventoriDataConverter inventoriDataConverterSpy;

    GdUacRecommendationRequest gdUacRecommendationRequest;

    @Before
    public void before() {
        gdUacRecommendationRequest = new GdUacRecommendationRequest();
        MockitoAnnotations.openMocks(this);
        inventoriDataConverterSpy = spy(inventoriDataConverter);
        doReturn(null)
                .when(inventoriDataConverterSpy).getConditions(any());
        gdUacRecommendationRequest = new GdUacRecommendationRequest();
        gdUacRecommendationRequest.withIncomeGradeLower(GdIncomeGrade.LOW);
        gdUacRecommendationRequest.withIncomeGradeUpper(GdIncomeGrade.PREMIUM);
        gdUacRecommendationRequest.withStrategy(new GdUacStrategy()
                .withStartDate(LocalDate.now())
                .withFinishDate(LocalDate.now())
                .withImpressionLimit(new GdImpressionLimit().withDays(10L)));
        gdUacRecommendationRequest.setStrategyName(GdInventoriStrategyType.AUTOBUDGET_AVG_CPV);
    }

    private List<String> getGoalsKeyWordValues() {
        var idToGoal = cryptaSegmentRepository.getBrandSafety();
        List<String> result = new ArrayList<>();
        var idToAdd = GOALS_IDS.stream().filter(id -> !idToGoal.containsKey(id)).collect(Collectors.toList());
        result.addAll(GOALS_IDS.stream().filter(idToGoal::containsKey).map(id -> idToGoal.get(id).getKeywordValue()).collect(Collectors.toList()));
        Long maxKeywordValue = idToGoal.isEmpty() ? 0 : Collections.max(idToGoal.values().stream().map(goal -> Long.parseLong(goal.getKeywordValue())).collect(Collectors.toList())) + 1;
        for (var i: idToAdd) {
            result.add(maxKeywordValue.toString());
            Goal goal = (Goal) (new Goal().withType(GoalType.BRANDSAFETY)
                    .withId(i)
                    .withKeyword("brand-safety-categories")
                    .withKeywordValue((maxKeywordValue++).toString()));
            steps.cryptaGoalsSteps().addGoals(goal);
        }
        return result;
    }

    @Test
    public void testWithExcludedBsCategories() {
        var excpected = getGoalsKeyWordValues().stream().map(id -> "brand-safety-categories:" + id).collect(Collectors.toList());
        gdUacRecommendationRequest.withBrandSafety(new Brandsafety().setEnabled(true).setAdditionalCategories(Collections.singletonList(4294967303L)));
        assertThat(inventoriDataConverterSpy.convertUacRecommendationRequest(gdUacRecommendationRequest).getExcludedBsCategories(),
                containsInAnyOrder(excpected.toArray()));
    }

    @Test
    public void testWithoutExcludedBsCategoriesBecauseItNull() {
        assertNull(inventoriDataConverterSpy.convertUacRecommendationRequest(gdUacRecommendationRequest).getExcludedBsCategories());
    }

    @Test
    public void testWithoutExcludedBsCategoriesBecauseEnabled() {
        gdUacRecommendationRequest.withBrandSafety(new Brandsafety().setEnabled(false).setAdditionalCategories(emptyList()));
        assertNull(inventoriDataConverterSpy.convertUacRecommendationRequest(gdUacRecommendationRequest).getExcludedBsCategories());
    }
}

