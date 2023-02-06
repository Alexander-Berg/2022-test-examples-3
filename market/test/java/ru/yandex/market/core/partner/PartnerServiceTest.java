package ru.yandex.market.core.partner;

import java.util.EnumSet;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.partner.model.ModelManagementAccess;
import ru.yandex.market.core.partner.model.PartnerInfo;
import ru.yandex.market.core.partner.model.PartnerSearchInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.campaign.model.PartnerId.datasourceId;
import static ru.yandex.market.core.campaign.model.PartnerId.supplierId;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "PartnerServiceTest.csv")
class PartnerServiceTest extends FunctionalTest {
    @Autowired
    PartnerService partnerService;

    @Test
    void testAddNew() {
        assertThat(partnerService.isModelManager(776, ModelManagementAccess.MANUAL)).isFalse();
        partnerService.addModelManagerShop(776, EnumSet.of(ModelManagementAccess.MANUAL));
        assertThat(partnerService.isModelManager(776, ModelManagementAccess.MANUAL)).isTrue();
    }

    @Test
    void testAddExisting() {
        assertThat(partnerService.isModelManager(774, ModelManagementAccess.MANUAL)).isTrue();
        partnerService.addModelManagerShop(774, EnumSet.of(ModelManagementAccess.MANUAL));
        assertThat(partnerService.isModelManager(774, ModelManagementAccess.MANUAL)).isTrue();
    }

    @Test
    void testRemoveExisting() {
        assertThat(partnerService.isModelManager(775, ModelManagementAccess.MANUAL)).isTrue();
        partnerService.removeModelManagerShop(775, EnumSet.of(ModelManagementAccess.MANUAL));
        assertThat(partnerService.isModelManager(775, ModelManagementAccess.MANUAL)).isFalse();
    }

    @Test
    void testMixedAccess() {
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isFalse();
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.BATCH)).isTrue();

        partnerService.addModelManagerShop(777, EnumSet.of(ModelManagementAccess.MANUAL));

        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isTrue();
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.BATCH)).isTrue();

        partnerService.addModelManagerShop(777, EnumSet.of(ModelManagementAccess.BATCH));

        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isTrue();
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.BATCH)).isTrue();

        partnerService.removeModelManagerShop(777, EnumSet.of(ModelManagementAccess.MANUAL));

        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isFalse();
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.BATCH)).isTrue();

        partnerService.removeModelManagerShop(777, EnumSet.of(ModelManagementAccess.BATCH));

        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isFalse();
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.BATCH)).isTrue();
    }

    @Test
    void testRemoveMissing() {
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isFalse();
        partnerService.removeModelManagerShop(777, EnumSet.of(ModelManagementAccess.MANUAL));
        assertThat(partnerService.isModelManager(777, ModelManagementAccess.MANUAL)).isFalse();
    }

    @Test
    @DbUnitDataSet(after = "PartnerServiceTest.createPartner.after.csv")
    void createPartner() {
        assertThat(partnerService.createPartner(CampaignType.SHOP)).isEqualTo(datasourceId(1));
        assertThat(partnerService.createPartner(CampaignType.SUPPLIER)).isEqualTo(supplierId(2));
    }

    @Test
    void getPartner() {
        assertThat(partnerService.getPartner(123L)).isEmpty();
        assertThat(partnerService.getPartner(774L)).get().isEqualTo(supplierId(774L));
        assertThat(partnerService.getPartner(775L)).get().isEqualTo(datasourceId(775L));
    }

    @Test
    void getPartnersByType() {
        var suppliers = partnerService.getAllPartnersByType(CampaignType.SUPPLIER);
        assertThat(suppliers).containsExactlyInAnyOrder(
                new PartnerInfo(774L, CampaignType.SUPPLIER, 123L),
                new PartnerInfo(776L, CampaignType.SUPPLIER, null)
        );
    }

    @Test
    void searchPartner() {
        var result = partnerService.searchPartner("774", 0, 10);
        assertThat(result).containsExactly(
                new PartnerSearchInfo(774L, 1001L, "shop", CampaignType.SUPPLIER, 123L),
                new PartnerSearchInfo(776L, 1002L, "shop774", CampaignType.SUPPLIER, -1L)
        );
    }

    @Test
    void searchPartnerWithSpaces() {
        var result = partnerService.searchPartner("wow, space", 0, 10);
        assertThat(result).isEmpty();
    }
}
