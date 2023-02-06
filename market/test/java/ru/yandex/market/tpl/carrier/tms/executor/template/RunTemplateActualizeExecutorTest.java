package ru.yandex.market.tpl.carrier.tms.executor.template;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateItem;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@TmsIntTest
class RunTemplateActualizeExecutorTest {

    private static final LocalTime FROM_TIME = LocalTime.of(3, 30, 0, 0);
    private static final LocalTime TO_TIME = LocalTime.of(6, 0, 0, 0);
    private static final int PALLETS_CAPACITY = 33;
    private final RunTemplateActualizeExecutor runTemplateActualizeExecutor;

    private final RunTemplateGenerator runTemplateGenerator;
    private final RunTemplateRepository runTemplateRepository;
    private final LMSClient lmsClient;
    private final TestUserHelper testUserHelper;
    private final TransactionTemplate transactionTemplate;

    @SneakyThrows

    @Test
    void shouldUpdateOutdatedPoint() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        RunTemplate runTemplate = runTemplateGenerator.generate(cb -> {
                cb.campaignId(company.getCampaignId());
                cb.items(List.of(
                        new NewRunTemplateItemData(
                                "123456",
                                "234567",
                                1,
                                Set.of(DayOfWeek.MONDAY),
                                false,
                                0,
                                1,
                                FROM_TIME,
                                TO_TIME,
                                PALLETS_CAPACITY
                        )
                ));
        });

        LogisticsPointResponse notActivePoint = LogisticsPointResponse.newBuilder()
                .active(false)
                .address(Address.newBuilder()
                        .country("Россия")
                        .region("Тверь")
                        .settlement("Тверь")
                        .street("Ленина")
                        .house("1")
                        .addressString("ул. Ленина, д. 1")
                        .latitude(BigDecimal.valueOf(11.2))
                        .longitude(BigDecimal.valueOf(11.2))
                        .locationId(123)
                        .exactLocationId(123)
                        .build())
                .locationZoneId(123L)
                .partnerId(678L)
                .build();

        Mockito.when(lmsClient.getLogisticsPoint(123456L))
                .thenReturn(Optional.of(notActivePoint));

        LogisticsPointResponse activePoint = LogisticsPointResponse.newBuilder()
                .id(7890123L)
                .partnerId(678L)
                .address(Address.newBuilder()
                        .country("Россия")
                        .region("Тверь")
                        .settlement("Тверь")
                        .street("Ленина")
                        .house("1")
                        .addressString("ул. Ленина, д. 1")
                        .latitude(BigDecimal.valueOf(11.2))
                        .longitude(BigDecimal.valueOf(11.2))
                        .exactLocationId(123)
                        .build())
                .locationZoneId(120564L)
                .phones(Set.of(new Phone("", "", "", PhoneType.PRIMARY)))
                .contact(new Contact("", "", ""))
                .build();

        Mockito.when(lmsClient.getLogisticsPoints(Mockito.any(LogisticsPointFilter.class)))
                .thenReturn(List.of(activePoint));

        PartnerResponse partnerResponse = Mockito.mock(PartnerResponse.class);
        Mockito.when(partnerResponse.getName())
                .thenReturn("aaaa");

        Mockito.when(lmsClient.getPartner(678L))
                .thenReturn(Optional.of(partnerResponse));

        runTemplateActualizeExecutor.doRealJob(null);

        transactionTemplate.execute(tc -> {
            RunTemplate runTemplateNew = runTemplateRepository.findByIdOrThrow(runTemplate.getId());
            RunTemplateItem runTemplateItem = runTemplateNew.streamItems().findFirst().orElseThrow();

            Assertions.assertThat(runTemplateItem.getWarehouseYandexIdFrom())
                    .isEqualTo("7890123");
            Assertions.assertThat(runTemplateItem.getFromTime())
                    .isEqualTo(FROM_TIME);
            Assertions.assertThat(runTemplateItem.getToTime())
                    .isEqualTo(TO_TIME);
            Assertions.assertThat(runTemplateItem.getPalletsCapacity())
                    .isEqualTo(PALLETS_CAPACITY);
            return null;
        });
    }
}
