package ru.yandex.market.tsum.pipelines.telephony.jobs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipelines.arcadia.resources.NewAndPreviousArcadiaRefs;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobCustomFields;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateSandboxCheckoutConfigFromArcadiaRefTest {

    @InjectMocks
    private CreateSandboxCheckoutConfigFromArcadiaRefJob job = new CreateSandboxCheckoutConfigFromArcadiaRefJob();

    @Mock
    NewAndPreviousArcadiaRefs refs;

    @Mock
    ArcadiaRef ref;

    @Mock
    private JobContext context;
    @Mock
    private ResourcesJobContext resourcesContext;


    @Before
    public void setUp() {
        when(context.resources()).thenReturn(resourcesContext);
        when(refs.getNewArcadiaRef()).thenReturn(ref);
    }

    @Test
    public void shouldProduceSandboxCustomField() throws Exception {
        job.execute(context);
        verify(resourcesContext).produce(any(SandboxTaskJobCustomFields.class));
    }

}
