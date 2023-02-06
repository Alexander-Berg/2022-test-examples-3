package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.model.campaign.GdiBaseCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStatusBsSynced;
import ru.yandex.direct.grid.model.campaign.GdiWalletAction;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaignUnderWallet;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultWallet;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.walletWithCanPayBeforeModeration;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class CampaignAccessServiceWalletTest {
    private static final long TEST_OPERATOR_UID = 100500;

    private static final long TEST_WALLET_ID = 1L;
    private static final long TEST_CID1 = 2L;
    private static final long TEST_CID2 = 3L;

    @InjectMocks
    private CampaignAccessHelper campaignAccessHelper;

    @Parameterized.Parameter(0)
    public String testDescription;

    @Parameterized.Parameter(1)
    public User operator;

    @Parameterized.Parameter(2)
    public GdiCampaign wallet;

    @Parameterized.Parameter(3)
    public List<GdiBaseCampaign> campaignsUnderWallet;

    @Parameterized.Parameter(4)
    public Set<GdiWalletAction> expectedActions;

    /*
        По-умолчанию все кампании под кошельком не сервисируемые, не агентские,
     промодерированы, активны, синхронизированны с БК и на них разрешена оплата.
    */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "1 default",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "2. На одной из кампаний под кошельком стоит запрет оплаты. Оплата ОС разрешена",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withNoPay(true),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "3. На всех кампаниях под кошельком стоит запрет оплаты. Оплата неразрешена ",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withNoPay(true),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withNoPay(true)),

                        ImmutableSet.of(GdiWalletAction.EDIT),
                },
                {
                        "4. Часть кампаний кампаний под кошельком не промодерирована",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "5. Все кампании под кошельком непромодерированы. Оплата ОС неразрешена",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT),
                },
                {
                        "6. Все кампании под кошельком непромодерированы. Но оператор может класть деньги (SUPER)",
                        operator(RbacRole.SUPER),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "7. Все кампании под кошельком непромодерированы. Но оператор может класть деньги (AGENCY)",
                        operator(RbacRole.AGENCY),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "8. Все кампании под кошельком непромодерированы. Но оператор может класть деньги (MANAGER)",
                        operator(RbacRole.MANAGER),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "9. Все кампании под кошельком непромодерированы. Но часть кампаний сервисируемы",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                        .withManagerUserId(123L),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "10. Все кампании под кошельком непромодерированы. Но часть кампаний агентские",
                        operator(RbacRole.CLIENT),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)
                                        .withAgencyId(777L),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "11. На всех кампаниях под кошельком стоит запрет оплаты. И все понты не помогают",
                        operator(RbacRole.SUPER),
                        defaultWallet(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withNoPay(true)
                                        .withAgencyId(777L),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withNoPay(true)),
                        ImmutableSet.of(GdiWalletAction.EDIT),
                },
                {
                        "12. На всех кампаниях под кошельком стоит запрет оплаты. (С опцией оплаты без модерации)",
                        operator(RbacRole.SUPER),
                        walletWithCanPayBeforeModeration(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withNoPay(true)
                                        .withAgencyId(777L),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withNoPay(true)),
                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },
                {
                        "13. Все кампании под кошельком непромодерированы. Оплата ОС разрешена (опция включена)",
                        operator(RbacRole.CLIENT),
                        walletWithCanPayBeforeModeration(TEST_WALLET_ID),
                        Arrays.asList(defaultCampaignUnderWallet(TEST_CID1, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO),
                                defaultCampaignUnderWallet(TEST_CID2, TEST_WALLET_ID)
                                        .withOrderId(0L)
                                        .withStatusBsSynced(GdiCampaignStatusBsSynced.NO)
                                        .withStatusModerate(CampaignStatusModerate.NO)
                                        .withStatusPostModerate(CampaignStatusPostmoderate.NO)),

                        ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY),
                },

        });
    }

    private static User operator(RbacRole role) {
        return new User()
                .withUid(TEST_OPERATOR_UID)
                .withRole(role)
                .withSuperManager(false)
                .withDeveloper(false);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void testGetActions() {

        Set<GdiWalletAction> walletActions = campaignAccessHelper.getWalletActions(
                operator, wallet, campaignsUnderWallet);

        assertThat(walletActions).is(matchedBy(beanDiffer(expectedActions)));
    }
}
