package ru.yandex.market.fps.module.campaign.test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.core.marketing.MarketingCampaignType;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.balance.BalanceService;
import ru.yandex.market.fps.balance.ContractService;
import ru.yandex.market.fps.balance.model.ClientContractInfo;
import ru.yandex.market.fps.balance.model.ClientContractInfoDto;
import ru.yandex.market.fps.balance.model.ClientContractInfoDtoResult;
import ru.yandex.market.fps.balance.model.ClientContractOfferListDto;
import ru.yandex.market.fps.module.campaign.Campaign;
import ru.yandex.market.fps.module.campaign.CampaignBusinessModel;
import ru.yandex.market.fps.module.campaign.ComplexCampaign;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.fps.module.vendor.partner.VendorPartnerClient;
import ru.yandex.market.fps.module.vendor.partner.model.Brand;
import ru.yandex.market.fps.ticket.Supplier1p;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.test.time.FixedTime;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.marketing.MarketingCampaignParamsDTO;
import ru.yandex.market.mbi.api.client.entity.marketing.MarketingCampaignTypeParamDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fps.balance.BalanceService.MARKET_BILLING_SERVICE_NUM;

@Transactional
@SpringJUnitConfig(InternalModuleCampaignTestConfiguration.class)
@FixedTime("13.06.2022 13:24:57")
public class AbstractComplexCampaignTest {
    protected static final String PROMO_CONTENT = "PROMO_CONTENT";
    protected static final String PROMO_TV = "PROMO_TV";
    protected static final String MARKET_COUPON = "MARKET_COUPON";

    protected static final Long EMPLOYEE_UID = 1111L;

    @Inject
    private SupplierTestUtils supplierTestUtils;
    @Inject
    protected BcpService bcpService;
    @Inject
    protected EntityStorageService entityStorageService;
    @Inject
    protected VendorPartnerClient vendorPartnerClient;
    @Inject
    protected MbiApiClient mbiApiClient;
    @Inject
    protected ContractService balanceContractService;
    @Inject
    protected BalanceService balanceService;

    @Inject
    protected OuTestUtils ouTestUtils;
    @Inject
    protected EmployeeTestUtils employeeTestUtils;
    @Inject
    protected MockSecurityDataService mockSecurityDataService;

    protected Supplier1p supplier1p;
    protected CampaignBusinessModel businessModel;

    @BeforeEach
    void setUp() {
        supplier1p = supplierTestUtils.createSupplier();
        var currentEmployee = employeeTestUtils.createEmployee(Randoms.string(), ouTestUtils.createOu(), EMPLOYEE_UID);
        mockSecurityDataService.setCurrentEmployee(currentEmployee);

        businessModel = entityStorageService.getByNaturalId(CampaignBusinessModel.FQN, "SELF");

        Brand brand = new Brand(10662867L, "Test Brand");
        List<Brand> brands = List.of(brand);

        when(mbiApiClient.getMarketingCampaignParams()).thenReturn(createMarketingCampaignParamsDTO());

        doReturn(createClientContractInfo()).when(balanceService).getClientContracts(anyInt(),
                any(ContractType.class), any(Date.class), anyString(), anyBoolean(), anyBoolean(), anyInt());
        doReturn(createClientContractOfferListDto()).when(balanceContractService).getContracts(anyInt(), anyLong(),
                anyString(), anyInt(), anyBoolean(), anyBoolean(), eq(ContractType.GENERAL));
        doReturn(createClientContractOfferListDto()).when(balanceContractService).getContracts(anyInt(),
                eq(ContractType.GENERAL));

        doReturn(brands).when(vendorPartnerClient).getBrands(anyInt(), anyInt(), anyString());
        doReturn(brand).when(vendorPartnerClient).getBrand(anyInt());
    }

    @AfterEach
    public void tearDown() {
        mockSecurityDataService.reset();
    }

    @NotNull
    protected Map<String, Object> buildComplexParams(Object start, Object end) {
        return Maps.of(
                ComplexCampaign.TITLE, "Test complex campaign",
                ComplexCampaign.DATE_START, start,
                ComplexCampaign.DATE_END, end,
                ComplexCampaign.APPROVED_BY_MANAGER_UID, EMPLOYEE_UID,
                ComplexCampaign.SUPPLIER, supplier1p.getGid()
        );
    }

    @NotNull
    protected Map<String, Object> buildCampaignParams() {
        return Maps.of(
                Campaign.TITLE, "Test campaign",
                Campaign.BRANDS, Set.of(10662867),
                Campaign.CATEGORIES, Set.of(456),
                Campaign.BUSINESS_MODEL, "SELF",
                Campaign.TYPE, PROMO_CONTENT,
                Campaign.SUM, 30_000,
                Campaign.APPROVED_BY_MANAGER_UID, EMPLOYEE_UID
        );
    }

    @NotNull
    protected Map<String, Object> buildCompensationalCampaignParams() {
        return Maps.of(
                Campaign.TITLE, "Test campaign",
                Campaign.BRANDS, Set.of(10662867),
                Campaign.CATEGORIES, Set.of(456),
                Campaign.BUSINESS_MODEL, "SELF",
                Campaign.TYPE, MARKET_COUPON,
                Campaign.SUM, 30_000,
                Campaign.APPROVED_BY_MANAGER_UID, EMPLOYEE_UID
        );
    }

    @NotNull
    protected Campaign createCampaign(ComplexCampaign complex, Map<String, Object> params, Map<String, Object> config) {
        var campaignParams = buildCampaignParams();
        campaignParams.put(Campaign.COMPLEX_CAMPAIGN, complex.getGid());
        campaignParams.putAll(params);

        return bcpService.create(Campaign.FQN, campaignParams, config);
    }

    @NotNull
    protected Campaign createCompensationalCampaign(ComplexCampaign complex, Map<String, Object> params, Map<String,
            Object> config) {
        var campaignParams = buildCompensationalCampaignParams();
        campaignParams.put(Campaign.COMPLEX_CAMPAIGN, complex.getGid());
        campaignParams.putAll(params);

        return bcpService.create(Campaign.FQN, campaignParams, config);
    }

    @NotNull
    protected MarketingCampaignParamsDTO createMarketingCampaignParamsDTO() {
        MarketingCampaignTypeParamDTO nonRequireAnaplan = new MarketingCampaignTypeParamDTO(false);
        MarketingCampaignTypeParamDTO requireAnaplan = new MarketingCampaignTypeParamDTO(true);
        Map<MarketingCampaignType, MarketingCampaignTypeParamDTO> campaignTypes =
                Map.of(
                        MarketingCampaignType.valueOf(PROMO_CONTENT), nonRequireAnaplan,
                        MarketingCampaignType.valueOf(PROMO_TV), nonRequireAnaplan,
                        MarketingCampaignType.valueOf(MARKET_COUPON), requireAnaplan
                );

        return new MarketingCampaignParamsDTO(campaignTypes);
    }

    @NotNull
    private ClientContractOfferListDto createClientContractOfferListDto() {
        var clientContractInfoDtoResult = new ClientContractInfoDtoResult();
        clientContractInfoDtoResult.setIsActive(true);
        clientContractInfoDtoResult.setServices(List.of(MARKET_BILLING_SERVICE_NUM));

        var clientContractInfoDto = new ClientContractInfoDto();
        clientContractInfoDto.result(clientContractInfoDtoResult);

        return new ClientContractOfferListDto().result(List.of(clientContractInfoDto));
    }

    @NotNull
    private List<ClientContractInfo> createClientContractInfo() {
        ClientContractInfo info = new ClientContractInfo.ClientContractInfoBuilder()
                .isActive(true)
                .withServices(new int[]{MARKET_BILLING_SERVICE_NUM})
                .build();
        return List.of(info);
    }

    @NotNull
    protected ComplexCampaign createComplexCampaign(Map<String, Object> params, Map<String, Object> config) {
        LocalDate start = Now.localDate().plusDays(1);
        LocalDate end = Now.localDate().plusDays(2);

        var complexParams = Maps.merge(
                buildComplexParams(start, end),
                params
        );

        return bcpService.create(ComplexCampaign.FQN, complexParams, config);
    }
}
