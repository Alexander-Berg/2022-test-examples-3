package ru.yandex.travel.externalapi.endpoint.bnovo;

import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.travel.externalapi.service.providers.AdministratorInterfaceProvider;
import ru.yandex.travel.hotels.administrator.grpc.proto.AdministratorInterfaceGrpc;
import ru.yandex.travel.hotels.administrator.grpc.proto.EAcceptAgreementStatus;
import ru.yandex.travel.hotels.administrator.grpc.proto.EAgreementStatusType;
import ru.yandex.travel.hotels.administrator.grpc.proto.EHotelStatus;
import ru.yandex.travel.hotels.administrator.grpc.proto.EUnpublishedReason;
import ru.yandex.travel.hotels.administrator.grpc.proto.TAcceptAgreementRsp;
import ru.yandex.travel.hotels.administrator.grpc.proto.TAgreementStatusRsp;
import ru.yandex.travel.hotels.administrator.grpc.proto.THotelDetailsChangedRsp;
import ru.yandex.travel.hotels.administrator.grpc.proto.THotelStatusRsp;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.testing.misc.TestResources;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class BnovoControllerTest {

    private static final String HOTEL_CODE = "1024";
    private static final String INN = "123456";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdministratorInterfaceProvider administratorInterfaceProvider;

    @Mock
    private AdministratorInterfaceGrpc.AdministratorInterfaceFutureStub administratorInterfaceFutureStubMock;

    @Test
    public void postHotelDetailsChanged() throws Exception {
        when(administratorInterfaceProvider.provideInterface()).thenReturn(administratorInterfaceFutureStubMock);
        when(administratorInterfaceFutureStubMock.hotelDetailsChanged(ArgumentMatchers.argThat(req ->
                req.getHotelCode().equals(HOTEL_CODE))))
                .thenReturn(Futures.immediateFuture(THotelDetailsChangedRsp.newBuilder().build()));
        String request = TestResources.readResource("bnovo/hotel_details_changed_request.json");
        MvcResult result = mockMvc.perform(
                post("/bnovo/hotel_details_changed")
                        .content(request)
                        .contentType("application/json"))
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void getHotelStatusNotFound() throws Exception {
        when(administratorInterfaceProvider.provideInterface()).thenReturn(administratorInterfaceFutureStubMock);
        when(administratorInterfaceFutureStubMock.hotelStatus(ArgumentMatchers.argThat(req ->
                req.getHotelCode().equals(HOTEL_CODE)))).thenReturn(Futures.immediateFuture(
                THotelStatusRsp.newBuilder()
                        .setHotelCode(HOTEL_CODE)
                        .setHotelStatus(EHotelStatus.H_NOT_FOUND)
                        .setUnpublishedReason(EUnpublishedReason.UR_NONE)
                        .build()));
        MvcResult result = mockMvc.perform(
                get("/bnovo/hotel_status")
                        .param("hotel_code", HOTEL_CODE))
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    public void getHotelStatusPublished() throws Exception {
        when(administratorInterfaceProvider.provideInterface()).thenReturn(administratorInterfaceFutureStubMock);
        when(administratorInterfaceFutureStubMock.hotelStatus(ArgumentMatchers.argThat(req ->
                req.getHotelCode().equals(HOTEL_CODE)))).thenReturn(Futures.immediateFuture(
                THotelStatusRsp.newBuilder()
                        .setHotelCode(HOTEL_CODE)
                        .setHotelStatus(EHotelStatus.H_PUBLISHED)
                        .setUnpublishedReason(EUnpublishedReason.UR_NONE)
                        .build()));
        String response = TestResources.readResource("bnovo/hotel_status_response.json")
                .replace(" ", "")
                .replace("\n", "");
        MvcResult result = mockMvc.perform(
                get("/bnovo/hotel_status")
                        .param("hotel_code", HOTEL_CODE))
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(response));
    }

    @Test
    public void checkAgreementNotFound() throws Exception {
        when(administratorInterfaceProvider.provideInterface()).thenReturn(administratorInterfaceFutureStubMock);
        when(administratorInterfaceFutureStubMock.agreementStatus(ArgumentMatchers.argThat(req ->
                req.getInn().equals(INN)))).thenReturn(Futures.immediateFuture(
                TAgreementStatusRsp.newBuilder()
                        .setAgreementStatus(EAgreementStatusType.AS_NOT_FOUND)
                        .build()));
        String response = TestResources.readResource("bnovo/agreement_status_not_found.json")
                .replace(" ", "")
                .replace("\n", "");
        MvcResult result = mockMvc.perform(
                get("/bnovo/check_agreement")
                        .param("inn", INN))
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(response));
    }

    @Test
    public void checkAgreementFound() throws Exception {
        when(administratorInterfaceProvider.provideInterface()).thenReturn(administratorInterfaceFutureStubMock);
        when(administratorInterfaceFutureStubMock.agreementStatus(ArgumentMatchers.argThat(req ->
                req.getInn().equals(INN)))).thenReturn(Futures.immediateFuture(
                TAgreementStatusRsp.newBuilder()
                        .setAgreementStatus(EAgreementStatusType.AS_FOUND)
                        .build()));
        String response = TestResources.readResource("bnovo/agreement_status_found.json")
                .replace(" ", "")
                .replace("\n", "");
        MvcResult result = mockMvc.perform(
                get("/bnovo/check_agreement")
                        .param("inn", INN))
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(response));
    }

    @Test
    public void acceptAgreement() throws Exception {
        when(administratorInterfaceProvider.provideInterface()).thenReturn(administratorInterfaceFutureStubMock);
        when(administratorInterfaceFutureStubMock.acceptAgreement(ArgumentMatchers.argThat(req ->
                req.getInn().equals(INN)
                        && req.getPartnerId().equals(EPartnerId.PI_BNOVO)
                        && req.getHotelCode().equals(HOTEL_CODE)))).thenReturn(Futures.immediateFuture(
                TAcceptAgreementRsp.newBuilder()
                        .setStatus(EAcceptAgreementStatus.AAS_SUCCESS)
                        .build()));
        String request = TestResources.readResource("bnovo/accept_agreement_request.json");
        String response = TestResources.readResource("bnovo/accept_agreement_response.json")
                .replace(" ", "")
                .replace("\n", "");
        MvcResult result = mockMvc.perform(
                post("/bnovo/accept_agreement")
                        .content(request)
                        .contentType("application/json"))
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(response));
    }

}
