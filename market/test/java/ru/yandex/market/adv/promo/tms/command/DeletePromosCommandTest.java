package ru.yandex.market.adv.promo.tms.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.tms.command.DeletePromosCommand.COMMAND_NAME;
import static ru.yandex.market.adv.promo.utils.CashbackMechanicTestUtils.createStandardCashback;

public class DeletePromosCommandTest extends FunctionalTest {
    @Autowired
    DeletePromosCommand deletePromosCommand;
    @Autowired
    private Terminal terminal;
    @Autowired
    private DataCampClient dataCampClient;
    private StringWriter terminalWriter;

    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        when(terminal.getWriter()).thenReturn(spy(new PrintWriter(terminalWriter)));
    }

    @DisplayName("Тест на то, что в Акционное Хранилище отправляется корректный запрос.")
    @Test
    void successfulDeletedPromoTest() {
        String promoId = "123_PROMOCODE";
        int source = 2;
        int businessId = 2;
        Map<String, String> options = Map.of(
                "single", "",
                "business_id", String.valueOf(businessId),
                "source", String.valueOf(source),
                "promo_id", promoId
        );
        doReturn(SyncGetPromo.DeletePromoBatchResponse.newBuilder().build()).when(dataCampClient).deletePromo(any());
        CommandInvocation commandInvocation =
                new CommandInvocation(COMMAND_NAME, new String[0], options);
        deletePromosCommand.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<SyncGetPromo.DeletePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.DeletePromoBatchRequest.class);
        verify(dataCampClient, times(1)).deletePromo(captor.capture());
        SyncGetPromo.DeletePromoBatchRequest expectedRequest = SyncGetPromo.DeletePromoBatchRequest.newBuilder()
                .addIdentifiers(SyncGetPromo.PromoIdentifiers.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(businessId)
                                .setSource(Promo.ESourceType.forNumber(source))
                                .setPromoId(promoId)
                        )
                )
                .build();
        assertThat(captor.getValue()).isEqualTo(expectedRequest);

        String expectedTerminalData = "Promo was successfully deleted.";
        assertThat(StringUtils.trimToEmpty(terminalWriter.toString())).isEqualTo(expectedTerminalData);
    }

    @DisplayName("" +
            "Тест на то, что если у партнёра единственная акция со стандартным кешбеком, то с ней ничего " +
            "делать не надо."
    )
    @Test
    void deleteStandardCashback_noDuplicates() {
        int partnerId = 1;
        int businessId = 123;
        String promoId = "1_PSC_51231451";
        Map<String, String> options = Map.of(
                "partner_standard_cashback", "",
                "business_id", String.valueOf(businessId),
                "partner_id", String.valueOf(partnerId)
        );
        CommandInvocation commandInvocation = new CommandInvocation(COMMAND_NAME, new String[0], options);

        SyncGetPromo.GetPromoBatchResponse response = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(createStandardCashback(promoId, businessId))
                )
                .build();
        doReturn(response).when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        deletePromosCommand.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<SyncGetPromo.DeletePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.DeletePromoBatchRequest.class);
        verify(dataCampClient, times(0)).deletePromo(captor.capture());

        String expectedTerminalData = "Partner has no standard cashbacks duplicates.";
        assertThat(StringUtils.trimToEmpty(terminalWriter.toString())).isEqualTo(expectedTerminalData);
    }

    @DisplayName("Тест на то, что если у партнёра нашлись акции с разными параметрами, то команда остановится.")
    @Test
    void deleteStandardCashback_incorrectDuplicates() {
        int partnerId = 1;
        int businessId = 123;
        String promoId1 = "1_PSC_51231451";
        String promoId2 = "1_PSC_51231451_I_hate_migrations";
        Map<String, String> options = Map.of(
                "partner_standard_cashback", "",
                "business_id", String.valueOf(businessId),
                "partner_id", String.valueOf(partnerId)
        );
        CommandInvocation commandInvocation = new CommandInvocation(COMMAND_NAME, new String[0], options);

        SyncGetPromo.GetPromoBatchResponse response = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(createStandardCashback(promoId1, businessId, 1, 1, 3, 5, false))
                        .addPromo(createStandardCashback(promoId2, businessId, 1, 1, 3, 5, true))
                )
                .build();
        doReturn(response).when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        deletePromosCommand.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<SyncGetPromo.DeletePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.DeletePromoBatchRequest.class);
        verify(dataCampClient, times(0)).deletePromo(captor.capture());

        String expectedTerminalData = "Can not delete promos. Partner has promos with different parameters.";
        assertThat(StringUtils.trimToEmpty(terminalWriter.toString())).isEqualTo(expectedTerminalData);
    }

    @DisplayName("Тест на то, что у партнёра есть несколько одинаковых кешбеков, из которых удалятся все, кроме одного")
    @Test
    void deleteStandardCashback_correctOperation() {
        int partnerId = 1;
        int businessId = 123;
        String promoId1 = "1_PSC_51231451";
        String promoId2 = "1_PSC_51231451_I_hate_migrations";
        String promoId3 = "1_PSC_51231451_yet_another_cashback";
        String promoId4 = "1_PSC_51231451_oh_stop_it_you";
        Map<String, String> options = Map.of(
                "partner_standard_cashback", "",
                "business_id", String.valueOf(businessId),
                "partner_id", String.valueOf(partnerId)
        );
        CommandInvocation commandInvocation = new CommandInvocation(COMMAND_NAME, new String[0], options);

        SyncGetPromo.GetPromoBatchResponse response = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(createStandardCashback(promoId1, businessId))
                        .addPromo(createStandardCashback(promoId2, businessId))
                        .addPromo(createStandardCashback(promoId3, businessId))
                        .addPromo(createStandardCashback(promoId4, businessId))
                )
                .build();
        doReturn(response).when(dataCampClient).getPromos(any(PromoDatacampRequest.class));
        doReturn(SyncGetPromo.DeletePromoBatchResponse.newBuilder().build()).when(dataCampClient).deletePromo(any());
        deletePromosCommand.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<SyncGetPromo.DeletePromoBatchRequest> captor =
                ArgumentCaptor.forClass(SyncGetPromo.DeletePromoBatchRequest.class);
        verify(dataCampClient, times(1)).deletePromo(captor.capture());

        SyncGetPromo.DeletePromoBatchRequest expectedRequest = SyncGetPromo.DeletePromoBatchRequest.newBuilder()
                .addIdentifiers(SyncGetPromo.PromoIdentifiers.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(businessId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .setPromoId(promoId2)
                        )
                )
                .addIdentifiers(SyncGetPromo.PromoIdentifiers.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(businessId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .setPromoId(promoId3)
                        )
                )
                .addIdentifiers(SyncGetPromo.PromoIdentifiers.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(businessId)
                                .setSource(Promo.ESourceType.PARTNER_SOURCE)
                                .setPromoId(promoId4)
                        )
                )
                .build();
        assertThat(captor.getValue()).isEqualTo(expectedRequest);

        String expectedTerminalData = "Promos was successfully deleted.";
        assertThat(StringUtils.trimToEmpty(terminalWriter.toString())).isEqualTo(expectedTerminalData);
    }
}
