package ru.yandex.direct.core.entity.bs.common.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@CoreTest
@ExtendWith(SpringExtension.class)
@ParametersAreNonnullByDefault
class BsOrderIdCalculatorTest {
    @Autowired
    private BsOrderIdCalculator bsOrderIdCalculator;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private Steps steps;

    @Test
    void checkCalculateOrderIdForCampaignWithoutOrderId() {
        CampaignInfo campaignInfo = createCampaignWithoutOrderId();

        Map<Long, Long> orderIdForCampaigns =
                bsOrderIdCalculator.calculateOrderIdIfNotExist(
                        campaignInfo.getShard(), List.of(campaignInfo.getCampaignId()));

        assertThat(orderIdForCampaigns).hasEntrySatisfying(
                campaignInfo.getCampaignId(),
                orderId -> assertThat(orderId).isEqualTo(campaignInfo.getCampaignId() + 100_000_000));
    }

    @Test
    void checkCalculateOrderIdForNotExistingCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(activeTextCampaign(null, null));
        testCampaignRepository.deleteCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());

        Map<Long, Long> orderIdForCampaigns =
                bsOrderIdCalculator.calculateOrderIdIfNotExist(
                        campaignInfo.getShard(), List.of(campaignInfo.getCampaignId()));

        assertThat(orderIdForCampaigns).isEmpty();
    }

    @Test
    void checkCalculateOrderIdForCampaignWithOrderId() {
        long expectedOrderId = 333333L;
        CampaignInfo campaignInfo = createCampaignWithOrderId(expectedOrderId);

        Map<Long, Long> orderIdForCampaigns =
                bsOrderIdCalculator.calculateOrderIdIfNotExist(
                        campaignInfo.getShard(), List.of(campaignInfo.getCampaignId()));

        assertThat(orderIdForCampaigns).hasEntrySatisfying(
                campaignInfo.getCampaignId(), orderId -> assertThat(orderId).isEqualTo(expectedOrderId));
    }

    private CampaignInfo createCampaignWithOrderId(@Nullable Long orderId) {
        return steps.campaignSteps()
                .createCampaign(activeTextCampaign(null, null).withOrderId(orderId));
    }

    private CampaignInfo createCampaignWithoutOrderId() {
        return createCampaignWithOrderId(null);
    }
}
