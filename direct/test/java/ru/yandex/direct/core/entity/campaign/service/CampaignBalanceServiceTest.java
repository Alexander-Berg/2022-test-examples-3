package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jdk.jfr.Description;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.balance.client.model.request.CreateOrUpdateOrdersBatchItem;
import ru.yandex.direct.balance.client.model.request.CreateOrUpdateOrdersBatchRequest;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.UidWithCampaignIds;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.balance.client.model.request.CreateOrUpdateOrdersBatchItem.BALANCE_MAX_ORDER_NAME_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
public class CampaignBalanceServiceTest {

    private static final long PRODUCT_ID = RandomNumberUtils.nextPositiveInteger();
    private static final long ENGINE_ID = RandomNumberUtils.nextPositiveInteger();

    @Mock
    private ProductService productService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private RbacService rbacService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private CampaignBalanceService campaignBalanceService;

    @Captor
    ArgumentCaptor<CreateOrUpdateOrdersBatchRequest> createOrUpdateOrdersBatchRequestArgumentCaptor;

    private long operatorUid;
    private long clientId;
    private long walletId;
    private long campaignId;
    private long managerUid;

    private String name;

    @Before
    public void before() {
        doReturn(true)
                .when(balanceService).createOrUpdateOrders(any());

        Product product = new Product()
                .withId(PRODUCT_ID)
                .withEngineId(ENGINE_ID);

        doReturn(product)
                .when(productService).getProductById(any());

        operatorUid = RandomNumberUtils.nextPositiveInteger();
        clientId = RandomNumberUtils.nextPositiveInteger();
        walletId = RandomNumberUtils.nextPositiveInteger();
        campaignId = RandomNumberUtils.nextPositiveInteger();
        managerUid = RandomNumberUtils.nextPositiveInteger();

        name = RandomStringUtils.randomAlphabetic(BALANCE_MAX_ORDER_NAME_LENGTH);
    }

    @Test
    public void simpleClientCreateOrUpdateOrdersOnCampCreation() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, null,
                null, List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);

        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void simpleClientCreateOrUpdateOrdersOnCampCreation_MoneyBlockedIsFalse() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, false))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, null,
                null, List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setUnmoderated(0);

        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void internalClientCreateOrUpdateOrdersOnCampCreation_UnmoderatedIsFalse() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId)
                .withType(CampaignType.INTERNAL_AUTOBUDGET);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, null,
                null, List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setUnmoderated(0);

        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void simpleClientCreateOrUpdateOrdersForWalletOnCampCreation() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);
        campaign.setType(CampaignType.WALLET);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, null,
                null, List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setIsUaOptimize("1");

        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void managerCreateOrUpdateOrdersOnCampCreation() {
        Long managerUid = (long) RandomNumberUtils.nextPositiveInteger();

        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, null,
                new UidWithCampaignIds(managerUid, List.of(campaign.getId())), List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setManagerUid(managerUid);

        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    @Description("То же самое managerCreateOrUpdateOrdersOnCampCreation но в managerUidWithServicedCampaignIds " +
            "передаётся какая-то другая кампания, в таком случае наша кампания не должна ассоциироваться с менеджером")
    public void managerCreateOrUpdateOrdersOnCampCreation_WhenAnotherCampaign() {
        Long managerUid = (long) RandomNumberUtils.nextPositiveInteger();

        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        long anotherCampaignId = campaign.getId() + 1;
        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, null,
                new UidWithCampaignIds(managerUid, List.of(anotherCampaignId)), List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setManagerUid(null);

        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void agencyCreateOrUpdateOrdersOnCampCreation() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        doReturn(List.of(managerUid))
                .when(rbacService).getManagersOfUser(anyLong());

        UidAndClientId agencyUidAndClientId = UidAndClientId.of((long) RandomNumberUtils.nextPositiveInteger(),
                ClientId.fromLong(RandomNumberUtils.nextPositiveInteger()));
        Client agencyClient =
                new Client().withId(agencyUidAndClientId.getClientId().asLong()).withPrimaryManagerUid(managerUid);
        doReturn(agencyClient)
                .when(clientService).getClient(eq(agencyUidAndClientId.getClientId()));

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, agencyUidAndClientId, null,
                List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setAgencyId(agencyUidAndClientId.getClientId().asLong());
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setManagerUid(managerUid);
        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void agencyCreateOrUpdateOrdersOnCampCreation_PreferAgencyWhenManagerPassed() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        doReturn(List.of(managerUid))
                .when(rbacService).getManagersOfUser(anyLong());

        UidAndClientId agencyUidAndClientId = UidAndClientId.of((long) RandomNumberUtils.nextPositiveInteger(),
                ClientId.fromLong(RandomNumberUtils.nextPositiveInteger()));
        Client agencyClient =
                new Client().withId(agencyUidAndClientId.getClientId().asLong()).withPrimaryManagerUid(managerUid);
        doReturn(agencyClient)
                .when(clientService).getClient(eq(agencyUidAndClientId.getClientId()));

        Long managerUid2 = (long) RandomNumberUtils.nextPositiveInteger();

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, agencyUidAndClientId,
                new UidWithCampaignIds(managerUid2, List.of(campaign.getId())),
                List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setAgencyId(agencyUidAndClientId.getClientId().asLong());
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setManagerUid(managerUid);
        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    @Test
    public void bayanAgencyCreateOrUpdateOrdersOnCampCreation() {
        CommonCampaign campaign = getCampaign(operatorUid, clientId, walletId, name, campaignId);

        doReturn(Map.of(campaignId, true))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), anyBoolean(), anyBoolean(), anyBoolean());

        doReturn(List.of(managerUid))
                .when(rbacService).getManagersOfUser(anyLong());

        UidAndClientId agencyUidAndClientId = UidAndClientId.of((long) RandomNumberUtils.nextPositiveInteger(),
                ClientId.fromLong(RandomNumberUtils.nextPositiveInteger()));
        Client agencyClient =
                new Client()
                        .withId(agencyUidAndClientId.getClientId().asLong())
                        .withIsIdmPrimaryManager(false)
                        .withPrimaryBayanManagerUid(managerUid);
        doReturn(agencyClient).when(clientService).getClient(any());

        campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(operatorUid, agencyUidAndClientId, null,
                List.of(campaign));

        verify(balanceService).createOrUpdateOrders(createOrUpdateOrdersBatchRequestArgumentCaptor.capture());

        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                getExpectedCreateOrUpdateOrdersBatchRequest(name, clientId, operatorUid, walletId, campaignId);
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setAgencyId(agencyUidAndClientId.getClientId().asLong());
        expectedCreateOrUpdateOrdersBatchRequest.getItems().get(0).setManagerUid(managerUid);
        assertThat(createOrUpdateOrdersBatchRequestArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedCreateOrUpdateOrdersBatchRequest)));
    }

    private static TextCampaign getCampaign(Long operatorUid, Long clientId, Long walletId, String name,
                                            long campaignId) {
        return new TextCampaign()
                .withId(campaignId)
                .withUid(operatorUid)
                .withAgencyId(0L)
                .withType(CampaignType.TEXT)
                .withProductId(PRODUCT_ID)
                .withClientId(clientId)
                .withName(name)
                .withWalletId(walletId)
                .withCurrency(CurrencyCode.RUB)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withSum(BigDecimal.ZERO)
                .withSumToPay(BigDecimal.ZERO);
    }

    private static CreateOrUpdateOrdersBatchRequest getExpectedCreateOrUpdateOrdersBatchRequest(
            String name,
            Long clientId,
            Long operatorUid,
            Long walletId,
            Long campaignId) {
        CreateOrUpdateOrdersBatchRequest expectedCreateOrUpdateOrdersBatchRequest =
                new CreateOrUpdateOrdersBatchRequest();
        CreateOrUpdateOrdersBatchItem item = new CreateOrUpdateOrdersBatchItem();
        item.setServiceOrderId(campaignId);
        item.setText(name);
        item.setClientId(clientId);
        item.setProductId(PRODUCT_ID);
        item.setServiceId(ENGINE_ID);
        item.setUnmoderated(1);
        item.setGroupServiceOrderId(walletId);
        expectedCreateOrUpdateOrdersBatchRequest.setOperatorUid(operatorUid);
        expectedCreateOrUpdateOrdersBatchRequest.setItems(List.of(item));
        return expectedCreateOrUpdateOrdersBatchRequest;
    }
}
