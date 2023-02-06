package ru.yandex.mbo.tool.jira.MBO11214;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 25.04.2017
 */
public class PublishModelsToolTest {

    private final Set<Long> requiredParametersIds = ImmutableSet.of(1L, 2L);

    @Test
    public void noRequiredParams() {
        ModelStorage.Model model = model().build();
        Assert.assertFalse(PublishModelsTool.canPublish(model, requiredParametersIds));
    }

    @Test
    public void hasRequiredParams() {
        ModelStorage.Model model = model()
            .addAllParameterValues(values(requiredParametersIds))
            .build();
        Assert.assertTrue(PublishModelsTool.canPublish(model, requiredParametersIds));
    }

    @Test
    public void notAllRequiredParams() {
        Set<Long> paramIds = ImmutableSet.of(requiredParametersIds.iterator().next());
        ModelStorage.Model model = model()
            .addAllParameterValues(values(paramIds))
            .build();
        Assert.assertFalse(PublishModelsTool.canPublish(model, requiredParametersIds));
    }

    @Test
    public void emptyModel() {
        ModelStorage.Model model = model().build();
        Assert.assertFalse(PublishModelsTool.canPublish(model, requiredParametersIds));
    }

    private static ModelStorage.Model.Builder model() {
        return ModelStorage.Model.newBuilder().setId(1).setPublished(false);
    }

    private static List<ModelStorage.ParameterValue> values(Collection<Long> paramIds) {
        List<ModelStorage.ParameterValue> result = new ArrayList<>();
        for (Long paramId : paramIds) {
            result.add(ModelStorage.ParameterValue.newBuilder()
                .setParamId(paramId)
                .build());
        }
        return result;
    }
}
