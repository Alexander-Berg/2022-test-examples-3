package ru.yandex.direct.internaltools.tools.canvas;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.canvas.model.OnCreativeOperationResult;
import ru.yandex.direct.canvas.tools_client.CanvasToolsClient;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.canvas.model.OnCreativeIdsOperationParameter;
import ru.yandex.direct.internaltools.tools.canvas.model.OnCreativeOperationResultWithId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OnCreativeIdsOperationsToolResultProcessingTest {
    @Autowired
    OnCreativeIdsOperationsTool onCreativeIdsOperationsTool;
    @Autowired
    CanvasToolsClient canvasToolsClient;
    private String request = "123, 456, 890";
    private Set<Long> creativeIds = Set.of(123L, 456L, 890L);

    @Before
    public void setUp() {
        Mockito.reset(canvasToolsClient);
    }

    @Test
    public void checkResultMapToList() {
        Map<Long, OnCreativeOperationResult> resultMap = Map.of(
                123L, OnCreativeOperationResult.ok(),
                456L, OnCreativeOperationResult.ok(),
                890L, OnCreativeOperationResult.ok()
        );

        OnCreativeIdsOperationParameter params = new OnCreativeIdsOperationParameter();
        params.setCreativeIds(request);
        params.setOperationName(OnCreativeIdsOperationParameter.OperationName.REBUILD);

        when(canvasToolsClient.rebuild(creativeIds)).thenReturn(resultMap);
        List<OnCreativeOperationResultWithId> actualResult = onCreativeIdsOperationsTool.getMassData(params);
        List<Long> gotIds = actualResult.stream().map(i -> i.getCreativeId()).collect(Collectors.toList());
        assertEquals(actualResult.size(), resultMap.size());
        assertTrue(gotIds.containsAll(resultMap.keySet()));
    }
}

