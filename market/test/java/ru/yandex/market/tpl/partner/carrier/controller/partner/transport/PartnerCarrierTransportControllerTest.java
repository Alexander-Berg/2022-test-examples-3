package ru.yandex.market.tpl.partner.carrier.controller.partner.transport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.transport.EcologicalClass;
import ru.yandex.market.tpl.partner.carrier.model.transport.OwnershipType;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportCreateDto;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportDto;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportUpdateDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler.COMPANY_HEADER;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PartnerCarrierTransportControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private static final String TRANSPORT_NAME = "???????????????? ????????????????";
    private static final String VEHICLE_NUMBER = "B123CY";
    private static final BigDecimal CAPACITY = new BigDecimal("1.2");
    private static final int PALLETS_CAPACITY = 12;
    private static final BigDecimal GROSS_WEIGHT_TONS = new BigDecimal("3.0");
    private static final BigDecimal MAXIMUM_LOAD_ON_AXLE_TONS = new BigDecimal("1.0");
    private static final BigDecimal MAX_WEIGHT_TONS = new BigDecimal("1.0");
    private static final BigDecimal HEIGHT_METERS = new BigDecimal("3.0");
    private static final BigDecimal WIDTH_METERS = new BigDecimal("4.0");
    private static final BigDecimal LENGTH_METERS = new BigDecimal("5.0");
    private static final EcologicalClass ECO_CLASS = EcologicalClass.EURO5;
    private static final OwnershipType OWNERSHIP_TYPE = OwnershipType.PROPRIETARY;

    private static final String TRANSPORT_NAME2 = "???????? ?????? ????????????";
    private static final String VEHICLE_NUMBER2 = "C123BY";
    private static final BigDecimal CAPACITY2 = new BigDecimal("3.4");
    private static final int PALLETS_CAPACITY2 = 56;
    private static final BigDecimal GROSS_WEIGHT_TONS2 = new BigDecimal("4.0");
    private static final BigDecimal MAXIMUM_LOAD_ON_AXLE_TONS2 = new BigDecimal("2.0");
    private static final BigDecimal MAX_WEIGHT_TONS2 = new BigDecimal("8.0");
    private static final BigDecimal HEIGHT_METERS2 = new BigDecimal("3.5");
    private static final BigDecimal WIDTH_METERS2 = new BigDecimal("4.5");
    private static final BigDecimal LENGTH_METERS2 = new BigDecimal("15.0");
    private static final EcologicalClass ECO_CLASS2 = EcologicalClass.EURO5;
    private static final OwnershipType OWNERSHIP_TYPE2 = OwnershipType.RENT;


    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final TransportRepository transportRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
    }


    @SneakyThrows
    @Test
    void shouldAllowToEditLicencePlateIfNoActiveRuns() {
        PartnerCarrierTransportCreateDto dto1 = new PartnerCarrierTransportCreateDto();
        dto1.setName("?????????? ??????????????");
        dto1.setNumber("??777????777");
        dto1.setBrand("??????");
        dto1.setModel("??????????????");
        dto1.setCapacity(CAPACITY);
        dto1.setPalletsCapacity(PALLETS_CAPACITY);
        var transportId = postTransport(List.of(dto1)).get(0).getId();

        mockMvc.perform(
                        get("/internal/partner/transports")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canEditNumber").value(true));


        PartnerCarrierTransportUpdateDto update = new PartnerCarrierTransportUpdateDto();
        update.setName(dto1.getName());
        update.setNumber("??888????888");
        update.setBrand(dto1.getBrand());
        update.setModel(dto1.getModel());
        update.setCapacity(dto1.getCapacity());
        update.setPalletsCapacity(dto1.getPalletsCapacity());

        mockMvc.perform(
                put("/internal/partner/transports/" + transportId)
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(update))
        ).andExpect(status().isOk());


    }

    @SneakyThrows
    @Test
    void shouldNotAllowToEditLicencePlateIfHasActiveRuns() {
        PartnerCarrierTransportCreateDto dto1 = new PartnerCarrierTransportCreateDto();
        dto1.setName("?????????? ??????????????");
        dto1.setNumber("??777????777");
        dto1.setBrand("??????");
        dto1.setModel("??????????????");
        dto1.setCapacity(CAPACITY);
        dto1.setPalletsCapacity(PALLETS_CAPACITY);
        var transportId = postTransport(List.of(dto1)).get(0).getId();

        var run= runGenerator.generate();
        var user = testUserHelper.findOrCreateUser(123L);
        var transport= transportRepository.findById(transportId).orElseThrow();
        runHelper.assignUserAndTransport(run, user, transport);

        String createContent = mockMvc.perform(
                        get("/internal/partner/transports")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canEditNumber").value(false))
                .andReturn()
                .getResponse().getContentAsString();

        PartnerCarrierTransportDto result = tplObjectMapper.readValue(createContent, PartnerCarrierTransportDto.class);
        PartnerCarrierTransportUpdateDto update = new PartnerCarrierTransportUpdateDto();
        update.setName(dto1.getName());
        update.setNumber("??888????888");
        update.setBrand(dto1.getBrand());
        update.setModel(dto1.getModel());
        update.setCapacity(dto1.getCapacity());
        update.setPalletsCapacity(dto1.getPalletsCapacity());

        mockMvc.perform(
                put("/internal/partner/transports/" + transportId)
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(update))
        ).andExpect(status().isBadRequest());
    }


    @SneakyThrows
    @Test
    void shouldReturn400IfIncorrectRequestBody() {
        PartnerCarrierTransportCreateDto dto1 = new PartnerCarrierTransportCreateDto();
        dto1.setNumber(VEHICLE_NUMBER);
        dto1.setCapacity(CAPACITY);
        dto1.setPalletsCapacity(PALLETS_CAPACITY);

        mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto1)))
                .andExpect(status().isBadRequest());

        PartnerCarrierTransportCreateDto dto2 = new PartnerCarrierTransportCreateDto();
        dto2.setName(TRANSPORT_NAME);
        dto2.setCapacity(CAPACITY);
        dto2.setPalletsCapacity(PALLETS_CAPACITY);

        mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto1)))
                .andExpect(status().isBadRequest());

        PartnerCarrierTransportCreateDto dto3 = new PartnerCarrierTransportCreateDto();
        dto3.setName(TRANSPORT_NAME);
        dto3.setNumber(VEHICLE_NUMBER);
        dto3.setPalletsCapacity(PALLETS_CAPACITY);

        mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto1)))
                .andExpect(status().isBadRequest());

        PartnerCarrierTransportCreateDto dto4 = new PartnerCarrierTransportCreateDto();
        dto4.setName(TRANSPORT_NAME);
        dto4.setNumber(VEHICLE_NUMBER);
        dto4.setCapacity(CAPACITY);

        mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto1)))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void shouldCreateAndGetTransport() {
        PartnerCarrierTransportCreateDto dto = new PartnerCarrierTransportCreateDto();
        dto.setName(TRANSPORT_NAME);
        dto.setNumber(VEHICLE_NUMBER);
        dto.setCapacity(CAPACITY);
        dto.setPalletsCapacity(PALLETS_CAPACITY);
        dto.setGrossWeightTons(GROSS_WEIGHT_TONS);
        dto.setMaxLoadOnAxleTons(MAXIMUM_LOAD_ON_AXLE_TONS);
        dto.setMaxWeightTons(MAX_WEIGHT_TONS);
        dto.setHeightMeters(HEIGHT_METERS);
        dto.setWidthMeters(WIDTH_METERS);
        dto.setLengthMeters(LENGTH_METERS);
        dto.setEcologicalClass(EcologicalClass.EURO5);
        dto.setOwnershipType(OWNERSHIP_TYPE);

        mockMvc.perform(
                post("/internal/partner/transports")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk());

        String content = mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FakePage fakePage = tplObjectMapper.readValue(content, FakePage.class);
        List<PartnerCarrierTransportDto> transports = fakePage.getContent();
        PartnerCarrierTransportDto transport = transports.get(0);
        Assertions.assertThat(transport.getName()).isEqualTo(TRANSPORT_NAME);
        Assertions.assertThat(transport.getNumber()).isEqualTo(VEHICLE_NUMBER);
        Assertions.assertThat(transport.getCapacity()).isEqualTo(CAPACITY);
        Assertions.assertThat(transport.getPalletsCapacity()).isEqualTo(PALLETS_CAPACITY);
        Assertions.assertThat(transport.getGrossWeightTons()).isEqualTo(GROSS_WEIGHT_TONS);
        Assertions.assertThat(transport.getMaxLoadOnAxleTons()).isEqualTo(MAXIMUM_LOAD_ON_AXLE_TONS);
        Assertions.assertThat(transport.getMaxWeightTons()).isEqualTo(MAX_WEIGHT_TONS);
        Assertions.assertThat(transport.getHeightMeters()).isEqualTo(HEIGHT_METERS);
        Assertions.assertThat(transport.getWidthMeters()).isEqualTo(WIDTH_METERS);
        Assertions.assertThat(transport.getLengthMeters()).isEqualTo(LENGTH_METERS);
        Assertions.assertThat(transport.getEcologicalClass()).isEqualTo(ECO_CLASS);
        Assertions.assertThat(transport.getOwnershipType().name()).isEqualTo(OWNERSHIP_TYPE.name());
    }

    @SneakyThrows
    @Test
    void shouldCreateAndUpdateAndGetTransport() {
        PartnerCarrierTransportCreateDto dto = new PartnerCarrierTransportCreateDto();
        dto.setName(TRANSPORT_NAME);
        dto.setNumber(VEHICLE_NUMBER);
        dto.setCapacity(CAPACITY);
        dto.setPalletsCapacity(PALLETS_CAPACITY);
        dto.setGrossWeightTons(GROSS_WEIGHT_TONS);
        dto.setMaxLoadOnAxleTons(MAXIMUM_LOAD_ON_AXLE_TONS);
        dto.setMaxWeightTons(MAX_WEIGHT_TONS);
        dto.setHeightMeters(HEIGHT_METERS);
        dto.setWidthMeters(WIDTH_METERS);
        dto.setLengthMeters(LENGTH_METERS);
        dto.setEcologicalClass(EcologicalClass.EURO5);
        dto.setOwnershipType(OWNERSHIP_TYPE);

        String createContent = mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();
        PartnerCarrierTransportDto result = tplObjectMapper.readValue(createContent, PartnerCarrierTransportDto.class);

        PartnerCarrierTransportUpdateDto update = new PartnerCarrierTransportUpdateDto();
        update.setName(TRANSPORT_NAME2);
        update.setNumber(VEHICLE_NUMBER2);
        update.setCapacity(CAPACITY2);
        update.setPalletsCapacity(PALLETS_CAPACITY2);
        update.setGrossWeightTons(GROSS_WEIGHT_TONS2);
        update.setMaxLoadOnAxleTons(MAXIMUM_LOAD_ON_AXLE_TONS2);
        update.setMaxWeightTons(MAX_WEIGHT_TONS2);
        update.setHeightMeters(HEIGHT_METERS2);
        update.setWidthMeters(WIDTH_METERS2);
        update.setLengthMeters(LENGTH_METERS2);
        update.setEcologicalClass(EcologicalClass.EURO2);
        update.setOwnershipType(OWNERSHIP_TYPE2);

        mockMvc.perform(
                put("/internal/partner/transports/{id}", result.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(update))
        ).andExpect(ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("$.name").value(TRANSPORT_NAME2),
                jsonPath("$.number").value(VEHICLE_NUMBER2),
                jsonPath("$.capacity").value(CAPACITY2),
                jsonPath("$.palletsCapacity").value(PALLETS_CAPACITY2),
                jsonPath("$.grossWeightTons").value(GROSS_WEIGHT_TONS2),
                jsonPath("$.maxLoadOnAxleTons").value(MAXIMUM_LOAD_ON_AXLE_TONS2),
                jsonPath("$.maxWeightTons").value(MAX_WEIGHT_TONS2),
                jsonPath("$.heightMeters").value(HEIGHT_METERS2),
                jsonPath("$.widthMeters").value(WIDTH_METERS2),
                jsonPath("$.lengthMeters").value(LENGTH_METERS2),
                jsonPath("$.ecologicalClass").value(EcologicalClass.EURO2.name())
        ));

        String content = mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        FakePage fakePage = tplObjectMapper.readValue(content, FakePage.class);
        List<PartnerCarrierTransportDto> transports = fakePage.getContent();
        PartnerCarrierTransportDto transport = transports.get(0);
        Assertions.assertThat(transport.getName()).isEqualTo(TRANSPORT_NAME2);
        Assertions.assertThat(transport.getNumber()).isEqualTo(VEHICLE_NUMBER2);
        Assertions.assertThat(transport.getCapacity()).isEqualTo(CAPACITY2);
        Assertions.assertThat(transport.getPalletsCapacity()).isEqualTo(PALLETS_CAPACITY2);
        Assertions.assertThat(transport.getGrossWeightTons()).isEqualTo(GROSS_WEIGHT_TONS2);
        Assertions.assertThat(transport.getMaxLoadOnAxleTons()).isEqualTo(MAXIMUM_LOAD_ON_AXLE_TONS2);
        Assertions.assertThat(transport.getMaxWeightTons()).isEqualTo(MAX_WEIGHT_TONS2);
        Assertions.assertThat(transport.getHeightMeters()).isEqualTo(HEIGHT_METERS2);
        Assertions.assertThat(transport.getWidthMeters()).isEqualTo(WIDTH_METERS2);
        Assertions.assertThat(transport.getLengthMeters()).isEqualTo(LENGTH_METERS2);
        Assertions.assertThat(transport.getEcologicalClass()).isEqualTo(EcologicalClass.EURO2);
        Assertions.assertThat(transport.getOwnershipType().name()).isEqualTo(OWNERSHIP_TYPE2.name());
    }

    @SneakyThrows
    @Test
    void shouldDeleteAndGetTransport() {
        PartnerCarrierTransportCreateDto dto = new PartnerCarrierTransportCreateDto();
        dto.setName(TRANSPORT_NAME);
        dto.setNumber(VEHICLE_NUMBER);
        dto.setCapacity(CAPACITY);
        dto.setPalletsCapacity(PALLETS_CAPACITY);

        String createContent = mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        PartnerCarrierTransportDto result = tplObjectMapper.readValue(createContent, PartnerCarrierTransportDto.class);


        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(
            delete("/internal/partner/transports/{id}", result.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));
    }

    @SneakyThrows
    @Test
    void shouldDeleteAndGetTransport2() {
        PartnerCarrierTransportCreateDto dto = new PartnerCarrierTransportCreateDto();
        dto.setName(TRANSPORT_NAME);
        dto.setNumber(VEHICLE_NUMBER);
        dto.setCapacity(CAPACITY);
        dto.setPalletsCapacity(PALLETS_CAPACITY);

        String createContent = mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        PartnerCarrierTransportDto result = tplObjectMapper.readValue(createContent, PartnerCarrierTransportDto.class);


        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(
                delete("/internal/partner/transports/{id}", result.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk());

        mockMvc.perform(
                get("/internal/partner/transports/{id}", result.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void shouldGetFilteredTransport() {
        List<PartnerCarrierTransportCreateDto> dtos = createTransportDtosForFilters();
        postTransport(dtos);

        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("name", "??????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$.content[0].name").value(Matchers.anyOf(
                        Matchers.equalTo("?????????? ????????????????"),
                        Matchers.equalTo("?????????? ??????????????"),
                        Matchers.equalTo("?????????? ??????????????????"))))
                .andExpect(jsonPath("$.content[1].name").value(Matchers.anyOf(
                        Matchers.equalTo("?????????? ????????????????"),
                        Matchers.equalTo("?????????? ??????????????"),
                        Matchers.equalTo("?????????? ??????????????????"))))
                .andExpect(jsonPath("$.content[2].name").value(Matchers.anyOf(
                        Matchers.equalTo("?????????? ????????????????"),
                        Matchers.equalTo("?????????? ??????????????"),
                        Matchers.equalTo("?????????? ??????????????????"))));

        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("name", "??????")
                        .param("brand", "??")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value(Matchers.anyOf(
                                        Matchers.equalTo("?????????? ????????????????"),
                                        Matchers.equalTo("?????????? ??????????????"))))
                .andExpect(jsonPath("$.content[1].name").value(Matchers.anyOf(
                                        Matchers.equalTo("?????????? ????????????????"),
                                        Matchers.equalTo("?????????? ??????????????"))));

        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("name", "??????")
                        .param("brand", "??")
                        .param("model", "????????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("?????????? ????????????????"));

        mockMvc.perform(
                get("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("number", "??77")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value(Matchers.anyOf(
                        Matchers.equalTo("Semi Truck"),
                        Matchers.equalTo("?????????? ??????????????"))))
                .andExpect(jsonPath("$.content[1].name").value(Matchers.anyOf(
                        Matchers.equalTo("Semi Truck"),
                        Matchers.equalTo("?????????? ??????????????"))));
    }

    @Test
    @SneakyThrows
    void shouldFilterTransportConsideringLowercase() {
        PartnerCarrierTransportCreateDto dto1 = new PartnerCarrierTransportCreateDto();
        dto1.setName("?????????? ??????????????");
        dto1.setNumber("??777??C777");
        dto1.setBrand("??????");
        dto1.setModel("??????????????");
        dto1.setCapacity(CAPACITY);
        dto1.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto2 = new PartnerCarrierTransportCreateDto();
        dto2.setName("?????????? ????????????????");
        dto2.setNumber("??555??C666");
        dto2.setBrand("??????");
        dto2.setModel("????????????");
        dto2.setCapacity(CAPACITY);
        dto2.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto3 = new PartnerCarrierTransportCreateDto();
        dto3.setName("?????????? ??????????????????");
        dto3.setNumber("??444??C666");
        dto3.setBrand("??????");
        dto3.setModel("????????????");
        dto3.setCapacity(CAPACITY);
        dto3.setPalletsCapacity(PALLETS_CAPACITY);

        postTransport(List.of(dto1, dto2, dto3));

        mockMvc.perform(get("/internal/partner/transports")
                .header(COMPANY_HEADER, company.getCampaignId())
                .param("brand", "????")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/partner/transports")
                .header(COMPANY_HEADER, company.getCampaignId())
                .param("brand", "????")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldSuggestTransportByName() {
        List<PartnerCarrierTransportCreateDto> dtos = createTransportDtosForFilters();
        postTransport(dtos);

        mockMvc.perform(
                get("/internal/partner/transports/name")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(5)));
    }

    @SneakyThrows
    @Test
    void shouldSuggestTransportByNumber() {
        List<PartnerCarrierTransportCreateDto> dtos = createTransportDtosForFilters();
        postTransport(dtos);

        mockMvc.perform(
                get("/internal/partner/transports/number")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(5)));
    }

    @SneakyThrows
    @Test
    void shouldSuggestTransportByModel() {
        List<PartnerCarrierTransportCreateDto> dtos = createTransportDtosForSuggestions();
        postTransport(dtos);

        mockMvc.perform(
                get("/internal/partner/transports/model")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(5)));


        mockMvc.perform(
                get("/internal/partner/transports/model")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("brand", "??????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0]").value(Matchers.containsString("???????????? ????????")))
                .andExpect(jsonPath("$[1]").value(Matchers.containsString("???????????? ????????")));

        mockMvc.perform(
                get("/internal/partner/transports/model")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("brand", "????????????????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0]").value(Matchers.containsString("???????????? ??????????????????")))
                .andExpect(jsonPath("$[1]").value(Matchers.containsString("???????????? ??????????????????")));

        mockMvc.perform(
                get("/internal/partner/transports/model")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("brand", "??????????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value(Matchers.containsString("???????????? ??????????")));
    }

    @SneakyThrows
    @Test
    void shouldSuggestTransportByBrand() {
        List<PartnerCarrierTransportCreateDto> dtos = createTransportDtosForSuggestions();
        postTransport(dtos);

        mockMvc.perform(
                get("/internal/partner/transports/brand")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(3)));


        mockMvc.perform(
                get("/internal/partner/transports/brand")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("model", "???????????? ????????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value(Matchers.containsString("??????")));

        mockMvc.perform(
                get("/internal/partner/transports/brand")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("model", "???????????? ??????????????????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value(Matchers.containsString("????????????????")));

        mockMvc.perform(
                get("/internal/partner/transports/brand")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("model", "???????????? ??????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value(Matchers.containsString("??????????")));

        mockMvc.perform(
                get("/internal/partner/transports/brand")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("model", "????")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0]").value("????????????????"));
    }

    private List<PartnerCarrierTransportCreateDto> createTransportDtosForSuggestions() {
        PartnerCarrierTransportCreateDto dto1 = new PartnerCarrierTransportCreateDto();
        dto1.setName("?????? 1");
        dto1.setNumber("??777????777");
        dto1.setBrand("??????");
        dto1.setModel("???????????? ???????? 1");
        dto1.setCapacity(CAPACITY);
        dto1.setPalletsCapacity(PALLETS_CAPACITY);


        PartnerCarrierTransportCreateDto dto2 = new PartnerCarrierTransportCreateDto();
        dto2.setName("?????? 2");
        dto2.setNumber("??777????177");
        dto2.setBrand("??????");
        dto2.setModel("???????????? ???????? 2");
        dto2.setCapacity(CAPACITY);
        dto2.setPalletsCapacity(PALLETS_CAPACITY);


        PartnerCarrierTransportCreateDto dto3 = new PartnerCarrierTransportCreateDto();
        dto3.setName("???????? 1");
        dto3.setNumber("??777????773");
        dto3.setBrand("????????????????");
        dto3.setModel("???????????? ?????????????????? 1");
        dto3.setCapacity(CAPACITY);
        dto3.setPalletsCapacity(PALLETS_CAPACITY);


        PartnerCarrierTransportCreateDto dto4 = new PartnerCarrierTransportCreateDto();
        dto4.setName("???????? 2");
        dto4.setNumber("??777????723");
        dto4.setBrand("????????????????");
        dto4.setModel("???????????? ?????????????????? 2");
        dto4.setCapacity(CAPACITY);
        dto4.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto5 = new PartnerCarrierTransportCreateDto();
        dto5.setName("?????????? 1");
        dto5.setNumber("??772????777");
        dto5.setBrand("??????????");
        dto5.setModel("???????????? ?????????? 1");
        dto5.setCapacity(CAPACITY);
        dto5.setPalletsCapacity(PALLETS_CAPACITY);

        return List.of(dto1, dto2, dto3, dto4, dto5);
    }

    private List<PartnerCarrierTransportCreateDto> createTransportDtosForFilters() {
        PartnerCarrierTransportCreateDto dto1 = new PartnerCarrierTransportCreateDto();
        dto1.setName("?????????? ??????????????");
        dto1.setNumber("??777????777");
        dto1.setBrand("??????");
        dto1.setModel("??????????????");
        dto1.setCapacity(CAPACITY);
        dto1.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto2 = new PartnerCarrierTransportCreateDto();
        dto2.setName("?????????? ????????????????");
        dto2.setNumber("??666????666");
        dto2.setBrand("??????");
        dto2.setModel("????????????");
        dto2.setCapacity(CAPACITY);
        dto2.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto3 = new PartnerCarrierTransportCreateDto();
        dto3.setName("?????????? ??????????????????");
        dto3.setNumber("??765????321");
        dto3.setBrand("????????????????");
        dto3.setModel("????????????????");
        dto3.setCapacity(CAPACITY);
        dto3.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto4 = new PartnerCarrierTransportCreateDto();
        dto4.setName("Semi Truck");
        dto4.setNumber("??776????123");
        dto4.setBrand("??????????");
        dto4.setModel("????????????????????");
        dto4.setCapacity(CAPACITY);
        dto4.setPalletsCapacity(PALLETS_CAPACITY);

        PartnerCarrierTransportCreateDto dto5 = new PartnerCarrierTransportCreateDto();
        dto5.setName("????????????????");
        dto5.setNumber("??123??396");
        dto5.setBrand("????????");
        dto5.setModel("??????????");
        dto5.setCapacity(CAPACITY);
        dto5.setPalletsCapacity(PALLETS_CAPACITY);

        return List.of(dto1, dto2, dto3, dto4, dto5);
    }

    @Test
    @SneakyThrows
    void shouldNotSaveInvalidLicencePlate() {
        PartnerCarrierTransportCreateDto dto = new PartnerCarrierTransportCreateDto();
        dto.setName("????????????????");
        dto.setNumber("??????????????????????????????????????????????????");
        dto.setBrand("????????");
        dto.setModel("??????????");
        dto.setCapacity(CAPACITY);
        dto.setPalletsCapacity(PALLETS_CAPACITY);
        mockMvc.perform(
                post("/internal/partner/transports")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(dto))
        ).andExpect(status().isBadRequest());
    }

    private List<PartnerCarrierTransportDto> postTransport(List<PartnerCarrierTransportCreateDto> dtos) throws Exception {
        var list = new ArrayList<PartnerCarrierTransportDto>();
        for (PartnerCarrierTransportCreateDto dto : dtos) {
            var createContent = mockMvc.perform(
                    post("/internal/partner/transports")
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(tplObjectMapper.writeValueAsString(dto))
            ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            PartnerCarrierTransportDto result = tplObjectMapper.readValue(createContent, PartnerCarrierTransportDto.class);
            list.add(result);
        }
        return list;
    }

    @Data
    private static class FakePage {
        List<PartnerCarrierTransportDto> content;
    }
}
