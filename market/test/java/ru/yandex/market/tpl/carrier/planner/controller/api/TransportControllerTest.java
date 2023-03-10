package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportSource;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportType;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportTypeSource;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.service.api.transport.InternalTransportFilter;
import ru.yandex.market.tpl.carrier.planner.service.api.transport.TransportSpecification;
import ru.yandex.mj.generated.server.model.TransportCreateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TransportControllerTest extends BasePlannerWebTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TestUserHelper testUserHelper;
    private final TransportRepository transportRepository;
    private final TransportTypeRepository transportTypeRepository;

    private Company firstCompany;
    private Company secondCompany;

    @BeforeEach
    void init() {
        firstCompany = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        secondCompany = testUserHelper.findOrCreateCompany("???????????? ????????????????");
    }

    @Test
    void testCreate() throws Exception {
        var dto = new TransportCreateDto()
                .number("MH891K")
                .name("?????????????? 1")
                .capacity(12d)
                .companyId(firstCompany.getId())
                .palletsCapacity(10)
                .brand("??????????");

        mockMvc.perform(post("/internal/transport/create")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("?????????????? 1"))
                .andExpect(jsonPath("$.number").value("MH891K"))
                .andExpect(jsonPath("$.brand").value("??????????"))
                .andExpect(jsonPath("$.source").value(TransportSource.LOGISTICS_COORDINATOR.name()));

        transactionTemplate.execute(t -> {
            assertThat(transportRepository.findAll(new TransportSpecification(
                    InternalTransportFilter.builder().name("?????????????? 1").build()
            )).get(0))
                    .extracting(
                            Transport::getName,
                            Transport::getNumber,
                            Transport::getCompany,
                            Transport::getBrand,
                            Transport::getModel,
                            Transport::getSource
                    )
                    .containsExactly(
                            "?????????????? 1",
                            "MH891K",
                            firstCompany,
                            "??????????",
                            null,
                            TransportSource.LOGISTICS_COORDINATOR
                    );

            assertThat(transportTypeRepository.findByNameAndCompanyId("?????????????? 1", firstCompany.getId())
                    .get()
            )
                    .extracting(
                            TransportType::getCompany,
                            TransportType::getName,
                            TransportType::getCapacity,
                            TransportType::getSource,
                            TransportType::getPalletsCapacity
                    )
                    .containsExactly(
                            firstCompany,
                            "?????????????? 1",
                            BigDecimal.valueOf(12.0),
                            TransportTypeSource.LOGISTICS_COORDINATOR,
                            10
                    );

            return null;
        });

        mockMvc.perform(post("/internal/transport/create")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(MAPPER.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGet() throws Exception {
        Transport firstTransport = testUserHelper.findOrCreateTransport("?????????????? ????????", firstCompany.getName());
        Transport secondTransport = testUserHelper.findOrCreateTransport("?????????????? ??????", secondCompany.getName());

        mockMvc.perform(get("/internal/transport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/transport")
                        .param("name", "?????????????? ????????"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(firstTransport.getId()));

        mockMvc.perform(get("/internal/transport")
                        .param("companyId", secondCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(secondTransport.getId()));

        mockMvc.perform(get("/internal/transport")
                .param("id", secondTransport.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(secondTransport.getId()));

        mockMvc.perform(get("/internal/transport")
                        .param("companyId", secondCompany.getId().toString())
                        .param("name", "?????????????? ????????")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));
    }

    @Test
    void testGetByNumber() throws Exception {
        //latin
        Transport transport = testUserHelper.createTransport("??????????", firstCompany, " BC 923 M");

        mockMvc.perform(get("/internal/transport")
                .param("number", "????923??"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(transport.getId()));


        //cyrillic
        mockMvc.perform(get("/internal/transport")
                .param("number", "???? 923?? "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(transport.getId()));

        //latin
        mockMvc.perform(get("/internal/transport")
                .param("number", "BC923M"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(transport.getId()));

    }
}
