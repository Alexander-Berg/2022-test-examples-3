package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.exception.ObjectNotFoundInSnapshotException;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.bsexport.snapshot.model.ExportedCampaign;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderDataFactoryGetAutoOptimizationTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;
    private BaseCampaign campaign;
    private ExportedCampaign exportedCampaign;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);

        campaign = createCampaign();

        exportedCampaign = new ExportedCampaign()
                .withId(campaign.getId());
        putExportedCampaignToSnapshot(exportedCampaign);
    }


    @Test
    void autoOptimizationIsTrue_resultIs1() {
        exportedCampaign.setAutoOptimization(true);
        assertThat(orderDataFactory.getAutoOptimization(campaign)).isEqualTo(1);
    }

    @Test
    void autoOptimizationIsFalse_resultIs0() {
        exportedCampaign.setAutoOptimization(false);
        assertThat(orderDataFactory.getAutoOptimization(campaign)).isEqualTo(0);
    }

    @Test
    void exportedCampaignNotFoundInSnapshot_throwsException() {
        // практически невозможный сценарий
        removeExportedCampaignFromSnapshot(campaign.getId());

        assertThrows(ObjectNotFoundInSnapshotException.class, () -> orderDataFactory.getAutoOptimization(campaign));
    }
}
