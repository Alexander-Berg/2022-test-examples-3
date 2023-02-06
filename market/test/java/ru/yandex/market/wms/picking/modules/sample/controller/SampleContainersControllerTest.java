package ru.yandex.market.wms.picking.modules.sample.controller;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.PutawayZoneType;
import ru.yandex.market.wms.core.base.response.GetMeasureBuffersResponse;
import ru.yandex.market.wms.core.base.response.Location;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.transportation.client.TransportationClient;
import ru.yandex.market.wms.transportation.core.domain.TransportUnitQualifier;
import ru.yandex.market.wms.transportation.core.domain.TransportUnitType;
import ru.yandex.market.wms.transportation.core.model.request.TransportOrderCreateRequestBody;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SampleContainersControllerTest extends IntegrationTest {

    @Autowired
    @MockBean
    private CoreClient coreClient;

    @Autowired
    @MockBean
    private TransportationClient transportationClient;

    @BeforeEach
    public void before() {
        Mockito.reset(coreClient, transportationClient);
    }

    @Test
    @DatabaseSetup("/sample/controller/containers/happy-pass/before.xml")
    @ExpectedDatabase(value = "/sample/controller/containers/happy-pass/after.xml", assertionMode = NON_STRICT)
    public void attachContainerHappyPass() throws Exception {
        mockMvc.perform(post("/sample/containers/CART123/filling/MEASURE_STK"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/containers/close-happy-path/before.xml")
    @ExpectedDatabase(value = "/sample/controller/containers/close-happy-path/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void closeContainerHappyPath() throws Exception {
        Mockito.when(coreClient.getMeasurementBuffers("PICKTO", PutawayZoneType.DIMENSIONS_INBOUND))
                .thenReturn(
                        new GetMeasureBuffersResponse(
                                List.of(
                                        new Location("DEST", "3")
                                )
                        )
                );
        Mockito.when(transportationClient.createTransportOrder(Mockito.any())).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.post("/sample/containers/TM00001/close"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Mockito.verify(transportationClient, Mockito.times(1)).createTransportOrder(
                TransportOrderCreateRequestBody.builder()
                        .from("PICKTO")
                        .unit(
                                TransportUnitQualifier.builder()
                                        .id("TM00001")
                                        .type(TransportUnitType.CONTAINER)
                                        .build()
                        )
                        .to(
                                Mockito.any()
                        )
                        .build()
        );
    }

    @Test
    @DatabaseSetup("/sample/controller/containers/close-location-not-found/immutable.xml")
    @ExpectedDatabase(value = "/sample/controller/containers/close-location-not-found/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void closeContainerLocNotFound() throws Exception {
        Mockito.when(coreClient.getMeasurementBuffers("PICKTO", PutawayZoneType.DIMENSIONS_INBOUND))
                .thenReturn(new GetMeasureBuffersResponse(Collections.emptyList()));

        mockMvc.perform(MockMvcRequestBuilders.post("/sample/containers/TM00001/close"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();
    }
}
