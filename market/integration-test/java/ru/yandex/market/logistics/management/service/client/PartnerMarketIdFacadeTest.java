package ru.yandex.market.logistics.management.service.client;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.MarketIdStatusDto;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.facade.PartnerMarketIdFacade;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Обновление marketId партнера по ОГРН")
class PartnerMarketIdFacadeTest extends AbstractContextualTest {

    private static final long PARTNER_ID = 1L;
    private static final Partner PARTNER = new Partner()
        .setId(PARTNER_ID)
        .setName("Fulfillment without legal info")
        .setPartnerType(PartnerType.FULFILLMENT)
        .setMarketId(1L);
    private static final long NEW_MARKET_ID = 777L;
    private static final long OGRN = 123456789;

    @Autowired
    private PartnerMarketIdFacade partnerMarketIdFacade;

    @AfterEach
    void afterEachTest() {
        verifyNoMoreInteractions(marketIdService);
    }

    @Test
    @DisplayName("Попытка изменить идентификаторы на такие же")
    @DatabaseSetup("/data/service/client/update_market_id_empty.xml")
    @ExpectedDatabase(
        value = "/data/service/client/after_no_changes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noUpdate() {
        softly.assertThat(partnerMarketIdFacade.updateAllUnprocessed()).hasSize(2);
    }

    @Test
    @DatabaseSetup("/data/service/client/update_market_id_pull_push.xml")
    @DisplayName("Успешное получение идентификаторов")
    @ExpectedDatabase(
        value = "/data/service/client/after_pull.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successfulPull() {
        when(marketIdService.getMarketId(OGRN)).thenReturn(Optional.of(NEW_MARKET_ID));
        when(marketIdService.linkMarketId(PARTNER_ID, NEW_MARKET_ID)).thenReturn(marketAccount());

        softly.assertThat(partnerMarketIdFacade.updateAllUnprocessed()).hasSize(1);

        verify(marketIdService).getMarketId(OGRN);
        verify(marketIdService).linkMarketId(PARTNER_ID, NEW_MARKET_ID);
        verify(marketIdService).enrichAndConfirmLegalInfo(NEW_MARKET_ID, PARTNER);
    }

    @Test
    @DatabaseSetup("/data/service/client/update_market_id_pull_push.xml")
    @ExpectedDatabase(
        value = "/data/service/client/after_push.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный пуш идентификаторов")
    void successfulPush() {
        when(marketIdService.createAndConfirmPartnerLegalInfo(PARTNER)).thenReturn(marketAccount());

        softly.assertThat(partnerMarketIdFacade.updateAllUnprocessed()).hasSize(1);

        verify(marketIdService).getMarketId(OGRN);
        verify(marketIdService).createAndConfirmPartnerLegalInfo(PARTNER);
    }

    @Test
    @DatabaseSetup("/data/service/client/update_market_id_pull_push.xml")
    @ExpectedDatabase(
        value = "/data/service/client/after_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка проставления marketId")
    void error() {
        when(marketIdService.getMarketId(OGRN)).thenThrow(new RuntimeException("some description of message"));

        softly.assertThat(partnerMarketIdFacade.updateAllUnprocessed()).hasSize(1);

        verify(marketIdService).getMarketId(OGRN);
    }

    @Test
    @DisplayName("Партнер не существует")
    void partnerNotExist() {
        softly.assertThatThrownBy(
            () -> partnerMarketIdFacade.updateForPartner(200L),
            "Nonexistent partner id must raise EntityNotFoundException"
        );
    }

    @Test
    @DatabaseSetup("/data/service/client/update_market_id_empty.xml")
    @DisplayName("Партнер уже был обновлен")
    @ExpectedDatabase(
        value = "/data/service/client/update_market_id_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerProcessedBefore() {
        softly.assertThat(partnerMarketIdFacade.updateForPartner(3L))
            .extracting(MarketIdStatusDto::getStatus).isEqualTo("OK");
    }

    @Test
    @DisplayName("Успешный пуш идентификатора одного партнера")
    @DatabaseSetup("/data/service/client/update_market_id_pull_push.xml")
    @ExpectedDatabase(
        value = "/data/service/client/after_push.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerProcessed() {
        when(marketIdService.createAndConfirmPartnerLegalInfo(PARTNER)).thenReturn(marketAccount());

        partnerMarketIdFacade.updateForPartner(1L);

        verify(marketIdService).getMarketId(OGRN);
        verify(marketIdService).createAndConfirmPartnerLegalInfo(PARTNER);
    }

    @Nonnull
    private MarketAccount marketAccount() {
        return MarketAccount.newBuilder()
            .setMarketId(NEW_MARKET_ID)
            .build();
    }
}
