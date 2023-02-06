package ru.yandex.market.tsum.pipelines.sre.jobs.dbaas;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.UserOuterClass;

public class PostgreCreateDbJobTest {

    @Test
    public void noLogPasswordTest() {
        String pass = RandomStringUtils.randomAlphanumeric(32);

        UserOuterClass.UserSpec.Builder userSpec = UserOuterClass.UserSpec.newBuilder().setPassword(pass);
        ClusterServiceOuterClass.CreateClusterRequest createClusterRequest = ClusterServiceOuterClass
            .CreateClusterRequest.newBuilder()
            .addUserSpecs(userSpec)
            .build();

        Assert.assertTrue(createClusterRequest.toString().contains(pass));
        Assert.assertFalse(createClusterRequest.toString().replace(pass, "**********").contains(pass));
    }
}
