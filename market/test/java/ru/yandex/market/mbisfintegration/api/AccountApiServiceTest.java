package ru.yandex.market.mbisfintegration.api;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.QueryResult;
import ru.yandex.market.mbisfintegration.generated.sf.model.RecordType;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.generated.sf.model.SaveResult;
import ru.yandex.mj.generated.client.self_client.api.AccountApiClient;
import ru.yandex.mj.generated.client.self_client.model.Link;
import ru.yandex.mj.generated.client.self_client.model.OwPartnerDistributionType;
import ru.yandex.mj.generated.client.self_client.model.OwShop;
import ru.yandex.mj.generated.client.self_client.model.OwSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.mj.generated.client.self_client.model.OwPartnerDistributionType.DistributionTypeEnum.FBY_;

class AccountApiServiceTest extends AbstractApiTest {

    private static final Long BUSINESS_ID = 1L;
    private static final Long CAMPAIGN_ID = 2L;
    private static final Long CLIENT_ID = 3L;
    private static final Long SHOP_ID = 4L;
    private static final Long SUPPLIER_ID = 4L;
    private static final String ACCOUNT_TITLE = "Account title";
    private static final String ACCOUNT_WEBSITE = "https://ya.ru";
    private static final String REGION = "Москва";
    private static final String RESULT_ID = "123xyz";
    private static final SaveResult SAVE_RESULT = new SaveResult().withSuccess(true).withId(RESULT_ID);
    private static final String SUPPLIER_RECORD_TYPE_ID = "456supplier";
    private static final String SHOP_RECORD_TYPE_ID = "789shop";

    @Autowired
    private AccountApiClient client;

    @Override
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        Mockito.when(soap.query("SELECT Id FROM RecordType WHERE SobjectType = 'Account' AND Name = 'Supplier' LIMIT 1"))
                .thenReturn(new QueryResult().withRecords(new RecordType().withId(SUPPLIER_RECORD_TYPE_ID)));
        Mockito.when(soap.query("SELECT Id FROM RecordType WHERE SobjectType = 'Account' AND Name = 'Shop' LIMIT 1"))
                .thenReturn(new QueryResult().withRecords(new RecordType().withId(SHOP_RECORD_TYPE_ID)));
    }

    @Test
    void createShop() throws Exception {
        Mockito.when(soap.create(anyList())).thenReturn(List.of(SAVE_RESULT));
        OwShop owShop = new OwShop()
                .businessId(BUSINESS_ID)
                .campaignId(CAMPAIGN_ID)
                .clientId(CLIENT_ID)
                .shopId(SHOP_ID)
                .title(ACCOUNT_TITLE)
                .domain(new Link().href(ACCOUNT_WEBSITE).value("IGNORED"))
                .region(REGION)
                .superAdminLogin("IGNORED")
                .superAdminUid(0L) //ignored
                .usesPartnerApi(true); //ignored

        assertThat(call(client.createShop(owShop))).isEqualTo(RESULT_ID);
        Mockito.verify(soap, Mockito.times(1)).create(List.of(
                new Account()
                        .withBusinessIDC(BUSINESS_ID.toString())
                        .withCompanyIDC(CAMPAIGN_ID.toString())
                        .withClientIDC(CLIENT_ID.doubleValue())
                        .withShopIDC(SHOP_ID.doubleValue())
                        .withName(ACCOUNT_TITLE)
                        .withWebsite(ACCOUNT_WEBSITE)
                        .withMBIRegionC(REGION)
                        .withRecordTypeId(SHOP_RECORD_TYPE_ID)
        ));
    }

    @Test
    void createSupplier() throws Exception {
        Mockito.when(soap.create(anyList())).thenReturn(List.of(SAVE_RESULT));
        OwSupplier owSupplier = new OwSupplier()
                .businessId(BUSINESS_ID)
                .campaignId(CAMPAIGN_ID)
                .clientId(CLIENT_ID)
                .supplierId(SUPPLIER_ID)
                .title(ACCOUNT_TITLE)
                .login("IGNORED")
                .ordersCount(0L) //ignored
                .salesSum("IGNORED")
                .stockRemainsCount(0L) //ignored
                .stockSkuRemainsCount(0L) //ignored
                .superAdminUid(0L) //ignored
                .usesPapi(true) //ignored
                .type("IGNORED");

        assertThat(call(client.createSupplier(owSupplier))).isEqualTo(RESULT_ID);
        Mockito.verify(soap, Mockito.times(1)).create(List.of(
                new Account()
                        .withBusinessIDC(BUSINESS_ID.toString())
                        .withCompanyIDC(CAMPAIGN_ID.toString())
                        .withClientIDC(CLIENT_ID.doubleValue())
                        .withSupplierIDC(SUPPLIER_ID.doubleValue())
                        .withName(ACCOUNT_TITLE)
                        .withRecordTypeId(SUPPLIER_RECORD_TYPE_ID)
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addDistributionType() throws Exception {
        Mockito.when(soap.retrieve(anyString(), eq("Account"), anyList())).thenReturn(List.of(
                new Account().withId(RESULT_ID).withDistributionSchemesC("ADV;DBS")
        ));
        call(client.addDistributionType(
                new OwPartnerDistributionType().account(RESULT_ID).distributionType(FBY_)
        ));

        ArgumentCaptor<List<SObject>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(soap, Mockito.times(1)).update(captor.capture());
        Account account = (Account) captor.getValue().get(0);
        assertThat(account.getDistributionSchemesC().split(";"))
                .containsExactlyInAnyOrder("ADV", "DBS", "FBY_");
    }
}