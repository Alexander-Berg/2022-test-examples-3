package ru.yandex.market.tpl.carrier.lms.controller.report;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.lms.controller.LmsControllerTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsRunReportControllerTest extends LmsControllerTest {

    private final TestableClock clock;

    private final TestUserHelper testUserHelper;
    private final RunCommandService runCommandService;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;

    private User user;
    private Run run;
    private Run run2;

    @BeforeEach
    void setUp() {
        clock.setFixed(ZonedDateTime.of(1990, 1, 1, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant(), DateTimeUtil.DEFAULT_ZONE_ID);

        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();

        OrderWarehouse warehouseTo = orderWarehouseGenerator.generateWarehouse();
        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(ow -> {
                                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                            new OrderWarehousePartner("123", "Склад грязи")
                                    ));
                                }))
                                .orderWarehouseTo(warehouseTo)
                                .deliveryIntervalFrom(ZonedDateTime.now(clock).minusHours(3).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.now(clock).minusHours(2).toInstant())
                                .build()
                        )
                        .orderNumber(1)
                        .build()
                )
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .externalId("234")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(ow -> {
                                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                            new OrderWarehousePartner("234", "Звенящая пошлость")
                                    ));
                                }))
                                .orderWarehouseTo(warehouseTo)
                                .deliveryIntervalFrom(ZonedDateTime.now(clock).minusHours(2).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.now(clock).minusHours(1).toInstant())
                                .build()
                        )
                        .orderNumber(2)
                        .build()
                )
                .build()
        );

        runHelper.assignUserAndTransport(run, user, transport);

        run2 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run2")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .externalId("345")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(ow -> {
                                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                            new OrderWarehousePartner("345", "Рога и копыта")
                                    ));
                                }))
                                .orderWarehouseTo(warehouseTo)
                                .deliveryIntervalFrom(ZonedDateTime.now(clock).minusHours(3).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.now(clock).minusHours(2).toInstant())
                                .build()
                        )
                        .orderNumber(1)
                        .build()
                )
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .externalId("567")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(ow -> {
                                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                            new OrderWarehousePartner("567", "Кузня Марса")
                                    ));
                                }))
                                .orderWarehouseTo(warehouseTo)
                                .deliveryIntervalFrom(ZonedDateTime.now(clock).minusHours(2).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.now(clock).minusHours(1).toInstant())
                                .build()
                        )
                        .orderNumber(2)
                        .build()
                )
                .build());
    }

    @SneakyThrows
    @Test
    void shouldReturnExcel() {
        mockMvc.perform(get("/LMS/carrier/reports/files/{date}", LocalDate.now(clock).toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @SneakyThrows
    @Disabled
    @Test
    void shouldReturnExcelAsFile() throws Exception {
        MvcResult result = mockMvc.perform(get("/LMS/carrier/reports/runs")
                .param("date", LocalDate.now(clock).toString())
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        File resultFile = new File(String.format("result-%d.xlsx", Instant.now().toEpochMilli() / 1000));
        try (FileOutputStream fos = new FileOutputStream(resultFile)) {
            IOUtils.copy(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()), fos);
        }
    }
}
