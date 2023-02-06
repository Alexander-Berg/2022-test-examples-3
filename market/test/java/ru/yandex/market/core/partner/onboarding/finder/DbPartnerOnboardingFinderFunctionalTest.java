package ru.yandex.market.core.partner.onboarding.finder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.partner.onboarding.sender.PartnerOnboardingNotificationConfigurator;
import ru.yandex.market.core.partner.onboarding.sender.PartnerOnboardingNotificationStep;
import ru.yandex.market.core.wizard.WizardService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "DbPartnerOnboardingFinderFunctionalTest.before.csv")
class DbPartnerOnboardingFinderFunctionalTest extends FunctionalTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    WizardService wizardService;

    DbPartnerOnboardingFinder finder;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    void setUp() {
        finder = new DbPartnerOnboardingFinder(
                jdbcTemplate,
                new PartnerOnboardingNotificationConfigurator(List.of(
                        new PartnerOnboardingNotificationStep(0, List.of()),
                        new PartnerOnboardingNotificationStep(5, List.of()),
                        new PartnerOnboardingNotificationStep(10, List.of())
                )),
                wizardService,
                environmentService
        );
    }

    @Test
    void findWithZeroPeriod() {
        // when
        var zeroLimit = finder.findOnboardingPartners(Instant.now());
        // then
        assertThat(zeroLimit.stream()
                .map(PartnerBeingOnboarded::getPartnerId)
                .collect(Collectors.toList())
        ).as(
                "IS_NEWBIE, те, которые подключились недавно, те, кто имеют ошибки рассчета"
        ).containsExactlyInAnyOrder(
                PartnerId.supplierId(10001L),
                PartnerId.supplierId(10002L),
                PartnerId.supplierId(10003L),
                PartnerId.supplierId(10005L),
                PartnerId.supplierId(10006L),
                PartnerId.supplierId(10009L),
                PartnerId.supplierId(10010L),
                PartnerId.supplierId(10015L),
                PartnerId.datasourceId(10012L),
                PartnerId.datasourceId(10016L)
        );
        assertThat(zeroLimit.stream()
                .map(PartnerBeingOnboarded::getPartnerCreatedAt)
                .collect(Collectors.toList())
        ).allMatch(createdAt -> !createdAt.equals(Instant.EPOCH));
    }

    @Test
    void findWithNonZeroPeriod() {
        //when
        var nonZeroLimit = finder.findOnboardingPartners(
                Instant.now().minus(Duration.ofHours(12))
        );

        //then
        assertThat(nonZeroLimit.stream()
                .map(PartnerBeingOnboarded::getPartnerId)
                .collect(Collectors.toList())
        ).as(
                "в первую очередь те, чье состояние давно не обновляли"
        ).containsExactlyInAnyOrder(
                PartnerId.supplierId(10003L), // был обновлен давно
                PartnerId.supplierId(10006L), // был обновлен давно
                PartnerId.supplierId(10009L), // вообще не был обновлен
                PartnerId.supplierId(10010L), // был недавно подключен
                PartnerId.supplierId(10015L), // имеет зафейленные шаги
                PartnerId.datasourceId(10012L), // недавно подключенный дбс
                PartnerId.datasourceId(10016L) // имеет зафейленные шаги
        );

        assertThat(nonZeroLimit.stream()
                .map(PartnerBeingOnboarded::getPartnerName)
                .collect(Collectors.toList())
        ).as(
                "Подгруженные имена поставщиков"
        ).containsExactlyInAnyOrder(
                "SUPPLIER_3",
                "SUPPLIER_6",
                "SUPPLIER_9",
                "SUPPLIER_10",
                "SUPPLIER_15",
                "SHOP_12",
                "SHOP_16"
        );

        assertThat(nonZeroLimit.stream()
                .map(PartnerBeingOnboarded::getCampaignId)
                .collect(Collectors.toList())
        ).as(
                "Подгруженные кампании поставщиков"
        ).containsExactlyInAnyOrder(
                100030L,
                100060L,
                100090L,
                100100L,
                100120L,
                100115L,
                100116L
        );
    }
}
