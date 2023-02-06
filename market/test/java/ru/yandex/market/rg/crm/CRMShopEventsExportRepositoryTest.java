package ru.yandex.market.rg.crm;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестирует {@link CRMShopEventsExportRepository}.
 *
 * @author Vadim Lyalin
 */
class CRMShopEventsExportRepositoryTest extends FunctionalTest {
    @Autowired
    private CRMShopEventsExportRepository crmShopEventsExportRepository;

    @Test
    @DbUnitDataSet(before = "shopEvents.before.csv")
    void test() {
        List<CrmEventInfo> crmEventInfos = new ArrayList<>();
        crmShopEventsExportRepository.provideDataTo(itr -> {
            while (itr.hasNext()) {
                CrmEventInfo info = itr.next();
                crmEventInfos.add(info);
            }
        });

        CrmEventInfo crmEventInfo1 = new CrmEventInfo();
        crmEventInfo1.setId(1);
        crmEventInfo1.setEventType(CRMEventType.SHOP_CREATED);
        PartnerInfo partnerInfo1 = new PartnerInfo();
        partnerInfo1.setPartnerId(1);
        partnerInfo1.setBusinessId(10L);
        partnerInfo1.setBusinessName("business");
        partnerInfo1.setCreatedAt(
                LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        crmEventInfo1.setPartnerInfo(partnerInfo1);

        CrmEventInfo crmEventInfo2 = new CrmEventInfo();
        crmEventInfo2.setId(2);
        crmEventInfo2.setEventType(CRMEventType.JURIDICAL_INFO_ADDED);
        PartnerInfo partnerInfo2 = new PartnerInfo();
        partnerInfo2.setPartnerId(2);
        partnerInfo2.setCreatedAt(
                LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        crmEventInfo2.setPartnerInfo(partnerInfo2);

        crmEventInfos.sort(Comparator.comparingLong(CrmEventInfo::getId));

        assertThat(crmEventInfos)
                .usingRecursiveComparison()
                .ignoringFields("eventTime")
                .isEqualTo(List.of(crmEventInfo1, crmEventInfo2));
    }
}
