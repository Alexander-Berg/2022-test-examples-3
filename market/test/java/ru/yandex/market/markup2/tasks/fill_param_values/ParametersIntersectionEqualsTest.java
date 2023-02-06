package ru.yandex.market.markup2.tasks.fill_param_values;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 10.07.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ParametersIntersectionEqualsTest {

    private static final long MODEL_ID = 1L;
    private static final Set<Long> PARAM_IDS = ImmutableSet.of(1L, 2L, 3L);

    @Test
    public void sameModelWithParametersIntersection() {
        Set<Long> secondParamIds = ImmutableSet.of(1L, 4L, 5L);
        FillParamValuesIdentity firstIdentity = new FillParamValuesIdentity(MODEL_ID, PARAM_IDS);
        FillParamValuesIdentity secondIdentity = new FillParamValuesIdentity(MODEL_ID, secondParamIds);

        Assert.assertEquals(firstIdentity.hashCode(), secondIdentity.hashCode());
        Assert.assertEquals(firstIdentity, secondIdentity);
    }

    @Test
    public void sameModelWithoutParametersIntersection() {
        Set<Long> secondParamIds = ImmutableSet.of(4L, 5L);
        FillParamValuesIdentity firstIdentity = new FillParamValuesIdentity(MODEL_ID, PARAM_IDS);
        FillParamValuesIdentity secondIdentity = new FillParamValuesIdentity(MODEL_ID, secondParamIds);

        Assert.assertEquals(firstIdentity.hashCode(), secondIdentity.hashCode());
        Assert.assertThat(firstIdentity, not(equalTo(secondIdentity)));
    }

    @Test
    public void differentModelsWithParametersIntersection() {
        long secondModelId = 2L;
        Set<Long> secondParamIds = ImmutableSet.of(3L, 4L, 5L);
        FillParamValuesIdentity firstIdentity = new FillParamValuesIdentity(MODEL_ID, PARAM_IDS);
        FillParamValuesIdentity secondIdentity = new FillParamValuesIdentity(secondModelId, secondParamIds);

        Assert.assertThat(firstIdentity.hashCode(), not(equalTo(secondIdentity.hashCode())));
        Assert.assertThat(firstIdentity, not(equalTo(secondIdentity)));
    }
}
