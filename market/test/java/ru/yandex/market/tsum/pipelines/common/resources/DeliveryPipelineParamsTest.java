package ru.yandex.market.tsum.pipelines.common.resources;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 08/06/2018
 */
public class DeliveryPipelineParamsTest {

    @Test(expected = IllegalStateException.class)
    public void getArcadiaUrlException() {
        DeliveryPipelineParams params = new DeliveryPipelineParams(
            "796b1a84d23b6612dfecf7ed63919aa63d67bd55",
            "e689f41b2804229196ac19fc3a9df163848c4595",
            null
        );
        params.getArcadiaUrl();
    }

    @Test
    public void getArcadiaUrl() {
        DeliveryPipelineParams trunk = new DeliveryPipelineParams(
            "42", "21", null
        );
        Assert.assertEquals("arcadia:/arc/trunk/arcadia@42", trunk.getArcadiaUrl());

        DeliveryPipelineParams branch = new DeliveryPipelineParams(
            "42", "21", null, "market/infra/hotfix.1528282753181.5063"
        );
        Assert.assertEquals("arcadia:/arc/market/infra/hotfix.1528282753181.5063/arcadia", branch.getArcadiaUrl());
    }
}
