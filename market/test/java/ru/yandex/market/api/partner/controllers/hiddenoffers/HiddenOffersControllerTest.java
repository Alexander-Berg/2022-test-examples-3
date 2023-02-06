package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.api.partner.controllers.hiddenoffers.model.HiddenOfferListRestModel;
import ru.yandex.market.api.partner.controllers.hiddenoffers.model.HiddenOfferRestModel;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.core.business.migration.BusinessMigrationService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.PartnerFeedService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HiddenOffersControllerTest {
    private static final long TEST_FEED_ID = 1069L;
    private static final long TEST_CLIENT_ID = 777L;
    private static final long TEST_CAMPAIGN_ID = 10774L;

    @Mock
    private HiddenOffersController hiddenOffersController;
    @Mock
    private HiddenOffersMarketSkuController hiddenOffersMarketSkuController;
    @Mock
    private CampaignService campaignService;
    @Mock
    private BusinessMigrationService datacampBusinessMigrationService;
    @Mock
    private PartnerFeedService validationHelper;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private EnvironmentService environmentService;

    private static PartnerServletRequest createPartnerRequest(long clientId, ApiLimitType apiLimitType) {
        PartnerServletRequest partnerRequest = new PartnerServletRequest(
                mock(HttpServletRequest.class),
                Integer.MAX_VALUE
        );
        partnerRequest.initClientId(clientId);
        partnerRequest.setApiLimitType(apiLimitType);

        return partnerRequest;
    }

    private static Object[] typedOffers() {
        return Stream.of(ApiLimitType.values())
                .filter(type -> type != ApiLimitType.DEFAULT)
                .toArray(Object[]::new);
    }

    @BeforeEach
    void setUp() {
        when(campaignService.getMarketCampaign(anyLong())).thenAnswer(invocation -> {
            CampaignInfo campaignInfo = new CampaignInfo();
            campaignInfo.setDatasourceId(774);
            campaignInfo.setId(TEST_CAMPAIGN_ID);
            campaignInfo.setType(CampaignType.SHOP);
            return campaignInfo;
        });
        when(validationHelper.getPartnerFeedIds(eq(CampaignType.SHOP), anyLong()))
                .thenReturn(Collections.singleton(TEST_FEED_ID));

        when(transactionTemplate.execute(Mockito.<TransactionCallback>any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            TransactionCallback arg = (TransactionCallback) args[0];
            return arg.doInTransaction(new SimpleTransactionStatus());
        });

        when(datacampBusinessMigrationService.checkPartnerBusinessNotLocked(eq(774L)))
                .thenReturn(true);

        hiddenOffersController = new HiddenOffersController(
                campaignService,
                datacampBusinessMigrationService,
                hiddenOffersMarketSkuController,
                environmentService
        );
    }

    @ParameterizedTest
    @MethodSource("typedOffers")
    @DisplayName("Проверяет, что при типизированном запросе в качестве ID клиента в скрытых оферах используется " +
            "захардкоженый ID для корректного учета лимитов всех скрытий от price labs")
    void testPriceLabsAddHiddenOfferApiLimit(ApiLimitType type) {
        PartnerServletRequest partnerRequest = createPartnerRequest(TEST_CLIENT_ID, type);
        partnerRequest.setPartnerId(PartnerId.partnerId(774L, CampaignType.SUPPLIER));

        HiddenOfferListRestModel rootDto = new HiddenOfferListRestModel();
        HiddenOfferRestModel offerDto = new HiddenOfferRestModel();
        offerDto.setFeedId(TEST_FEED_ID);
        offerDto.setOfferId("test");
        rootDto.setHiddenOffers(Collections.singletonList(offerDto));

        ArgumentCaptor<Long> priceLabsClientIdCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.doNothing()
                .when(hiddenOffersMarketSkuController)
                .addHiddenOffers(
                        any(CampaignInfo.class),
                        any(HiddenOfferListRestModel.class),
                        priceLabsClientIdCaptor.capture(),
                        any(Instant.class)
                );


        hiddenOffersController.addHiddenOffers(
                10774L,
                rootDto,
                partnerRequest
        );

        assertThat(type.getInternalClientId(), not(0));
        assertThat(priceLabsClientIdCaptor.getValue(), equalTo(type.getInternalClientId()));
    }

    @Test
    void testInternalClientIdDefault() {
        assertThat(ApiLimitType.DEFAULT.getInternalClientId(), equalTo(0L));
    }

}
