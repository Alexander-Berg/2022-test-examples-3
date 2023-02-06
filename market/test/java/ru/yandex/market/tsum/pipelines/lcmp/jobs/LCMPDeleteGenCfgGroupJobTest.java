package ru.yandex.market.tsum.pipelines.lcmp.jobs;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;

public class LCMPDeleteGenCfgGroupJobTest {
    @Spy
    private ComponentChangeRequest componentChangeRequest = new ComponentChangeRequest();

    @Mock
    private NannyClient nannyClient;

    @InjectMocks
    private LCMPDeleteGenCgfGroupJob sut;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getGroupsToDeleteTest() {
        ComponentSpecResource previousComponent = TestComponentSpecGenerator.generate(
            List.of("n1", "n2", "n3"), List.of("n5", "n6"));
        componentChangeRequest.setPreviousComponentSpecResource(previousComponent);

        ComponentSpecResource targetComponent = TestComponentSpecGenerator.generate(
            List.of("n1", "n2"), List.of("n5"));
        componentChangeRequest.setTargetComponentSpecResource(targetComponent);
        Mockito.doReturn(List.of()).when(nannyClient).getServicesUsingGencfgGroup("n3");
        Mockito.doReturn(List.of("SERVICE_USING")).when(nannyClient).getServicesUsingGencfgGroup("n6");

        List<String> groupsToDelete = sut.getGroupsToDelete();
        Assert.assertEquals(List.of("n3"), groupsToDelete);
    }
}
