package ru.yandex.market.abo.core.premod.item.creator;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.autoorder.AutoOrderAccountRotationService;
import ru.yandex.market.abo.core.autoorder.code.AutoOrderCodeService;
import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.premod.PremodItemService;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.mm.model.AccountType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.premod.model.PremodItemStatus.CANCELLED;
import static ru.yandex.market.abo.core.premod.model.PremodItemStatus.NEW;
import static ru.yandex.market.abo.core.premod.model.PremodItemStatus.NEWBORN;
import static ru.yandex.market.abo.core.premod.model.PremodItemStatus.PASS;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.LOGO_CHECK;

/**
 * @author komarovns
 * @date 13.11.18
 */
class ResolveItemStatusesTest {
    private static final long TICKET_ID = 100;
    private static final long SHOP_ID = 774;

    @InjectMocks
    private MarketItemCreatorHelper marketItemCreatorHelper;
    @Mock
    private PremodItemService premodItemService;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private OfferService offerService;
    @Mock
    private AutoOrderCodeService autoOrderCodeService;
    @Mock
    private AutoOrderAccountRotationService autoOrderAccountRotationService;
    @Mock
    private ConfigurationService aboConfigurationService;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void resolveAutoOrderCheckStatus_hasException() {
        when(exceptionalShopsService.shopHasException(eq(SHOP_ID), eq(ExceptionalShopReason.AUTOORDER_EXCEPTIONS)))
                .thenReturn(true);

        assertAutoOrderStatus(CANCELLED);
    }

    @Test
    void resolveAutoOrderCheckStatus_coreConfigDisabled() {
        when(aboConfigurationService.getValueAsInt(CoreConfig.PREMOD_AUTO_ORDER.getId())).thenReturn(0);

        assertAutoOrderStatus(CANCELLED);
    }

    @Test
    void resolveAutoOrderCheckStatus_enabledForCheck() {
        when(aboConfigurationService.getValueAsInt(CoreConfig.PREMOD_AUTO_ORDER.getId())).thenReturn(1);
        when(exceptionalShopsService.shopHasException(eq(SHOP_ID), eq((ExceptionalShopReason.AUTOORDER_EXCEPTIONS))))
                .thenReturn(false);

        assertAutoOrderStatus(NEW);
    }

    @Test
    void resolveLogoCheckStatusTest_FirstCheckWithLogo() {
        var ticket = createTicket();

        when(premodItemService.loadLastCheckWithResult(SHOP_ID, LOGO_CHECK)).thenReturn(null);
        when(mbiApiService.getShopLogoUploadDate(SHOP_ID)).thenReturn(LocalDateTime.now());

        assertEquals(NEWBORN, marketItemCreatorHelper.resolveLogoCheckStatus(ticket));
    }

    @Test
    void resolveLogoCheckStatusTest_FirstCheckWithoutLogo() {
        var ticket = createTicket();

        when(premodItemService.loadLastCheckWithResult(SHOP_ID, LOGO_CHECK)).thenReturn(null);
        when(mbiApiService.getShopLogoUploadDate(SHOP_ID)).thenReturn(null);

        assertNull(marketItemCreatorHelper.resolveLogoCheckStatus(ticket));
    }

    @Test
    void resolveLogoCheckStatusTest_ChangeLogoAfterLastCheck() {
        var ticket = createTicket();

        when(mbiApiService.getShopLogoUploadDate(SHOP_ID)).thenReturn(LocalDateTime.now());
        when(premodItemService.loadLastCheckWithResult(SHOP_ID, LOGO_CHECK))
                .thenReturn(createItem(TICKET_ID - 1, LocalDateTime.now().minusDays(1)));

        assertEquals(NEWBORN, marketItemCreatorHelper.resolveLogoCheckStatus(ticket));
    }

    /**
     * Перебираем статусы фичи/кол-во офферов
     * Сравниваем с условием и джавадока {@link MarketItemCreatorHelper#resolveCutPriceCheckStatus}
     */
    @ParameterizedTest
    @MethodSource("addCutPriceCheckIfNeededTestMethodSource")
    void addCutPriceCheckIfNeededTest(int offersCount, ParamCheckStatus featureStatus) {
        when(mbiApiService.getFeatureStatus(SHOP_ID, FeatureType.CUT_PRICE)).thenReturn(featureStatus);
        when(offerService.countReportOffers(any(), any())).thenReturn(offersCount);

        PremodItemStatus status = marketItemCreatorHelper.resolveCutPriceCheckStatus(createTicket());

        if (!MarketItemCreatorHelper.DONT_CHECK_CUT_PRICE_FEATURE_STATUSES.contains(featureStatus) && offersCount > 0) {
            assertEquals(NEWBORN, status);
        } else {
            assertEquals(CANCELLED, status);
        }
    }

    static Stream<Arguments> addCutPriceCheckIfNeededTestMethodSource() {
        return StreamEx.of(0, 1)
                .cross(ParamCheckStatus.values())
                .mapKeyValue((offersCount, featureStatus) -> Arguments.of(offersCount, featureStatus));
    }

    private void assertAutoOrderStatus(PremodItemStatus expectedStatus) {
        assertEquals(expectedStatus, marketItemCreatorHelper.resolveAutoOrderItem(createTicket()));
        verify(autoOrderCodeService).addCodeForHypothesis(TICKET_ID);
        verify(autoOrderAccountRotationService).attachAccountToTicket(TICKET_ID, AccountType.AUTO_PREMOD);
    }

    private static PremodTicket createTicket() {
        var ticket = new PremodTicket(SHOP_ID, 0, PremodCheckType.CPC_PREMODERATION);
        ticket.setId(TICKET_ID);
        ticket.setShopId(SHOP_ID);
        return ticket;
    }

    private static PremodItem createItem(long ticketId, LocalDateTime modificationTime) {
        var item = new PremodItem(ticketId, PASS, LOGO_CHECK);
        item.setModificationTime(modificationTime);
        return item;
    }
}
