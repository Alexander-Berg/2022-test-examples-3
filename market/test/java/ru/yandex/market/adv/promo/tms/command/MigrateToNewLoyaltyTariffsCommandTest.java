package ru.yandex.market.adv.promo.tms.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab;
import Market.DataCamp.DataCampPromo.PromoMechanics.PartnerStandartCashback.StandartGroup;
import NMarket.Common.Promo.Promo;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.tms.command.dao.PromoYTDao;
import ru.yandex.market.adv.promo.tms.command.model.CustomCashbackPromoInfo;
import ru.yandex.market.adv.promo.tms.command.model.StandardCashbackPromoInfo;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.tms.command.MigrateToNewLoyaltyTariffsCommand.COMMAND_NAME;
import static ru.yandex.market.adv.promo.tms.command.MigrateToNewLoyaltyTariffsCommand.NEW_TARIFFS_VERSION;

class MigrateToNewLoyaltyTariffsCommandTest extends FunctionalTest {
    @Autowired
    private MigrateToNewLoyaltyTariffsCommand command;

    @Autowired
    private Terminal terminal;
    private StringWriter terminalWriter;

    @Autowired
    private PromoYTDao promoYTDao;

    @Autowired
    private DataCampClient dataCampClient;

    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        when(terminal.getWriter()).thenReturn(spy(new PrintWriter(terminalWriter)));
    }

    @Test
    void migrateStandardCashbackTest() {
        String oldTariffsWithDefaultValuesPromoId = "1234_RSC_9876543";
        String oldTariffsWithDifferentValuesPromoId = "2345_RSC_11111111";
        String oldTariffsWithNonDefaultValuesPromoId = "3456_RSC_123123123";
        mockStandardCashback(
                oldTariffsWithDefaultValuesPromoId,
                oldTariffsWithDifferentValuesPromoId,
                oldTariffsWithNonDefaultValuesPromoId
        );
        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, new String[0], Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(3)).addPromo(captor.capture());
        List<DataCampPromo.PromoDescription> updatedPromos = captor.getAllValues();
        assertEquals(3, updatedPromos.size());

        Map<String, DataCampPromo.PromoDescription> updatedPromosById = updatedPromos.stream()
                .collect(
                        Collectors.toMap(
                                promo -> promo.getPrimaryKey().getPromoId(),
                                Function.identity()
                        )
                );
        MatcherAssert.assertThat(
                updatedPromosById.keySet(),
                containsInAnyOrder(
                        oldTariffsWithDefaultValuesPromoId,
                        oldTariffsWithDifferentValuesPromoId,
                        oldTariffsWithNonDefaultValuesPromoId
                )
        );

        assertTrue(
                updatedPromosById.get(oldTariffsWithDefaultValuesPromoId)
                        .getMechanicsData().hasPartnerStandartCashback()
        );
        assertTrue(
                updatedPromosById.get(oldTariffsWithDifferentValuesPromoId)
                        .getMechanicsData().hasPartnerStandartCashback()
        );
        assertTrue(
                updatedPromosById.get(oldTariffsWithNonDefaultValuesPromoId)
                        .getMechanicsData().hasPartnerStandartCashback()
        );

        assertEquals(
                NEW_TARIFFS_VERSION,
                updatedPromosById.get(oldTariffsWithDefaultValuesPromoId)
                        .getMechanicsData().getPartnerStandartCashback().getMarketTariffsVersionId()
        );
        assertEquals(
                NEW_TARIFFS_VERSION,
                updatedPromosById.get(oldTariffsWithDifferentValuesPromoId)
                        .getMechanicsData().getPartnerStandartCashback().getMarketTariffsVersionId()
        );
        assertEquals(
                NEW_TARIFFS_VERSION,
                updatedPromosById.get(oldTariffsWithNonDefaultValuesPromoId)
                        .getMechanicsData().getPartnerStandartCashback().getMarketTariffsVersionId()
        );

        MatcherAssert.assertThat(
                updatedPromosById.get(oldTariffsWithDefaultValuesPromoId)
                        .getMechanicsData().getPartnerStandartCashback().getStandartGroupList(),
                containsInAnyOrder(
                        StandartGroup.newBuilder()
                                .setCodeName("cehac")
                                .setValue(1)
                                .build(),
                        StandartGroup.newBuilder()
                                .setCodeName("diy")
                                .setValue(2)
                                .build(),
                        StandartGroup.newBuilder()
                                .setCodeName("default")
                                .setValue(3)
                                .build()
                )
        );

        MatcherAssert.assertThat(
                updatedPromosById.get(oldTariffsWithDifferentValuesPromoId)
                        .getMechanicsData().getPartnerStandartCashback().getStandartGroupList(),
                containsInAnyOrder(
                        StandartGroup.newBuilder()
                                .setCodeName("cehac")
                                .setValue(2)
                                .build(),
                        StandartGroup.newBuilder()
                                .setCodeName("diy")
                                .setValue(2)
                                .build(),
                        StandartGroup.newBuilder()
                                .setCodeName("default")
                                .setValue(8)
                                .build()
                )
        );

        MatcherAssert.assertThat(
                updatedPromosById.get(oldTariffsWithNonDefaultValuesPromoId)
                        .getMechanicsData().getPartnerStandartCashback().getStandartGroupList(),
                containsInAnyOrder(
                        StandartGroup.newBuilder()
                                .setCodeName("cehac")
                                .setValue(3)
                                .build(),
                        StandartGroup.newBuilder()
                                .setCodeName("diy")
                                .setValue(5)
                                .build(),
                        StandartGroup.newBuilder()
                                .setCodeName("default")
                                .setValue(12)
                                .build()
                )
        );
    }

    private void mockStandardCashback(
            String oldTariffsWithDefaultValuesPromoId,
            String oldTariffsWithDifferentValuesPromoId,
            String oldTariffsWithNonDefaultValuesPromoId
    ) {
        StandardCashbackPromoInfo oldTariffsWithDefaultValues = new StandardCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1234)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId(oldTariffsWithDefaultValuesPromoId)
                        .build(),
                1234,
                0,
                Map.of(
                        "cehac", 1,
                        "diy", 3,
                        "default", 5
                )
        );
        StandardCashbackPromoInfo oldTariffsWithDifferentValues = new StandardCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(2345)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId(oldTariffsWithDifferentValuesPromoId)
                        .build(),
                2345,
                0,
                Map.of(
                        "cehac", 2,
                        "diy", 3,
                        "default", 8
                )
        );
        StandardCashbackPromoInfo oldTariffsWithNonDefaultValues = new StandardCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(3456)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId(oldTariffsWithNonDefaultValuesPromoId)
                        .build(),
                3456,
                0,
                Map.of(
                        "cehac", 3,
                        "diy", 5,
                        "default", 12
                )
        );
        StandardCashbackPromoInfo newTariffsWithDefaultValues = new StandardCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(4567)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId("4567_RSC_345234123")
                        .build(),
                4567,
                1,
                Map.of(
                        "cehac", 1,
                        "diy", 2,
                        "default", 3
                )
        );
        StandardCashbackPromoInfo newTariffsWithDifferentValues = new StandardCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(5678)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId("5678_RSC_135135")
                        .build(),
                5678,
                1,
                Map.of(
                        "cehac", 1,
                        "diy", 3,
                        "default", 5
                )
        );

        doReturn(
                List.of(
                        oldTariffsWithDefaultValues, oldTariffsWithDifferentValues, oldTariffsWithNonDefaultValues,
                        newTariffsWithDefaultValues, newTariffsWithDifferentValues
                )
        ).when(promoYTDao).getStandardCashbackPromosInfo(anyString(), anyBoolean(), eq(0), anyInt());
    }

    @Test
    void migrateCustomCashbackTest() {
        String oldTariffsPromoId = "1234_PCC_12345";
        mockCustomCashback(oldTariffsPromoId);
        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, new String[0], Collections.emptyMap());
        command.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<DataCampPromo.PromoDescription> captor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        verify(dataCampClient, times(1)).addPromo(captor.capture());
        DataCampPromo.PromoDescription updatedPromo = captor.getValue();

        assertEquals(oldTariffsPromoId, updatedPromo.getPrimaryKey().getPromoId());
        DataCampPromo.PromoMechanics.PartnerCustomCashback customCashback =
                updatedPromo.getMechanicsData().getPartnerCustomCashback();
        assertEquals(NEW_TARIFFS_VERSION, customCashback.getMarketTariffsVersionId());
        assertEquals(2, customCashback.getCashbackValue());
        assertEquals(1, customCashback.getPriority());
        assertEquals(CreationTab.FILE, customCashback.getSource());
    }

    private void mockCustomCashback(String oldTariffsPromoId) {
        CustomCashbackPromoInfo oldTariffs = new CustomCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(1234)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId(oldTariffsPromoId)
                        .build(),
                1234,
                0,
                2,
                1,
                CreationTab.FILE
        );
        CustomCashbackPromoInfo newTariffs = new CustomCashbackPromoInfo(
                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setBusinessId(2345)
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .setPromoId("2345_PCC_4556676")
                        .build(),
                2345,
                1,
                5,
                3,
                CreationTab.DYNAMIC_GROUPS
        );

        doReturn(
                List.of(oldTariffs, newTariffs)
        ).when(promoYTDao).getCustomCashbackPromosInfo(anyString(), anyBoolean(), eq(0), anyInt());
    }
}
