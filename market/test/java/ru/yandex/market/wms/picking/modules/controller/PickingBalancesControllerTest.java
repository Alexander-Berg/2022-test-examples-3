package ru.yandex.market.wms.picking.modules.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse;
import ru.yandex.market.wms.core.base.response.GetParentContainerResponse;
import ru.yandex.market.wms.core.client.CoreClient;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingBalancesControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    private CoreClient coreClient;

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getItemsByLocHappyPath() throws Exception {
        mockMvc.perform(get("/balances/by-loc/STAGE"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getItemsBySkuHappyPath() throws Exception {
        mockMvc.perform(get("/balances/by-sku/ROV0000000000000056014"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getItemsByIdHappyPath() throws Exception {
        Mockito.when(coreClient.getParentContainer(any(String.class)))
                .thenReturn(new GetParentContainerResponse(null));
        Mockito.when(coreClient.getChildContainers(any(String.class)))
                .thenReturn(new GetChildContainersResponse(List.of()));

        mockMvc.perform(get("/balances/by-id/ID_TEST"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup-virtual.xml")
    public void getVirtualItemsByIdHappyPath() throws Exception {
        Mockito.when(coreClient.getParentContainer(any(String.class)))
                .thenReturn(new GetParentContainerResponse(null));
        Mockito.when(coreClient.getChildContainers(any(String.class)))
                .thenReturn(new GetChildContainersResponse(List.of()));

        mockMvc.perform(get("/balances/by-id/ID_TEST"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response-virtual.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getItemsByIdWithParentIdHappyPath() throws Exception {
        Mockito.when(coreClient.getParentContainer(any(String.class)))
                .thenReturn(new GetParentContainerResponse("PARENT_ID"));
        Mockito.when(coreClient.getChildContainers(any(String.class)))
                .thenReturn(new GetChildContainersResponse(List.of()));

        mockMvc.perform(get("/balances/by-id/ID_TEST"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response-with-parentid.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup-with-childids.xml")
    public void getItemsByIdWithChildIdHappyPath() throws Exception {
        Mockito.when(coreClient.getParentContainer(any(String.class)))
                .thenReturn(new GetParentContainerResponse(null));
        Mockito.when(coreClient.getChildContainers(any(String.class)))
                .thenReturn(new GetChildContainersResponse(List.of("ID_TEST_CHILD", "ID_TEST_CHILD_EMPTY")));

        mockMvc.perform(get("/balances/by-id/ID_TEST"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response-with-childids.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getItemsBySerialNumberHappyPath() throws Exception {
        mockMvc.perform(get("/balances/by-sn/4501493110"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/responseBySN.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup-pallet.xml")
    public void getItemsByPalletHappyPath() throws Exception {
        mockMvc.perform(get("/balances/by-pallet/PLT0000001"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response-pallet.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup-lotlocid.xml")
    public void checkReceivingContainerByIdValid() throws Exception {
        mockMvc.perform(get("/balances/check-barcode/000436587"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response-lotlocid-valid.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup-lotlocid.xml")
    public void checkReceivingContainerByIdInvalid() throws Exception {
        mockMvc.perform(get("/balances/check-barcode/000666777"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/balances/1/response-lotlocid-invalid.json")))
                .andReturn();
    }

    @Test
    public void getItemsByNullLoc() throws Exception {
        mockMvc.perform(get("/balances/by-loc/"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getNothingByLoc() throws Exception {
        mockMvc.perform(get("/balances/by-loc/1-01"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void getItemsByNullSku() throws Exception {
        mockMvc.perform(get("/balances/by-sku/"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getNothingBySku() throws Exception {
        mockMvc.perform(get("/balances/by-sku/ROV12345"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void getItemsByNullId() throws Exception {
        mockMvc.perform(get("/balances/by-id/"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getNothingById() throws Exception {
        mockMvc.perform(get("/balances/by-id/ID"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void getItemsByNullSerialNumber() throws Exception {
        mockMvc.perform(get("/balances/by-sn/"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/balances/1/setup.xml")
    public void getNothingBySerialNumber() throws Exception {
        mockMvc.perform(get("/balances/by-sn/SERIALNUMBER"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }
}
