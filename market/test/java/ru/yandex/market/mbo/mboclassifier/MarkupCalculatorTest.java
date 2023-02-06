package ru.yandex.market.mbo.mboclassifier;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author commince
 * <p>
 * Кейсы перекладываний для удобства описания буду обозначать так
 * -> CATEGORY_ID_1(IS_TRAIN_SAMPLE_1)
 * -> ...
 * -> CATEGORY_ID_N(IS_TRAIN_SAMPLE_N)
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MarkupCalculatorTest {
    private long currentOperatorId = 0L; // счетчик для удобной генерации уидов юзеров

    @Test
    public void calculate21madeByManagers2() {
        /*
         * Три перекладывания
         * -> 2(+)
         * -> 1(+)**Сделано менеджером**
         * -> 2(-)
         *
         * Результат:
         * 1(+)
         */
        OffersStorage.OfferMarkup markup = calculateMarkup(
            buildCategoryChange(2L, true),
            buildCategoryChange(1L, true, true),
            buildCategoryChange(2L, false)
        );

        Assert.assertEquals(1L, markup.getResultCategoryId());
        Assert.assertTrue(markup.getIsTrainSample());
    }

    @Test
    public void calculate212() {
        /*
         * Три перекладывания
         * -> 2(+)
         * -> 1(+)
         * -> 2(-)
         *
         * Результат:
         * 2(+)
         */
        OffersStorage.OfferMarkup markup = calculateMarkup(
            buildCategoryChange(2L, true),
            buildCategoryChange(1L, true),
            buildCategoryChange(2L, false)
        );

        Assert.assertEquals(2L, markup.getResultCategoryId());
        Assert.assertTrue(markup.getIsTrainSample());
    }

    @Test
    public void calculateMarkup121() {
        /*
         * Три перекладывания
         * -> 1(+)
         * -> 2(-)
         * -> 1(+)
         *
         * Результат:
         * 1(+)
         */
        OffersStorage.OfferMarkup markup = calculateMarkup(
            buildCategoryChange(1L, true),
            buildCategoryChange(2L, false),
            buildCategoryChange(1L, true)
        );

        Assert.assertEquals(1L, markup.getResultCategoryId());
        Assert.assertTrue(markup.getIsTrainSample());
    }

    private OffersStorage.OfferMarkup calculateMarkup(OffersStorage.CategoryChange... changes) {
        List<OffersStorage.CategoryChange> list = Arrays.asList(changes);
        return MarkupCalculator.calculateMarkup(list);
    }

    private OffersStorage.CategoryChange buildCategoryChange(long categoryId, boolean isTrainSample,
                                                             boolean isManager) {
        OffersStorage.CategoryChange.Builder categoryChangeBuilder = OffersStorage.CategoryChange.newBuilder();
        categoryChangeBuilder.setOperatorUid(currentOperatorId++);
        categoryChangeBuilder.setOperatorCategoryId(categoryId);
        categoryChangeBuilder.setForceApproved(isManager);
        categoryChangeBuilder.setModificationDate(new Date().getTime());
        categoryChangeBuilder.setAutoApproved(false);
        categoryChangeBuilder.setTrainSample(isTrainSample);
        categoryChangeBuilder.setTaskType("RARE");
        return categoryChangeBuilder.build();
    }

    private OffersStorage.CategoryChange buildCategoryChange(long categoryId, boolean isTrainSample) {
        return buildCategoryChange(categoryId, isTrainSample, false);
    }
}
