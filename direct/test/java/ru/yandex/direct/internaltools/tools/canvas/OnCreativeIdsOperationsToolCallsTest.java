package ru.yandex.direct.internaltools.tools.canvas;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.canvas.tools_client.CanvasToolsClient;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.canvas.model.OnCreativeIdsOperationParameter;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OnCreativeIdsOperationsToolCallsTest {
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
    public void checkCalling() {
        OnCreativeIdsOperationParameter params = new OnCreativeIdsOperationParameter();
        params.setCreativeIds(request);


        for (OnCreativeIdsOperationParameter.OperationName operationName :
                OnCreativeIdsOperationParameter.OperationName.values()) {
            params.setOperationName(operationName);
            onCreativeIdsOperationsTool.process(params);
        }

        InOrder inOrder = inOrder(canvasToolsClient);
        inOrder.verify(canvasToolsClient).reshootScreenshot(creativeIds);
        inOrder.verify(canvasToolsClient).rebuild(creativeIds);
        inOrder.verify(canvasToolsClient).sendToDirect(creativeIds);
        inOrder.verify(canvasToolsClient).sendToRtbHost(creativeIds);
        verifyNoMoreInteractions(canvasToolsClient);
    }
}

