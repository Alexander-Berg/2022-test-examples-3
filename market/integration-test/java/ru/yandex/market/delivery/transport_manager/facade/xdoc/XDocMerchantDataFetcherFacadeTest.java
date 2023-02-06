package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.enums.XDocMerchantType;
import ru.yandex.market.delivery.transport_manager.domain.enums.XDocToDcMarketSchemeReason;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocMerchantData;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

@DatabaseSetup(
    value = {
        "/repository/register/registers_with_contractor.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml"
    }
)
@DatabaseSetup(
    value = "/repository/transportation/update/register_relations.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
public class XDocMerchantDataFetcherFacadeTest extends AbstractContextualTest {
    @Autowired
    private XDocMerchantDataFetcherFacade facade;

    @Autowired
    private LMSClient lmsClient;

    @Test
    void test3p() {
        XDocMerchantData merchantData = facade.getMerchantData(1L, false, 10L);
        softly.assertThat(merchantData).isEqualTo(
            new XDocMerchantData(XDocMerchantType.MARKET, XDocToDcMarketSchemeReason.THIRD_PARTY)
        );
    }

    @Test
    void test1p() {
        XDocMerchantData merchantData = facade.getMerchantData(3L, true, 10L);
        softly.assertThat(merchantData).isEqualTo(
            new XDocMerchantData(
                XDocMerchantType.MARKET,
                XDocToDcMarketSchemeReason.NO_SINGLE_REAL_SUPPLIER_ID,
                null,
                null,
                0
            )
        );

        merchantData = facade.getMerchantData(1L, true, 10L);
        softly.assertThat(merchantData).isEqualTo(
            new XDocMerchantData(
                XDocMerchantType.MARKET,
                XDocToDcMarketSchemeReason.NO_LMS_PARTNER,
                null,
                null,
                0
            )
        );

        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setRealSupplierIds(Set.of("c1")).build()))
            .thenReturn(List.of(PartnerResponse.newBuilder().id(5L).build()));

        merchantData = facade.getMerchantData(1L, true, 10L);
        softly.assertThat(merchantData).isEqualTo(
            new XDocMerchantData(
                XDocMerchantType.MARKET,
                XDocToDcMarketSchemeReason.NO_ACTIVE_WAREHOUSE,
                5L,
                null,
                0
            )
        );

        Mockito.when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(5L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        )).thenReturn(List.of(LogisticsPointResponse.newBuilder().id(10L).build()));

        merchantData = facade.getMerchantData(1L, true, 10L);
        softly.assertThat(merchantData).isEqualTo(
            new XDocMerchantData(
                XDocMerchantType.MARKET,
                XDocToDcMarketSchemeReason.NO_ACTIVE_RELATION_WITH_DC,
                5L,
                10L,
                0
            )
        );

        Mockito.when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(5L))
                .toPartnersIds(Set.of(10L))
                .build(),
            null
            )
        ).thenReturn(new PageResult<PartnerRelationEntityDto>().setData(
            List.of(PartnerRelationEntityDto.newBuilder().enabled(true).id(1L).build()))
        );

        merchantData = facade.getMerchantData(1L, true, 10L);
        softly.assertThat(merchantData).isEqualTo(
            new XDocMerchantData(
                XDocMerchantType.LMS,
                XDocToDcMarketSchemeReason.NONE,
                5L,
                10L,
                0
            )
        );

    }
}
