package ru.yandex.market.tsum.sox.deploy;

import com.google.protobuf.Timestamp;
import nanny.tickets.Releases;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NannyValidationParametersTest {

    @Test
    public void needApprovalCheck() {
        NannyValidationParameters oneResourceWithoutValidation = buildValidationParameters(Collections.singleton("SOX_RESOURCE_1"));
        NannyValidationParameters allResourcesWithValidation = buildValidationParameters(Collections.emptySet());
        NannyValidationParameters allResourcesWithoutValidation = buildValidationParameters(new HashSet<>(Arrays.asList("SOX_RESOURCE_1", "SOX_RESOURCE_2")));

        Assert.assertTrue(oneResourceWithoutValidation.needApprovalCheck());
        Assert.assertTrue(allResourcesWithValidation.needApprovalCheck());
        Assert.assertFalse(allResourcesWithoutValidation.needApprovalCheck());
    }

    private NannyValidationParameters buildValidationParameters(Set<String> resourcesWithoutApprovalCheck) {

        List<Releases.SandboxResource> resources = Arrays.asList(
            Releases.SandboxResource.newBuilder().setType("SOX_RESOURCE_1").setId("1").build(),
            Releases.SandboxResource.newBuilder().setType("SOX_RESOURCE_2").setId("2").build()
        );

        Releases.SandboxRelease release = Releases.SandboxRelease.newBuilder()
            .setTaskId("123")
            .setCreationTime(Timestamp.newBuilder().setSeconds(new Date().getTime()).build())
            .setTitle("Cool title")
            .setReleaseType(SandboxReleaseType.STABLE.getSandboxName())
            .addAllResources(resources)
            .build();

        Collection<String> services =  Arrays.asList("production_service_man", "production_service_iva");
        Set<String> soxResources = new HashSet<>(Arrays.asList("SOX_RESOURCE_1", "SOX_RESOURCE_2"));

       return new NannyValidationParameters(release, services, soxResources, resourcesWithoutApprovalCheck);
    }
}