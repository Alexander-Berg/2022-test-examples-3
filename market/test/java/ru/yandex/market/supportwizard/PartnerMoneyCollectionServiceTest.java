package ru.yandex.market.supportwizard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.PartnerMoney;
import ru.yandex.market.supportwizard.base.PartnerMoneyCollection;
import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.base.ProgramType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.PartnerMoneyCollectionService;
import ru.yandex.market.supportwizard.storage.AgencyEntity;
import ru.yandex.market.supportwizard.storage.AgencyRepository;
import ru.yandex.market.supportwizard.storage.PartnerMoneyRepository;

/**
 * Тесты для {@link PartnerMoneyCollectionService}
 */
@DbUnitDataSet
public class PartnerMoneyCollectionServiceTest extends BaseFunctionalTest {
    private static final Long SHOP_A_ID = 3L;
    private static final Long SHOP_A_CAMPAIGN_ID = 245246L;
    private static final String SHOP_A_NAME = "testShopName1";
    private static final Long SHOP_A_MONEY = 1000L;
    private static final PartnerMoney SHOP_A =
            new PartnerMoney.Builder(SHOP_A_ID, PartnerType.SHOP)
                    .campaignId(SHOP_A_CAMPAIGN_ID)
                    .partnerName(SHOP_A_NAME)
                    .money(SHOP_A_MONEY)
                    .partnerPlacementProgramTypes(List.of(ProgramType.ADV))
                    .build();

    private static final Long SHOP_B_ID = 2345L;
    private static final Long SHOP_B_CAMPAIGN_ID = 103L;
    private static final String SHOP_B_NAME = "testShopName2";
    private static final Long SHOP_B_MONEY = 1100L;
    private static final PartnerMoney SHOP_B =
            new PartnerMoney.Builder(SHOP_B_ID, PartnerType.SHOP)
                    .campaignId(SHOP_B_CAMPAIGN_ID)
                    .partnerName(SHOP_B_NAME)
                    .money(SHOP_B_MONEY)
                    .partnerPlacementProgramTypes(List.of(ProgramType.DBS))
                    .build();

    private static final Long SHOP_C_ID = 2346L;
    private static final Long SHOP_C_CAMPAIGN_ID = 1030L;
    private static final String SHOP_C_NAME = "testShopName3";
    private static final Long SHOP_C_MONEY = 1200L;
    private static final PartnerMoney SHOP_C =
            new PartnerMoney.Builder(SHOP_C_ID, PartnerType.SHOP)
                    .campaignId(SHOP_C_CAMPAIGN_ID)
                    .partnerName(SHOP_C_NAME)
                    .money(SHOP_C_MONEY)
                    .partnerPlacementProgramTypes(List.of(ProgramType.ADV))
                    .build();
    private static final String AGENCY_NAME = "testAgency";

    @Autowired
    PartnerMoneyCollectionService partnerMoneyCollectionService;

    @Autowired
    AgencyRepository agencyRepository;

    @Autowired
    PartnerMoneyRepository partnerMoneyRepository;

    @Test
    void testLoadPartnerMoneyCollectionServiceFromAgency() {
        AgencyEntity agencyEntity = new AgencyEntity(1L, AGENCY_NAME,
                new HashSet<>(Set.of(SHOP_A_ID, SHOP_B_ID)));
        List<PartnerMoney> shops = List.of(SHOP_A, SHOP_B);

        agencyRepository.save(agencyEntity);
        partnerMoneyRepository.save(SHOP_A.toPartnerMoney());
        partnerMoneyRepository.save(SHOP_B.toPartnerMoney());

        PartnerMoneyCollection partnerMoneyCollection =
                partnerMoneyCollectionService.loadPartnerMoneyCollection(AGENCY_NAME).get();

        Assertions.assertEquals(new PartnerMoneyCollection(shops), partnerMoneyCollection);
        Assertions.assertEquals(SHOP_A_MONEY + SHOP_B_MONEY, partnerMoneyCollection.getSum());
    }

    @Test
    void testLoadPartnerMoneyCollectionServiceFromDuplicateAgency() {
        AgencyEntity agencyEntity = new AgencyEntity(1L, AGENCY_NAME,
                Set.of(SHOP_A_ID, SHOP_B_ID));
        AgencyEntity agencyEntity2 = new AgencyEntity(2L, AGENCY_NAME,
                Set.of(SHOP_A_ID, SHOP_C_ID));
        List<PartnerMoney> shops = List.of(SHOP_A, SHOP_C);

        partnerMoneyRepository.save(SHOP_A.toPartnerMoney());
        partnerMoneyRepository.save(SHOP_B.toPartnerMoney());
        partnerMoneyRepository.save(SHOP_C.toPartnerMoney());
        agencyRepository.save(agencyEntity);
        agencyRepository.save(agencyEntity2);

        PartnerMoneyCollection partnerMoneyCollection =
                partnerMoneyCollectionService.loadPartnerMoneyCollection(AGENCY_NAME).get();

        Assertions.assertEquals(new PartnerMoneyCollection(shops), partnerMoneyCollection);
        Assertions.assertEquals(SHOP_A_MONEY + SHOP_C_MONEY, partnerMoneyCollection.getSum());
    }
}

