package ru.yandex.ir.entities;

import java.util.Arrays;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.ir.classify.om.be.RequestType;
import ru.yandex.misc.bender.Bender;

class YtOfferModelPairDtoTest {
    @Test
    void deserializationTest() {

        String classifierMagicId = "ca7ba509f82a6a676da49e016f92ca40";
        long matchedModelId = 174208554L;
        long[] candidateModelIds = {
                455047926,
                456535930
        };
        RequestType requestType = RequestType.MODEL;

        YTreeMapNode entry = new YTreeBuilder()
                .beginMap()
                .key("classifier_magic_id").value(classifierMagicId)
                .key("matched_model_id").value(matchedModelId)
                .key("candidate_model_ids").value(candidateModelIds)
                .key("target_type").value(requestType.name())
                .buildMap();

        String json = YtUtils.yson2json(JsonNodeFactory.instance, entry).toString();

        Assert.assertEquals("Correct JSON generated", "{\"matched_model_id\":174208554," +
                "\"candidate_model_ids\":[455047926,456535930],\"target_type\":\"MODEL\"," +
                "\"classifier_magic_id\":\"ca7ba509f82a6a676da49e016f92ca40\"}", json);

        YtOfferModelPairDto ytOfferModelPairDto = Bender.jsonParser(YtOfferModelPairDto.class).parseJson(json);
        Assert.assertEquals("Default string value for absent key mapped", "", ytOfferModelPairDto.getDescription());
        Assert.assertEquals("Default number value for absent key mapped", 0L,
                (long) ytOfferModelPairDto.getCategoryId());
        Assert.assertEquals("Default array value for absent key mapped", 0,
                ytOfferModelPairDto.getSignaturesV6().size());
        Assert.assertEquals("Correct string mapped", classifierMagicId,
                ytOfferModelPairDto.getClassifierMagicId());
        Assert.assertEquals("Correct number mapped", matchedModelId,
                (long) ytOfferModelPairDto.getMatchedModelId());
        Assert.assertTrue("Correct array mapped", Arrays.equals(candidateModelIds,
                ytOfferModelPairDto.getCandidateModelIds().stream().mapToLong(Long::longValue).toArray()));
        Assert.assertEquals("Correct enum mapped", requestType, ytOfferModelPairDto.getTargetType());
    }
}
