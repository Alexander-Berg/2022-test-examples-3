package ru.yandex.market.wms.receiving.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.exception.BadRequestException;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.core.base.dto.SerialInventoryDto;
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse;
import ru.yandex.market.wms.core.base.response.GetMeasureBuffersResponse;
import ru.yandex.market.wms.core.base.response.GetParentContainerResponse;
import ru.yandex.market.wms.core.base.response.GetSerialInventoriesByIdResponse;
import ru.yandex.market.wms.core.base.response.Location;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.core.client.exception.ContainerCannotBeUsedAsChildException;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.transportation.core.model.CreateTransportOrderDTO;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ContainerControllerTest extends ReceivingIntegrationTest {

    @MockBean
    @Autowired
    private CoreClient coreClient;

    @MockBean
    @Autowired
    protected JmsTemplate defaultJmsTemplate;

    private final ArgumentCaptor<CreateTransportOrderDTO> createTransportOrderDTOArgumentCaptor =
            ArgumentCaptor.forClass(CreateTransportOrderDTO.class);

    @AfterEach
    void tearDown() {
        Mockito.reset(coreClient);
    }

    @Test
    @DatabaseSetup("/controller/container/get-containers-on-table/existing/db.xml")
    @ExpectedDatabase(value = "/controller/container/get-containers-on-table/existing/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getByExistingTableId() throws Exception {
        assertApiCallOk("controller/container/get-containers-on-table/existing/request.json",
                "controller/container/get-containers-on-table/existing/response.json",
                post("/container/get-containers-on-table"));
    }

    @Test
    @DatabaseSetup("/controller/container/get-containers-on-table/wrong/db.xml")
    @ExpectedDatabase(value = "/controller/container/get-containers-on-table/wrong/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getByWrongTableId() throws Exception {
        assertApiCallOk("controller/container/get-containers-on-table/wrong/request.json",
                "controller/container/get-containers-on-table/wrong/response.json",
                post("/container/get-containers-on-table"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RCP000001", "CDR000001", "BL00000001", "BM00000001", "TM00000001", "VS00000001"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentTableWithoutNesting(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-false.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE01")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RCP000001", "CDR000001", "BL00000001", "BM00000001"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentTareAlreadyHasParent(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse("PARENT01"));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-false.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE02")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TM00000001", "VS00000001", "BL00000001", "BM00000001", "WRONGID01"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentTareNotFlipboxConveyor(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse(null));
        doThrow(createContainerCannotBeUsedAsChildException())
                .when(coreClient).checkChild("WRONGID01");

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-false.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE02")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RCP000001", "CDR000001", "BL00000001", "BM00000001"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentWithDefaultContainerConveyor(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-false.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE02")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITH_DEFAULT_CONTAINER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TM00000001", "VS00000001", "WRONGID01"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentTareNotFlipboxNotConveyor(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse(null));
        doThrow(createContainerCannotBeUsedAsChildException())
                .when(coreClient).checkChild(any(String.class));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-false.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE03")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    @Test
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentWithWrongLocXIdStatus() throws Exception {
        Mockito.when(coreClient.getParentContainer("RCP03"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-false.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE02")
                        .param("containerId", "RCP03")
                        .param("containerType", "ANOMALY")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RCP000001", "CDR000001"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentTrueConveyor(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-true.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE02")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RCP000001", "CDR000001", "BL00000001", "BM00000001"})
    @DatabaseSetup("/controller/container/check-need-parent-container/before.xml")
    public void checkNeedParentTrueNotConveyor(String id) throws Exception {
        Mockito.when(coreClient.getParentContainer(id))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(null, "controller/container/check-need-parent-container/response-true.json",
                get("/container/check-need-parent-container")
                        .param("tableId", "STAGE03")
                        .param("containerId", id)
                        .param("containerType", "STOCK")
                        .param("defaultContainerSettings", "WITHOUT_DEFAULT_CONTAINER"));
    }

    /**
     * Привязанной тары нет.
     * запрос на перемещение товара в новую пустую тару, не требует подтверждения
     */
    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/not-conveyor-items.xml")
    @DatabaseSetup("/controller/container/put-item/new-container/before.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/new-container/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemOnContainerWhenNewContainerNotConfirmed() throws Exception {
        createEmptyMocks();

        assertApiCallOk(
                "controller/container/put-item/new-container/request1.json",
                "controller/container/put-item/new-container/response1.json",
                post("/container/put-item"));
    }

    /**
     * Привязанной тары нет.
     * запрос на перемещение товара в новую пустую тару, не требует подтверждения
     */
    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/not-conveyor-items.xml")
    @DatabaseSetup("/controller/container/put-item/relink-empty-container/before.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/relink-empty-container/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemOnContainerWhenRelinkContainer() throws Exception {
        createEmptyMocks();

        assertApiCallOk(
                "controller/container/put-item/relink-empty-container/request1.json",
                "controller/container/put-item/relink-empty-container/response1.json",
                post("/container/put-item"));
    }

    /**
     * Запрос на перемещение товара в уже привязанную тару, не требует подтверждения
     */
    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/not-conveyor-items.xml")
    @DatabaseSetup("/controller/container/put-item/same-container/containers.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/same-container/containers.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemOnContainerWhenSameContainer() throws Exception {
        createEmptyMocks();

        assertApiCallOk(
                "controller/container/put-item/same-container/request.json",
                "controller/container/put-item/same-container/response.json",
                post("/container/put-item"));
    }

    /**
     * Следущие 2 теста идут вместе. Тара привязана.
     * первый - это запрос на перемещение товара в новую пустую тару, требующая подтверждения,
     * второй - это подтвержденный запрос. Происходит отвязка старой тары и привязка новой
     */
    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/not-conveyor-items.xml")
    @DatabaseSetup("/controller/container/put-item/another-container/before.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/another-container/between.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemOnContainerWhenAnotherNewContainerNotConfirmed() throws Exception {
        createEmptyMocks();

        assertApiCallOk(
                "controller/container/put-item/another-container/request1.json",
                "controller/container/put-item/another-container/response1.json",
                post("/container/put-item"));
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/not-conveyor-items.xml")
    @DatabaseSetup("/controller/container/put-item/another-container/between.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/another-container/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemOnContainerWhenAnotherNewContainerConfirmed() throws Exception {
        createEmptyMocks();

        assertApiCallOk(
                "controller/container/put-item/another-container/request2.json",
                "controller/container/put-item/another-container/response2.json",
                post("/container/put-item"));
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-overweight/before-simple.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-overweight/before-simple.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithOverweightSimpleError() throws Exception {
        createEmptyMocks();
        doThrow(createContainerCannotBeUsedAsChildException())
                .when(coreClient).checkChild(any(String.class));

        assertApiCallError(
                "controller/container/put-item/conveyor-overweight/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Контейнер превысил ограничения по весу для конвейера, выберите другой контейнер");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-overweight/before-2nd-item.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/common-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-overweight/before-2nd-item.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithOverweightIn2ndRequestError() throws Exception {
        createEmptyMocks();
        doThrow(createContainerCannotBeUsedAsChildException())
                .when(coreClient).checkChild(any(String.class));

        assertApiCallError(
                "controller/container/put-item/conveyor-overweight/request2.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Контейнер превысил ограничения по весу для конвейера, выберите другой контейнер");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-big-and-small/before-by-cube.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-big-and-small/before-by-cube.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorBigAndSmallByCubeError() throws Exception {
        createEmptyMocks();
        doThrow(createContainerCannotBeUsedAsChildException())
                .when(coreClient).checkChild(any(String.class));

        assertApiCallError(
                "controller/container/put-item/conveyor-big-and-small/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Нельзя складывать в одну тару большие и маленькие товары");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-big-and-small/before-by-weight.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-big-and-small/before-by-weight.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorBigAndSmallByWeightError() throws Exception {
        createEmptyMocks();
        doThrow(createContainerCannotBeUsedAsChildException())
                .when(coreClient).checkChild(any(String.class));

        assertApiCallError(
                "controller/container/put-item/conveyor-big-and-small/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Нельзя складывать в одну тару большие и маленькие товары");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-nesting-big-and-small/before-by-cube.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-nesting-big-and-small/before-by-cube.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithNestingBigAndSmallByCubeError() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));

        Mockito.when(coreClient.getChildContainers("RCP02"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP02"))
                .thenReturn(new GetParentContainerResponse("TM01"));

        assertApiCallError(
                "controller/container/put-item/conveyor-nesting-big-and-small/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Нельзя складывать в одну тару большие и маленькие товары");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-nesting-big-and-small/before-by-weight.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-nesting-big-and-small/before-by-weight.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithNestingBigAndSmallByWeightError() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));

        Mockito.when(coreClient.getChildContainers("RCP02"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP02"))
                .thenReturn(new GetParentContainerResponse("TM01"));

        assertApiCallError(
                "controller/container/put-item/conveyor-nesting-big-and-small/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Нельзя складывать в одну тару большие и маленькие товары");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-nesting-overweight/before.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-nesting-overweight/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithNestingOverweightError() throws Exception {
        Mockito.when(coreClient.getChildContainers("PARENT01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));
        Mockito.when(coreClient.getParentContainer("RCP02"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallError(
                "controller/container/put-item/conveyor-nesting-overweight/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-nesting-another-container-inside/before.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-nesting-another-container-inside/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithNestingContainerHasAnotherContainersInsideError() throws Exception {
        Mockito.when(coreClient.getChildContainers("PARENT01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));

        Mockito.when(coreClient.getChildContainers("RCP01"))
                .thenReturn(new GetChildContainersResponse(List.of("")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse("PARENT01"));

        assertApiCallError(
                "controller/container/put-item/conveyor-nesting-another-container-inside/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "В тару уже вложены другие тары, в нее нельзя положить товар");
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-nesting-another-container-inside/before.xml")
    void putItemConveyorWithNestingContainerHasAnotherContainersInsideOK() throws Exception {
        Mockito.when(coreClient.getChildContainers("PARENT01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));

        Mockito.when(coreClient.getChildContainers("RCP01"))
                .thenReturn(new GetChildContainersResponse(List.of("")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse("PARENT01"));

        assertApiCallOk(
                "controller/container/put-item/conveyor-nesting-another-container-inside/request_move_to_lost.json",
                "controller/container/put-item/conveyor-nesting-another-container-inside/response.json",
                post("/container/put-item"));
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/conveyor-nesting-ok/before.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/conveyor-nesting-ok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithNestingOk() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));

        Mockito.when(coreClient.getChildContainers("RCP02"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP02"))
                .thenReturn(new GetParentContainerResponse("TM01"));

        assertApiCallOk(
                "controller/container/put-item/conveyor-nesting-ok/request.json",
                "controller/container/put-item/conveyor-nesting-ok/response.json",
                post("/container/put-item"));
    }

    @Test
    @DatabaseSetup("/controller/container/put-item/common-before.xml")
    @DatabaseSetup("/controller/container/put-item/not-conveyor-items.xml")
    @DatabaseSetup("/controller/container/put-item/another-type/containers.xml")
    @ExpectedDatabase(value = "/controller/container/put-item/another-type/containers.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/container/put-item/common-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void putItemConveyorWithContainerHasAnotherType() throws Exception {
        createEmptyMocks();

        assertApiCallError(
                "controller/container/put-item/another-type/request.json",
                post("/container/put-item"),
                HttpStatus.BAD_REQUEST,
                "Container ReceivingContainer(containerId=CART123," +
                        " locationId=STAGE01, type=THERMAL) has another type " +
                        "and can not be linked to type STOCK");
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    void addParentLinkAlreadyExists() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse("TM01"));

        assertApiCallOk(
                "controller/container/add-parent-container/link-already-exists/request.json",
                null,
                post("/container/add-parent-container"));
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    void addParentAnotherLinkExistsError() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse("TM02"));

        assertApiCallError(
                "controller/container/add-parent-container/another-link-exists-error/request.json",
                post("/container/add-parent-container"),
                HttpStatus.BAD_REQUEST,
                "Для выбранных параметров вложенность не поддерживается");
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    void addParentAnotherWrongTableLinkError() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP02")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallError(
                "controller/container/add-parent-container/conveyor-nesting-error-wrong-table-link/request.json",
                post("/container/add-parent-container"),
                HttpStatus.BAD_REQUEST,
                "Одна из вложенных тар внутри TM01 привязана к другому столу. Выберите другую родительскую тару.");
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    void addParentAnotherWrongTableLinkOk() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP02")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(
                "controller/container/add-parent-container/conveyor-nesting-error-wrong-table-link-2/request.json",
                null,
                post("/container/add-parent-container"));
    }

    /**
     * Проверяет случай когда сервис core вернул дочерние контейнеры, но по балансам они никуда не привязаны. В таком
     * случае запрещаем вкладывать данный контейнер в ТОТ.
     *
     */
    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    void checkTotForWasteContainers() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("WASTE")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallError(
                "controller/container/add-parent-container/conveyor-nesting-error-wrong-table-link/request.json",
                post("/container/add-parent-container"),
                HttpStatus.BAD_REQUEST,
                "Одна из вложенных тар внутри TM01 привязана к другому столу. Выберите другую родительскую тару.");
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    @DatabaseSetup("/controller/container/add-parent-container/another-items-inside-error/before.xml")
    void addParentParentAlreadyHasItemsError() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));
        doThrow(new BadRequestException("Родительский контейнер TM01 содержит УИТы"))
                .when(coreClient).checkParent(any(String.class));

        assertApiCallError(
                "controller/container/add-parent-container/another-items-inside-error/request.json",
                post("/container/add-parent-container"),
                HttpStatus.BAD_REQUEST,
                "Родительский контейнер TM01 содержит УИТы");
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    void addParentOk() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(
                "controller/container/add-parent-container/ok/request.json",
                null,
                post("/container/add-parent-container"));
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    @ExpectedDatabase(value = "/controller/container/add-parent-container/empty-totes/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void addParentOkUpdatesToteStatusAndLoc() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(
                "controller/container/add-parent-container/ok/request.json",
                null,
                post("/container/add-parent-container"));
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    @DatabaseSetup("/controller/container/add-parent-container/parent-link/before.xml")
    void failIfParentHasLink() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallError(
                "controller/container/add-parent-container/parent-link/request.json",
                post("/container/add-parent-container"),
                HttpStatus.BAD_REQUEST,
                "Тара TM01 привязана к столу. Выберите другую тару.");
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    @DatabaseSetup("/controller/container/add-parent-container/parent-link/before.xml")
    void parentHasLinkOk() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallOk(
                "controller/container/add-parent-container/parent-link/request_unlink.json",
                null,
                post("/container/add-parent-container"));
    }

    @Test
    @DatabaseSetup("/controller/container/add-parent-container/common-before.xml")
    @DatabaseSetup("/controller/container/add-parent-container/children-with-another-type/before.xml")
    void addParentContainerParentHasChildrenWithAnotherType() throws Exception {
        Mockito.when(coreClient.getChildContainers("TM01"))
                .thenReturn(new GetChildContainersResponse(List.of("RCP03")));
        Mockito.when(coreClient.getParentContainer("RCP01"))
                .thenReturn(new GetParentContainerResponse(null));

        assertApiCallError(
                "controller/container/add-parent-container/children-with-another-type/request.json",
                post("/container/add-parent-container"),
                HttpStatus.BAD_REQUEST,
                "Container TM01 has children with type EXPENSIVE and can not be parent of type THERMAL");
    }

    @Test
    @DatabaseSetup("/controller/container/check-max-weight/before.xml")
    void checkMaxWeightOk() throws Exception {
        assertApiCallOk(
                null,
                null,
                get("/container/check-max-weight")
                        .param("sku", "ROV0000000000000000001")
                        .param("storerKey", "000001")
                        .param("qty", "25"));
    }

    @Test
    @DatabaseSetup("/controller/container/check-max-weight/before.xml")
    void checkMaxWeightOkNeedMeasurement() throws Exception {
        assertApiCallOk(
                null,
                null,
                get("/container/check-max-weight")
                        .param("sku", "ROV0000000000000000003")
                        .param("storerKey", "000001")
                        .param("qty", "25"));
    }

    @Test
    @DatabaseSetup("/controller/container/check-max-weight/before.xml")
    void checkMaxWeightError1() throws Exception {
        assertApiCallError(
                null,
                get("/container/check-max-weight")
                        .param("sku", "ROV0000000000000000001")
                        .param("storerKey", "000001")
                        .param("qty", "26"),
                HttpStatus.BAD_REQUEST,
                "Выбранное количество товара превышает макс. допустимый вес для конвейера, " +
                        "войдет только 25 шт.");
    }

    @Test
    @DatabaseSetup("/controller/container/check-max-weight/before.xml")
    void checkMaxWeightError2() throws Exception {
        assertApiCallError(
                null,
                get("/container/check-max-weight")
                        .param("sku", "ROV0000000000000000002")
                        .param("storerKey", "000001")
                        .param("qty", "26"),
                HttpStatus.BAD_REQUEST,
                "Товар не подходит для перемещения на конвейере по весу");
    }

    @Test
    @DatabaseSetup(value = "/controller/container/close-anomaly/ok/before.xml")
    @ExpectedDatabase(value = "/controller/container/close-anomaly/ok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerAnomalyOk() throws Exception {
        assertApiCallOk("controller/container/close-anomaly/ok/request.json",
                null,
                post("/container/close-anomaly"));
    }

    @Test
    @DatabaseSetup(value = "/controller/container/close-anomaly/error/before.xml")
    void closeContainerAnomalyError() throws Exception {
        assertApiCallError("controller/container/close-anomaly/error/request.json",
                post("/container/close-anomaly"),
                HttpStatus.BAD_REQUEST,
                "Переданный контейнер имеет тип Сток, а должен иметь тип Аномалия");
    }

    @Test
    @DatabaseSetup(value = "/controller/container/close-measure/conveyor/1/before.xml")
    void closeContainerMeasureToConveyor() throws Exception {
        Mockito.when(coreClient.getMeasurementBuffers(any(), any()))
                .thenReturn(new GetMeasureBuffersResponse(List.of(new Location("DIM_BUF_IN", ""))));

        assertApiCallOk("controller/container/close-measure/conveyor/1/request.json",
                null,
                post("/container/close-measure"));

        verify(defaultJmsTemplate, times(1)).convertAndSend(endsWith("create-transport-order"),
                createTransportOrderDTOArgumentCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderDTO request = createTransportOrderDTOArgumentCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getSource().getLoc()).isNotNull();
            assertions.assertThat(request.getSource().getLoc()).isEqualTo("CONV_IN");
            assertions.assertThat(request.getContainer()).isNotNull();
            assertions.assertThat(request.getContainer()).isEqualTo("TM10");
            assertions.assertThat(request.getDestination().getLoc()).isNotNull();
            assertions.assertThat(request.getDestination().getLoc()).isEqualTo("DIM_BUF_IN");
        });
    }

    @Test
    @DatabaseSetup(value = "/controller/container/close-measure/conveyor/1/before.xml")
    void closeContainerMeasureError() throws Exception {
        Mockito.when(coreClient.getMeasurementBuffers(any(), any()))
                .thenReturn(new GetMeasureBuffersResponse(Collections.emptyList()));

        assertApiCallError("controller/container/close-measure/conveyor/1/request.json",
                post("/container/close-measure"),
                HttpStatus.BAD_REQUEST,
                "Для локации STAGE01 не найдена связанная локация входного буфера переобмерочного стола");

        verify(defaultJmsTemplate, times(0)).convertAndSend(endsWith("create-transport-order"),
                createTransportOrderDTOArgumentCaptor.capture());
    }

    @Test
    @DatabaseSetup(value = "/controller/container/close-measure/task-detail/1/before.xml")
    @ExpectedDatabase(value = "/controller/container/close-measure/task-detail/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerMeasureCreateTaskDetail() throws Exception {
        Mockito.when(coreClient.getMeasurementBuffers(any(), any()))
                .thenReturn(new GetMeasureBuffersResponse(List.of(new Location("DIM_BUF_IN", ""))));

        Mockito.when(coreClient.getSerialInventoriesById(any()))
                        .thenReturn(new GetSerialInventoriesByIdResponse(List.of(
                                new SerialInventoryDto("", "", "", "", "", "",
                                        new BigDecimal(1), "", ""))));

        assertApiCallOk("controller/container/close-measure/task-detail/1/request.json",
                null,
                post("/container/close-measure"));

        verify(defaultJmsTemplate, times(0)).convertAndSend(endsWith("create-transport-order"),
                createTransportOrderDTOArgumentCaptor.capture());
    }

    @Test
    @DatabaseSetup(value = "/controller/container/close-measure/task-detail/2/before.xml")
    @ExpectedDatabase(value = "/controller/container/close-measure/task-detail/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerNonConveyableMeasureCreateTaskDetail() throws Exception {
        Mockito.when(coreClient.getMeasurementBuffers(any(), any()))
                .thenReturn(new GetMeasureBuffersResponse(List.of(new Location("DIM_BUF_IN", ""))));

        Mockito.when(coreClient.getSerialInventoriesById(any()))
                .thenReturn(new GetSerialInventoriesByIdResponse(List.of(
                        new SerialInventoryDto("", "", "", "", "", "",
                                new BigDecimal(1), "", ""))));

        assertApiCallOk("controller/container/close-measure/task-detail/2/request.json",
                null,
                post("/container/close-measure"));

        verify(defaultJmsTemplate, times(0)).convertAndSend(endsWith("create-transport-order"),
                createTransportOrderDTOArgumentCaptor.capture());
    }

    private void createEmptyMocks() {
        Mockito.when(coreClient.getChildContainers(any(String.class)))
                .thenReturn(new GetChildContainersResponse(List.of()));
        Mockito.when(coreClient.getParentContainer(any(String.class)))
                .thenReturn(new GetParentContainerResponse(null));
    }

    private void assertApiCallOk(String requestFile, String responseFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(requestFile, request, HttpStatus.OK, responseFile, null);
    }

    private void assertApiCallError(String requestFile,
                                    MockHttpServletRequestBuilder request,
                                    HttpStatus status) throws Exception {
        assertApiCall(requestFile, request, status, null, null);
    }

    private void assertApiCallError(String requestFile,
                                    MockHttpServletRequestBuilder request,
                                    HttpStatus status,
                                    String errorDescription) throws Exception {
        assertApiCall(requestFile, request, status, null, errorDescription);
    }

    private void assertApiCall(String requestFile,
                               MockHttpServletRequestBuilder request,
                               HttpStatus status,
                               String responseFile,
                               String errorDescription) throws Exception {
        if (null != requestFile) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(requestFile));
        }

        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status.value()))
                .andReturn();

        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString());
        }

        if (errorDescription != null) {
            assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .contains(errorDescription);
        }
    }

    private static ContainerCannotBeUsedAsChildException createContainerCannotBeUsedAsChildException() {
        return new ContainerCannotBeUsedAsChildException("Неподходящий тип дочернего контейнера");
    }
}
