package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.utils;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.campaign.model.BillingAggregateCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmDealsCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DisallowedTargetTypesCalculatorTest {

    static Stream<Arguments> params() {
        DbStrategy searchStrategy = new DbStrategy();
        searchStrategy.withPlatform(CampaignsPlatform.SEARCH);

        DbStrategy contextStrategy = new DbStrategy();
        contextStrategy.withPlatform(CampaignsPlatform.CONTEXT);

        DbStrategy bothStrategy = new DbStrategy();
        bothStrategy.withPlatform(CampaignsPlatform.BOTH);

        return Stream.of(
                arguments(new CpmDealsCampaign(), List.of(0, 1, 2)),
                arguments(new CpmBannerCampaign().withStrategy(searchStrategy), List.of(0, 1, 2)),
                arguments(new CpmBannerCampaign().withStrategy(contextStrategy), List.of(0, 1, 2)),
                arguments(new CpmBannerCampaign().withStrategy(bothStrategy), List.of(0, 1, 2)),
                arguments(new CpmYndxFrontpageCampaign().withStrategy(searchStrategy), List.of(0, 1, 2)),
                arguments(new CpmYndxFrontpageCampaign().withStrategy(contextStrategy), List.of(0, 1, 2)),
                arguments(new CpmYndxFrontpageCampaign().withStrategy(bothStrategy), List.of(0, 1, 2)),
                arguments(new CpmPriceCampaign().withStrategy(searchStrategy), List.of(0, 1, 2)),
                arguments(new CpmPriceCampaign().withStrategy(contextStrategy), List.of(0, 1, 2)),
                arguments(new CpmPriceCampaign().withStrategy(bothStrategy), List.of(0, 1, 2)),

                arguments(new InternalAutobudgetCampaign().withStrategy(searchStrategy), List.of()),
                arguments(new InternalAutobudgetCampaign().withStrategy(contextStrategy), List.of()),
                arguments(new InternalAutobudgetCampaign().withStrategy(bothStrategy), List.of()),
                arguments(new InternalFreeCampaign(), List.of()),
                arguments(new InternalDistribCampaign(), List.of()),

                arguments(new WalletTypedCampaign(), List.of()),
                arguments(new BillingAggregateCampaign(), List.of()),

                arguments(new TextCampaign().withStrategy(searchStrategy), List.of(3)),
                arguments(new TextCampaign().withStrategy(contextStrategy), List.of(0, 1, 2)),
                arguments(new TextCampaign().withStrategy(bothStrategy), List.of()));
    }

    @ParameterizedTest
    @MethodSource("params")
    void test(CommonCampaign campaign, List<Integer> expectedResult) {
        var disallowedTargetTypesCalculator = new DisallowedTargetTypesCalculator();
        var gotResult = disallowedTargetTypesCalculator.calculate(campaign);
        assertThat(gotResult).isEqualTo(expectedResult);
    }
}
