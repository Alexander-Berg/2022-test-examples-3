package ru.yandex.market.mboc.tms.executors;

import java.io.IOException;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.mdm.tms.executors.UpdateCargoTypesExecutor;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.services.cargotype.DownloadCargoTypeServiceMock;
import ru.yandex.market.mboc.common.utils.WordProtoUtil;

@SuppressWarnings("checkstyle:magicnumber")
public class UpdateCargoTypesExecutorTest extends MdmBaseDbTestClass {

    private UpdateCargoTypesExecutor updateCargoTypesExecutor;

    @Autowired
    private CargoTypeRepository cargoTypeRepository;

    @Autowired
    private TransactionHelper transactionHelper;

    private DownloadCargoTypeServiceMock cargoTypeServiceMock;
    private CategoryParametersService categoryParametersService;

    @Before
    public void before() {
        cargoTypeServiceMock = new DownloadCargoTypeServiceMock();
        categoryParametersService = Mockito.mock(CategoryParametersService.class);
        this.updateCargoTypesExecutor = new UpdateCargoTypesExecutor(
            cargoTypeServiceMock,
            cargoTypeRepository,
            transactionHelper,
            categoryParametersService
        );
        cargoTypeRepository.deleteAll();
    }

    @Test
    public void whenFindsDuplicateInPostgresShouldFail() throws IOException {
        cargoTypeRepository.insertBatch(Arrays.asList(
            new CargoType(1, "2", null),
            new CargoType(2, "2", null)
        ));

        cargoTypeServiceMock.setCargoTypes();

        Assertions.assertThatThrownBy(() -> {
            updateCargoTypesExecutor.execute();
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageMatching(".*objects share the same field.*");
    }

    @Test
    public void whenFindsDuplicatesInReceivedJsonShouldThrow() throws IOException {
        cargoTypeServiceMock.setCargoTypes(
            new CargoType(1, "2", null),
            new CargoType(2, "2", null)
        );

        Assertions.assertThatThrownBy(() -> updateCargoTypesExecutor.execute())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageMatching(".*objects share the same field.*");
    }

    @Test
    public void whenFindsOneToManyMatchShouldThrow() throws IOException {
        cargoTypeRepository.insert(new CargoType(1, "2", null));

        cargoTypeServiceMock.setCargoTypes(
            new CargoType(1, "1", null),
            new CargoType(2, "2", null)
        );

        Assertions.assertThatThrownBy(() -> updateCargoTypesExecutor.execute())
            .isInstanceOf(RuntimeException.class)
            .hasMessageMatching("received data matches incorrectly.*");
    }

    @Test
    public void whenFindsManyToOneMatchShouldThrow() throws IOException {
        cargoTypeRepository.insertBatch(Arrays.asList(
            new CargoType(1, "1", null),
            new CargoType(2, "2", null)
        ));

        cargoTypeServiceMock.setCargoTypes(new CargoType(1, "2", null));

        Assertions.assertThatThrownBy(() -> updateCargoTypesExecutor.execute())
            .isInstanceOf(RuntimeException.class)
            .hasMessageMatching("received data matches incorrectly.*");
    }

    @Test
    public void whenReceivesCorrectDataReturnsCorrectNewData() throws Exception {
        cargoTypeRepository.insert(new CargoType(1, "1", null));

        cargoTypeServiceMock.setCargoTypes(
            new CargoType(1, "1", null),
            new CargoType(2, "2", null)
        );

        Assertions.assertThat(cargoTypeRepository.findAll())
            .containsExactly(new CargoType(1, "1", null));
        updateCargoTypesExecutor.execute();
        Assertions.assertThat(cargoTypeRepository.findAll())
            .containsExactlyInAnyOrder(
                new CargoType(1, "1", null),
                new CargoType(2, "2", null)
            );
    }

    @Test
    public void testUpdateMboData() throws IOException {
        cargoTypeRepository.insert(new CargoType(1, "1", null));

        cargoTypeServiceMock.setCargoTypes(
            new CargoType(1, "1", null),
            new CargoType(2, "2", null)
        );

        MboParameters.Parameter p = MboParameters.Parameter.newBuilder()
            .setId(5L)
            .setXslName("cargoType2")
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .addOption(MboParameters.Option.newBuilder().setId(11L).addName(WordProtoUtil.defaultWord("true")).build())
            .addOption(MboParameters.Option.newBuilder().setId(10L).addName(WordProtoUtil.defaultWord("false")).build())
            .build();
        Mockito.when(categoryParametersService.getGlobalParameters(Mockito.any()))
            .thenReturn(MboParameters.GetGlobalParametersResponse.newBuilder()
                .addParameter(p)
                .build());

        updateCargoTypesExecutor.execute();

        Assertions.assertThat(cargoTypeRepository.findAll()).containsExactlyInAnyOrder(
            new CargoType(1, "1", null),
            new CargoType(2, "2", 5L, 10L, 11L)
        );
    }
}
