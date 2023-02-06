package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDoc;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDocRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunDocumentControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private final MockMvc mockMvc;
    private final RunGenerator manualRunService;
    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final CarRequestDocRepository carRequestDocRepository;
    private final RunHelper runHelper;

    private Run run;
    private Company company;
    private User user1;
    private Transport transport;

    @BeforeEach
    void setup() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID);

        transport = testUserHelper.findOrCreateTransport();

        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        OrderWarehouse warehouseTo = orderWarehouseGenerator.generateWarehouse();
        run = manualRunService.generate(b -> b
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("123")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(64)
                                                .build(),
                                        1,
                                        null,
                                        null
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("345")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(64)
                                                .build(),
                                        2,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    @SneakyThrows
    @Test
    void carRequestFailed() {
        CarRequestDoc doc = carRequestDocRepository.findCarRequestDocByRun(run).get(0);
        mockMvc.perform(get("/internal/partner/runs/{id}/carRequest/{docId}", run.getId(), doc.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().is4xxClientError());
    }

    @SneakyThrows
    @Test
    void carRequest() {
        runHelper.assignUserAndTransport(run, user1, transport);
        CarRequestDoc doc = carRequestDocRepository.findCarRequestDocByRun(run).get(0);
        mockMvc.perform(get("/internal/partner/runs/{id}/carRequest/{docId}", run.getId(), doc.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk());
    }
}
