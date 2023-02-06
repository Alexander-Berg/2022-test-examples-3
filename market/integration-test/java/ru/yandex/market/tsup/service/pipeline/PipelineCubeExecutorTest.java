package ru.yandex.market.tsup.service.pipeline;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.PipelineCubeExecutor;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeStatus;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineNameImpl;
import ru.yandex.market.tsup.repository.mappers.PipelineCubeMapper;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SampleInputCube;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SamplePartnerData;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SamplePartnerOutputCube;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SamplePipeline;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SamplePointData;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SamplePointOutputCube;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SampleTransportData;
import ru.yandex.market.tsup.service.pipeline.sample_classes.SampleTransportationData;
import ru.yandex.market.tsup.service.pipeline.sample_classes.enum_data.EnumPipeline;
import ru.yandex.market.tsup.service.pipeline.sample_classes.enum_data.SampleEnumCube;
import ru.yandex.market.tsup.service.pipeline.sample_classes.enum_data.SampleEnumInputData;
import ru.yandex.market.tsup.service.provider.PipelineCubeProvider;
import ru.yandex.market.tsup.service.provider.PipelineProvider;

public class PipelineCubeExecutorTest extends AbstractContextualTest {
    @Autowired
    private PipelineCubeExecutor cubeExecutor;

    @Autowired
    private PipelineCubeMapper cubeMapper;

    @Autowired
    private PipelineCubeProvider cubeProvider;

    @Autowired
    private PipelineProvider pipelineProvider;

    private final SamplePipeline samplePipeline = new SamplePipeline();
    private final SamplePointOutputCube samplePointOutputCube = Mockito.spy(new SamplePointOutputCube());
    private final SampleInputCube sampleInputCube = Mockito.spy(new SampleInputCube());
    private final SamplePartnerOutputCube samplePartnerOutputCube = Mockito.spy(new SamplePartnerOutputCube());

    @BeforeEach
    void init() {
        Mockito.doReturn(samplePipeline).when(pipelineProvider).provide(PipelineNameImpl.QUICK_TRIP_CREATOR);
        Mockito.doReturn(samplePartnerOutputCube).when(cubeProvider)
            .provide(PipelineCubeName.CARRIER_TRANSPORT_CREATOR);
        Mockito.doReturn(sampleInputCube).when(cubeProvider).provide(PipelineCubeName.ROUTE_SCHEDULE_CREATOR);
        Mockito.doReturn(samplePointOutputCube).when(cubeProvider).provide(PipelineCubeName.CARRIER_COURIER_CREATOR);
    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/for_sample_runner_all_data.xml")
    @ExpectedDatabase(
        value = "/repository/pipeline/sample/after_sample_runner_all_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testMapping() {
        ArgumentCaptor<SampleTransportationData> captor = ArgumentCaptor.forClass(SampleTransportationData.class);
        cubeExecutor.run(3L);
        Mockito.verify(sampleInputCube).execute(captor.capture());

        SampleTransportationData realData = captor.getValue();
        SampleTransportationData expectedData = new SampleTransportationData()
            .setPartnerData(new SamplePartnerData().setId(1L))
            .setPointData(new SamplePointData(123L))
            .setTransportData(new SampleTransportData(666L));

        softly.assertThat(realData).isEqualTo(expectedData);
        softly.assertThat(cubeMapper.getById(3L).getStatus()).isEqualTo(PipelineCubeStatus.FINISHED);
    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/for_sample_runner_not_enough_data.xml")
    void notEnoughDataFail() {
        softly.assertThatThrownBy(() -> cubeExecutor.run(3L));
    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/for_sample_runner_all_data.xml")
    void testWrongStatus() {
        cubeExecutor.run(1L);
        Mockito.verify(sampleInputCube, Mockito.times(0)).execute(Mockito.any());
    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/sample_runner_one_to_one.xml")
    void oneToOneMapping() {
        ArgumentCaptor<SamplePointData> captor = ArgumentCaptor.forClass(SamplePointData.class);
        cubeExecutor.run(2L);
        Mockito.verify(samplePointOutputCube).execute(captor.capture());

        SamplePointData realData = captor.getValue();

        softly.assertThat(realData).isEqualTo(new SamplePointData(123L));

    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/sample_runner_one_to_one_null_result.xml")
    void testValidation() {
        softly.assertThatThrownBy(() -> cubeExecutor.run(2L));
    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/sample_runner_enum.xml")
    void checkEnumIsMapped() {
        EnumPipeline pipeline = new EnumPipeline();
        SampleEnumCube cube = Mockito.spy(new SampleEnumCube());

        Mockito.doReturn(pipeline).when(pipelineProvider).provide(PipelineNameImpl.QUICK_TRIP_CREATOR);
        Mockito.doReturn(cube).when(cubeProvider).provide(PipelineCubeName.ROUTE_SCHEDULE_CREATOR);

        cubeExecutor.run(1L);

        ArgumentCaptor<SampleEnumInputData> inputCaptor = ArgumentCaptor.forClass(SampleEnumInputData.class);

        Mockito.verify(cube).execute(inputCaptor.capture());

        softly.assertThat(inputCaptor.getValue()).isEqualTo(new SampleEnumInputData(TransportationType.XDOC_TRANSPORT,
            null));
    }

    @Test
    @DatabaseSetup("/repository/pipeline/sample/sample_runner_empty_field.xml")
    void checkEmptyFieldIsMapped() {
        EnumPipeline pipeline = new EnumPipeline();
        SampleEnumCube cube = Mockito.spy(new SampleEnumCube());

        Mockito.doReturn(pipeline).when(pipelineProvider).provide(PipelineNameImpl.QUICK_TRIP_CREATOR);
        Mockito.doReturn(cube).when(cubeProvider).provide(PipelineCubeName.ROUTE_SCHEDULE_CREATOR);

        cubeExecutor.run(1L);

        ArgumentCaptor<SampleEnumInputData> inputCaptor = ArgumentCaptor.forClass(SampleEnumInputData.class);

        Mockito.verify(cube).execute(inputCaptor.capture());

        softly.assertThat(inputCaptor.getValue()).isEqualTo(
            new SampleEnumInputData(TransportationType.XDOC_TRANSPORT, null)
        );
    }
}
