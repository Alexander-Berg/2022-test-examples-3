package ru.yandex.direct.core.testing.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.jooq.DSLContext;
import org.jooq.util.mysql.MySQLDSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageMinBid;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageBidCurrency;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.currency.repository.CpmYndxFrontpageMinBidsRepository;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignsCpmYndxFrontpageRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.tables.CampaignsCpmYndxFrontpage.CAMPAIGNS_CPM_YNDX_FRONTPAGE;
import static ru.yandex.direct.dbschema.ppcdict.tables.CpmYndxFrontpageMinBids.CPM_YNDX_FRONTPAGE_MIN_BIDS;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

/**
 * Репозиторий для работы с группой типа CPM_YNDX_FRONTPAGE в тестах
 * На данный момент здесь
 * 1) Заполнение таблицы ppcdict.cpm_yndx_frontpage_min_bids
 * фиктивными значениями для юнит-тестов на валидацию ставки для показа на главной
 * 2) Заполнение таблицы ppc.campaigns_cpm_yndx_frontpage значениями типа показа кампаний (десктоп/мобильная)
 */
@Component
public class TestCpmYndxFrontpageRepository {
    public static final List<CpmYndxFrontpageMinBid> DEFAULT_BIDS_FOR_TEST = ImmutableList.of(
            getCpmYndxFrontpageMinBidWithChfMobile(RUSSIA_REGION_ID, .7),
            getCpmYndxFrontpageMinBidWithChfDesktop(RUSSIA_REGION_ID, 1.),
            getCpmYndxFrontpageMinBidWithChfBrowserNewTab(RUSSIA_REGION_ID, 1.),
            getCpmYndxFrontpageMinBidWithChfMobile(UKRAINE_REGION_ID, .7),
            getCpmYndxFrontpageMinBidWithChfDesktop(UKRAINE_REGION_ID, .9),
            getCpmYndxFrontpageMinBidWithChfBrowserNewTab(UKRAINE_REGION_ID, .9),
            getCpmYndxFrontpageMinBidWithChfMobile(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, 1.3),
            getCpmYndxFrontpageMinBidWithChfDesktop(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, 1.5),
            getCpmYndxFrontpageMinBidWithChfBrowserNewTab(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, 1.5),
            getCpmYndxFrontpageMinBidWithChfMobile(MOSCOW_REGION_ID, 0.0001),
            getCpmYndxFrontpageMinBidWithChfDesktop(MOSCOW_REGION_ID, 2.5),
            getCpmYndxFrontpageMinBidWithChfBrowserNewTab(MOSCOW_REGION_ID, 2.5),
            getCpmYndxFrontpageMinBidWithChfMobile(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, 1.1),
            getCpmYndxFrontpageMinBidWithChfDesktop(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, 1.2),
            getCpmYndxFrontpageMinBidWithChfBrowserNewTab(SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, 4.2));


    private final DslContextProvider dslContextProvider;
    private final JooqMapperWithSupplier<CpmYndxFrontpageMinBid> frontpageMinBidReadWriteMapper;

    @Autowired
    public TestCpmYndxFrontpageRepository(
            DslContextProvider dslContextProvider,
            CpmYndxFrontpageMinBidsRepository cpmYndxFrontpageMinBidsRepository
    ) {
        this.dslContextProvider = dslContextProvider;
        frontpageMinBidReadWriteMapper = cpmYndxFrontpageMinBidsRepository.frontpageMinBidReadWriteMapper;
    }

    public void fillMinBidsTestValues() {
        insertMinBids(dslContextProvider.ppcdict(), DEFAULT_BIDS_FOR_TEST);
    }

    public void insertMinBids(DSLContext context, List<CpmYndxFrontpageMinBid> minBids) {
        new InsertHelper<>(context, CPM_YNDX_FRONTPAGE_MIN_BIDS)
                .addAll(frontpageMinBidReadWriteMapper, minBids)
                .onDuplicateKeyUpdate()
                .set(CPM_YNDX_FRONTPAGE_MIN_BIDS.MIN_BID, MySQLDSL.values(CPM_YNDX_FRONTPAGE_MIN_BIDS.MIN_BID))
                .execute();
    }

    public void setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(int shard,
                                                                  Long campaignId, Collection<FrontpageCampaignShowType> allowedTypes) {
        String alllowedTypesDbValue = allowedTypes.stream()
                .map(FrontpageCampaignShowType::toSource)
                .map(Enum::toString)
                .collect(Collectors.joining(","));
        InsertHelper<CampaignsCpmYndxFrontpageRecord> insertHelper =
                new InsertHelper<>(dslContextProvider.ppc(shard), CAMPAIGNS_CPM_YNDX_FRONTPAGE);
        insertHelper
                .set(CAMPAIGNS_CPM_YNDX_FRONTPAGE.CID, campaignId)
                .set(CAMPAIGNS_CPM_YNDX_FRONTPAGE.ALLOWED_FRONTPAGE_TYPES, alllowedTypesDbValue)
                .newRecord();
        insertHelper
                .onDuplicateKeyUpdate()
                .set(CAMPAIGNS_CPM_YNDX_FRONTPAGE.ALLOWED_FRONTPAGE_TYPES,
                        MySQLDSL.values(CAMPAIGNS_CPM_YNDX_FRONTPAGE.ALLOWED_FRONTPAGE_TYPES));
        insertHelper.execute();
    }

    private static CpmYndxFrontpageMinBid getCpmYndxFrontpageMinBidWithChfMobile(Long regionId, Double minBid) {
        return new CpmYndxFrontpageMinBid()
                .withMinBid(BigDecimal.valueOf(minBid))
                .withFrontpageCampaignShowType(FrontpageCampaignShowType.FRONTPAGE_MOBILE)
                .withRegionId(regionId)
                .withFrontpageBidCurrency(FrontpageBidCurrency.CHF);
    }

    private static CpmYndxFrontpageMinBid getCpmYndxFrontpageMinBidWithChfDesktop(Long regionId, Double minBid) {
        return new CpmYndxFrontpageMinBid()
                .withMinBid(BigDecimal.valueOf(minBid))
                .withFrontpageCampaignShowType(FrontpageCampaignShowType.FRONTPAGE)
                .withRegionId(regionId)
                .withFrontpageBidCurrency(FrontpageBidCurrency.CHF);
    }

    private static CpmYndxFrontpageMinBid getCpmYndxFrontpageMinBidWithChfBrowserNewTab(Long regionId, Double minBid) {
        return new CpmYndxFrontpageMinBid()
                .withMinBid(BigDecimal.valueOf(minBid))
                .withFrontpageCampaignShowType(FrontpageCampaignShowType.BROWSER_NEW_TAB)
                .withRegionId(regionId)
                .withFrontpageBidCurrency(FrontpageBidCurrency.CHF);
    }

}
