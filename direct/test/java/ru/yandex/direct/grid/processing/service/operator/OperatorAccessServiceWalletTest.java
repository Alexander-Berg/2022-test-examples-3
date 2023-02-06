package ru.yandex.direct.grid.processing.service.operator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.model.campaign.GdWallet;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletAction;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletStatus;
import ru.yandex.direct.grid.processing.model.client.GdClientAccess;
import ru.yandex.direct.grid.processing.model.client.GdClientFeatures;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdTestClientAccess;
import ru.yandex.direct.grid.processing.model.client.GdUserInfo;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.VERIFIED;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.DYNAMIC;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.MCBANNER;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.MOBILE_CONTENT;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.PERFORMANCE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.TEXT;
import static ru.yandex.direct.grid.processing.service.client.ClientDataService.createClientInfo;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdUserInfo;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdClientAutoOverdraftInfo;
import static ru.yandex.direct.grid.processing.util.UserHelper.defaultClientNds;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class OperatorAccessServiceWalletTest extends OperatorAccessServiceBaseTest {
    private static final int TEST_SHARD = RandomNumberUtils.nextPositiveInteger(22);
    private static final ClientId TEST_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final ClientId TEST_CLIENT_ID_TWO = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final ClientId TEST_CLIENT_WITH_ACTIVE_PRICE_PACKAGE = ClientId.fromLong(
            RandomNumberUtils.nextPositiveLong());
    private static final long TEST_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long TEST_CHIEF_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long TEST_CHIEF_CLIENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final long AGENCY_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long AGENCY_CLIENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final long FREELANCER_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long WALLET_ID = RandomNumberUtils.nextPositiveLong();
    private static final long SECOND_WALLET_ID = RandomNumberUtils.nextPositiveLong();
    private static final ClientId FREELANCER_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Set<GdCampaignType> CAN_CREATE_CAMPAIGN_TYPES =
            ImmutableSet.of(TEXT, PERFORMANCE, MOBILE_CONTENT, DYNAMIC, MCBANNER);

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    static User operator() {
        return TestUsers.defaultUser()
                .withId(TEST_USER_ID)
                .withClientId(TEST_CLIENT_ID_TWO)
                .withChiefUid(TEST_CHIEF_USER_ID)
                .withUseCampDescription(false);
    }

    static GdUserInfo chiefGdInfo() {
        return toGdUserInfo(TestUsers.defaultUser()
                .withId(TEST_CHIEF_USER_ID)
                .withClientId(TEST_CLIENT_ID)
                .withChiefUid(TEST_CHIEF_USER_ID))
                .withMetrikaCountersNum(0);
    }

    private static GdClientInfo clientInfo() {
        return createClientInfo(TEST_SHARD, TestClients.defaultClient()
                        .withId(TEST_CLIENT_ID.asLong())
                        .withChiefUid(TEST_CHIEF_USER_ID),
                Map.of(TEST_CHIEF_USER_ID, chiefGdInfo()), null, Collections.emptyMap(), Collections.emptyMap(),
                defaultClientNds(TEST_CLIENT_ID.asLong()), GeoTreeType.GLOBAL,
                null, emptySet(), emptySet(), false, emptyMap(), emptyMap(), false, VERIFIED, false)
                .withAutoOverdraftInfo(defaultGdClientAutoOverdraftInfo());
    }

    private static GdClientInfo clientInfoWithActivePricePackage() {
        return createClientInfo(TEST_SHARD, TestClients.defaultClient()
                        .withId(TEST_CLIENT_WITH_ACTIVE_PRICE_PACKAGE.asLong())
                        .withChiefUid(TEST_CHIEF_USER_ID),
                Map.of(TEST_CHIEF_USER_ID, chiefGdInfo()), null, Collections.emptyMap(), Collections.emptyMap(),
                defaultClientNds(TEST_CLIENT_WITH_ACTIVE_PRICE_PACKAGE.asLong()), GeoTreeType.GLOBAL,
                null, emptySet(), emptySet(), false, emptyMap(), emptyMap(), false, VERIFIED, false);
    }

    private static GdClientFeatures features() {
        return new GdClientFeatures()
                .withIsVideoConstructorEnabled(false)
                .withIsVideoConstructorCreateFromScratchEnabled(false)
                .withIsVideoConstructorFeedEnabled(false)
                .withIsVideoConstructorBeruruTemplateEnabled(false)
                .withHasEcommerce(false)
                .withHasCampaignsWithCPAStrategy(false)
                .withHasMetrikaCounters(false)
                .withIsWalletEnabled(false)
                .withCampaignIdForVCardsManagement(null)
                .withHasCampaignsWithShows(false)
                .withHasCampaignsWithStats(false)
                .withUsedCampaignTypes(emptySet())
                .withCampaignManagers(emptySet())
                .withIsInternalAdsAllowed(false)
                .withIsCpmCampaignsEnabled(false)
                .withIsMinusWordsLibEnabled(false)
                .withIsContentPromotionVideoInGridSupported(false)
                .withIsContentPromotionCollectionInGridSupported(false)
                .withIsCpmYndxFrontpageOnGridEnabled(false)
                .withIsCpmPriceCampaignEnabled(false)
                .withIsContentPromotionVideoEnabled(false)
                .withIsClientAllowedToRemoderate(false)
                .withIsConversionCenterEnabled(false)
                .withIsCrrStrategyAllowed(false);
    }

    private static GdTestClientAccess clientAccess() {
        return new GdTestClientAccess()
                .withWalletCanEnable(false)
                .withWalletAllowPay(false)
                .withWalletIsWalletIdLinkToBalanceDisabled(true)
                .withWalletCanUseOverdraftRest(false)
                .withWalletCanUseAutopay(false)
                .withShowBalanceLink(false);
    }

    @Parameterized.Parameter()
    public String testName;

    @Parameterized.Parameter(1)
    public User operator;

    @Parameterized.Parameter(2)
    public GdClientInfo clientInfo;

    @Parameterized.Parameter(3)
    public CampaignsAndWallets campaignsAndWallets;

    @Parameterized.Parameter(4)
    public GdClientAccess expectedAccess;

    @Parameterized.Parameter(5)
    public Condition<GdClientAccess> accessCondition;

    // 1 без кошелька
    // 2 c выключенным кошельком
    // 3 включенный кошелек, разные роли
    // 4 тест на несколько кошельков
    // 5 нюансы для флагов по отдельности
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // 1 без кошелька
                {
                        "CLIENT без кошелька",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.CLIENT),
                        clientInfo(),
                        CampaignsAndWallets.empty(),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(false)
                                .withWalletIsWalletIdLinkToBalanceDisabled(true)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(false),
                        null
                },
                // 2 c выключенным кошельком
                {
                        "CLIENT с выключенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.CLIENT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withWallets(List.of(disabledWalletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(false)
                                .withWalletIsWalletIdLinkToBalanceDisabled(true)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(false),
                        null
                },
                // 3 включенный кошелек, разные роли
                {
                        "CLIENT с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.CLIENT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(true)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(false),
                        null
                },
                {
                        "SUPER с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.SUPER),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(true)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(true),
                        null
                },
                {
                        "SUPERREADER с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.SUPERREADER),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(false)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(true),
                        null
                },
                {
                        "SUPPORT с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.SUPPORT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(true)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(true),
                        null
                },
                {
                        "PLACER с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.PLACER),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(false)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(false),
                        null
                },
                {
                        "MEDIA с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.MEDIA),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(false)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(false),
                        null
                },
                {
                        "MANAGER с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.MANAGER),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(true)
                                .withWalletIsWalletIdLinkToBalanceDisabled(true)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(true),
                        null
                },
                {
                        "AGENCY с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.AGENCY),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(true)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(false),
                        null
                },
                {
                        "LIMITED_SUPPORT с включенным кошельком",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.LIMITED_SUPPORT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
                                .withWallets(List.of(walletCampaign())),
                        clientAccess()
                                .withWalletCanEnable(false)
                                .withWalletAllowPay(true)
                                .withWalletIsWalletIdLinkToBalanceDisabled(false)
                                .withWalletCanUseOverdraftRest(false)
                                .withWalletCanUseAutopay(true)
                                .withShowBalanceLink(true),
                        null
                },

                // 4 тест на несколько кошельков
                {
                        "Несколько кошельков, выбирается правильный",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.CLIENT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(
                                        defaultCampaign().withWalletId(WALLET_ID),
                                        defaultCampaign().withWalletId(SECOND_WALLET_ID)
                                ))
                                .withWallets(List.of(
                                walletCampaign()
                                        .withId(WALLET_ID)
                                        .withCurrency(CurrencyCode.YND_FIXED)
                                        .withStatus(new GdWalletStatus().withEnabled(false)),
                                walletCampaign()
                                        .withId(SECOND_WALLET_ID)
                                        .withCurrency(CurrencyCode.RUB)
                                        .withStatus(new GdWalletStatus().withEnabled(true)))),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getWalletAllowPay, "WALLET_ALLOW_PAY", true)
                        )
                },

                // TODO доделать тесты в DIRECT-115656
                // Включение ОС
                {
                        "CLIENT не может включить кошелек, если нет кампаний с подходящим типом",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.CLIENT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign().withType(CampaignType.BILLING_AGGREGATE))),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getWalletCanEnable, "WALLET_CAN_ENABLE", false),
                                propertyEqualTo(GdTestClientAccess::getWalletAllowPay, "WALLET_ALLOW_PAY", false)
                        )
                },
                {
                        "CLIENT может включить кошелек, если есть кампании с подходящим типом и нет " +
                                "кампаний-кошельков=ОС выключен",
                        operator()
                                .withUseCampDescription(true)
                                .withRole(RbacRole.CLIENT),
                        clientInfo(),
                        CampaignsAndWallets.empty()
                                .withCampaigns(List.of(defaultCampaign())),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getWalletCanEnable, "WALLET_CAN_ENABLE", true),
                                propertyEqualTo(GdTestClientAccess::getWalletAllowPay, "WALLET_ALLOW_PAY", false)
                        )
                },
//                {
//                        "CLIENT не может включить кошелек, если есть кампании-кошельки и ОС включен 1) есть
//                        кампании " +
//                                "под кошельком",
//                        operator()
//                                .withRole(RbacRole.CLIENT),
//                        clientInfo(),
//                        CampaignsAndWallets.empty()
//                                .withCampaigns(List.of(defaultCampaign().withWalletId(WALLET_ID)))
//                                .withWallets(List.of(walletCampaign())),
//                        null,
//                        allOf(
//                                propertyEqualTo(GdTestClientAccess.WALLET_CAN_ENABLE, false),
//                                propertyEqualTo(GdTestClientAccess.WALLET_ALLOW_PAY, true)
//                        )
//                },
//                {
//                        "CLIENT не может включить кошелек, если есть кампании-кошельки и ОС включен 2) " +
//                                "CanPayBeforeModeration",
//                        operator()
//                                .withRole(RbacRole.CLIENT),
//                        clientInfo(),
//                        List.of(walletCampaign()
//                                .withWalletCanPayBeforeModeration(true)),
//                        null,
//                        allOf(
//                                propertyEqualTo(GdTestClientAccess.WALLET_CAN_ENABLE, false),
//                                propertyEqualTo(GdTestClientAccess.WALLET_ALLOW_PAY, true)
//                        )
//                },
//                {
//                        "CLIENT может включить кошелек, если есть кампании с подходящим типом и есть кампании " +
//                                "кошельки, но ОС отключен",
//                        operator()
//                                .withRole(RbacRole.CLIENT),
//                        clientInfo(),
//                        List.of(defaultCampaign(),
//                                walletCampaign()
//                                        .withWalletCanPayBeforeModeration(false)),
//                        null,
//                        allOf(
//                                propertyEqualTo(GdTestClientAccess.WALLET_CAN_ENABLE, true),
//                                propertyEqualTo(GdTestClientAccess.WALLET_ALLOW_PAY, false)
//                        )
//                },

        });
    }

    private static class CampaignsAndWallets {
        List<GdWallet> wallets;
        List<GdiCampaign> campaigns;

        public static CampaignsAndWallets empty() {
            return new CampaignsAndWallets()
                    .withCampaigns(emptyList())
                    .withWallets(emptyList());
        }

        public List<GdWallet> getWallets() {
            return wallets;
        }

        public CampaignsAndWallets withWallets(List<GdWallet> wallets) {
            this.wallets = wallets;
            return this;
        }

        public List<GdiCampaign> getCampaigns() {
            return campaigns;
        }

        public CampaignsAndWallets withCampaigns(List<GdiCampaign> campaigns) {
            this.campaigns = campaigns;
            return this;
        }
    }

    @Test
    public void getAccess() {
        doReturn(emptySet())
                .when(clientService).clientIdsWithApiEnabled(any());
        doReturn(ImmutableMap.of(operator.getChiefUid(), false))
                .when(userService).getUsersAgencyDisallowMoneyTransfer(Collections.singleton(operator.getChiefUid()));
        doReturn(emptyList())
                .when(freelancerService).getFreelancers(anyCollection());
        doReturn(true)
                .when(autopayService).canUseAutopay(anyInt(), anyLong(), any());
        doReturn(true)
                .when(rbacService).isRelatedClient(FREELANCER_CLIENT_ID, TEST_CLIENT_ID);
        doReturn(Optional.empty())
                .when(rbacClientsRelations)
                .getInternalAdProductRelation(operator.getClientId(), ClientId.fromLong(clientInfo.getId()));
        doReturn(true)
                .when(rbacService).isRelatedClient(FREELANCER_CLIENT_ID, TEST_CLIENT_WITH_ACTIVE_PRICE_PACKAGE);
        doReturn(true)
                .when(rbacService).isRelatedClient(FREELANCER_CLIENT_ID, TEST_CLIENT_WITH_ACTIVE_PRICE_PACKAGE);
        doReturn(ImmutableList.builder().addAll(campaignsAndWallets.getCampaigns()).build())
                .when(campaignInfoService).getAllBaseCampaigns(any(ClientId.class));
        doReturn(campaignsAndWallets.getWallets())
                .when(campaignInfoService)
                .extractWalletsList(any(User.class), any(GdClientInfo.class), any(), any(), any());
        doReturn(features())
                .when(clientDataService)
                .getClientFeatures(any(ClientId.class), any(User.class), any());
        doReturn(new ClientLimits())
                .when(clientLimitsService)
                .getClientLimits(any(ClientId.class));
        if (operator.getRole().equals(RbacRole.CLIENT) && clientInfo.getAgencyClientId() == null) {
            doReturn(true)
                    .when(rbacService).canWrite(operator.getUid(), clientInfo.getChiefUserId());
        }
        GdClientInfo client = clientInfo.withChiefUser(new GdUserInfo()
                .withUserId(TEST_CHIEF_USER_ID)
                .withClientId(TEST_CHIEF_CLIENT_ID));
        GdClientAccess access = operatorAccessService.getAccess(
                operator, client, operator, Instant.now());
        GdTestClientAccess clientAccess = JsonUtils.fromJson(JsonUtils.toJson(access), GdTestClientAccess.class);

        if (accessCondition != null) {
            assertThat(clientAccess).is(accessCondition);
        } else {
            assertThat(clientAccess)
                    .is(matchedBy(beanDiffer(expectedAccess)
                            .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()))
                    );
        }
    }


    private static GdWallet walletCampaign() {
        return new GdWallet()
                .withId(WALLET_ID)
                .withSum(BigDecimal.valueOf(300L))
                .withActions(Set.of(GdWalletAction.PAY))
                .withStatus(new GdWalletStatus().withEnabled(true))
                .withIsAgencyWallet(false);
    }

    private static GdWallet disabledWalletCampaign() {
        return new GdWallet()
                .withId(WALLET_ID)
                .withSum(BigDecimal.valueOf(300L))
                .withActions(emptySet())
                .withStatus(new GdWalletStatus().withEnabled(false))
                .withIsAgencyWallet(false);
    }

    private static <T, M extends Model> Condition<M> propertyEqualTo(Function<M, T> property, String name, T value) {
        return new Condition<M>(
                obj -> Objects.equals(property.apply(obj), value),
                "property %s is equal to %s", name, String.valueOf(value));
    }
}
