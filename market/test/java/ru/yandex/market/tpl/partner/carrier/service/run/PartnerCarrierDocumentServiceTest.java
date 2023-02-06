package ru.yandex.market.tpl.partner.carrier.service.run;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PartnerCarrierDocumentServiceTest extends BaseTplPartnerCarrierWebIntTest {

    private final PartnerCarrierDocumentService partnerCarrierDocumentService;

    private final RunGenerator manualRunService;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final DsRepository dsRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final CarRequestDocRepository carRequestDocRepository;

    private Run run;
    private Company company;
    private User user1;
    private Transport transport;
    private CarRequestDoc carRequestDoc;

    @BeforeEach
    void setup() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName("ООО Иствард")
                .login("anotherLogin@yandex.ru")
                .contractId("123456")
                .contractDate(LocalDate.of(2021, 1, 1))
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID);

        transport = testUserHelper.findOrCreateTransport();

        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        OrderWarehouse warehouseTo = orderWarehouseGenerator.generateWarehouse(
                ow -> {
                    ow.setIncorporation("ФФЦ Ласт поинт");
                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                            new OrderWarehousePartner("3", "ФФЦ Ферст поинт")
                    ));
                }
        );
        OrderWarehouse warehouseTo2 = orderWarehouseGenerator.generateWarehouse(
                ow -> {
                    ow.setIncorporation("ФФЦ Ласт поинт");
                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                            new OrderWarehousePartner("4", "ФФЦ Ласт поинт")
                    ));
                }
        );
        run = manualRunService.generate(b -> b
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .name("First to second point")
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("123")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(
                                                        ow -> {
                                                            ow.setIncorporation("РЦ Ферст поинт");
                                                            ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                                                    new OrderWarehousePartner("1", "РЦ Ферст поинт")
                                                            ));
                                                        }
                                                ))
                                                .orderWarehouseTo(warehouseTo)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(64)
                                                .build(),
                                        1,
                                        1,
                                        4
                                ),
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("345")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(
                                                        ow -> {
                                                            ow.setIncorporation("РЦ Секонд поинт");
                                                            ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                                                    new OrderWarehousePartner("2", "РЦ Секонд поинт")
                                                            ));
                                                        }
                                                ))
                                                .orderWarehouseTo(warehouseTo2)
                                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                                .pallets(64)
                                                .build(),
                                        2,
                                        2,
                                        3
                                )
                        )
                )
        );


    }

    @SneakyThrows
    @Test
    void carRequest() {

        runHelper.assignUserAndTransport(run, user1, transport);

        carRequestDoc = carRequestDocRepository.findCarRequestDocByRun(run).get(0);

        byte[] actual = partnerCarrierDocumentService.getCarRequestDoc(run.getId(), carRequestDoc.getId(), company);

        Assertions.assertThat(actual).isNotEmpty();
    }

    @SneakyThrows
    @Test
    void carRequestWithNotAssignedShift() {

        carRequestDoc = carRequestDocRepository.findCarRequestDocByRun(run).get(0);

        Assertions.assertThatThrownBy(() ->
                partnerCarrierDocumentService.getCarRequestDoc(run.getId(), carRequestDoc.getId(), company)
        );

    }

    @SneakyThrows
    @Disabled
    @Test
    void carRequestWithFile() {
        runHelper.assignUserAndTransport(run, user1, transport);

        carRequestDoc = carRequestDocRepository.findCarRequestDocByRun(run).get(0);

        byte[] actual = partnerCarrierDocumentService.getCarRequestDoc(run.getId(), carRequestDoc.getId(), company);

        Files.write(Paths.get("out.pdf"), actual);

        Assertions.assertThat(actual).isNotEmpty();
    }
}
