package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.parameterizer;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SubstitutionConverterTest {

    private final SubstitutionConverter substitutionConverter = new SubstitutionConverter();

    @ParameterizedTest(name = "{0}")
    @MethodSource("longCampaignNameReplacingParams")
    void longCampaignNameTest(String campaignName, ReplacingParams params) {
        var converted = substitutionConverter.replace("campaign_name", params).getResult();
        assertThat(campaignName).startsWith(converted);
        assertThat(converted.length()).isLessThanOrEqualTo(60);

        var nextSpaceAfterPrefix = campaignName.indexOf(' ', converted.length() + 1);
        assertThat(nextSpaceAfterPrefix == -1 || nextSpaceAfterPrefix > 60).isTrue();
    }

    static Stream<Arguments> longCampaignNameReplacingParams() {
        var names = Stream.of(
                "small string",
                "sixteen symbols ".repeat(5),
                "toomanysymbolstosplit ".repeat(4),
                "a".repeat(61)
        );

        return names.map(name -> {
            var replacingParams = ReplacingParams.builder()
                    .withCampaignName(name)
                    .withCampaignNameLat(name)
                    .withCampaignType(CampaignType.PERFORMANCE)
                    .withCid(42L)
                    .withPid(43L)
                    .build();
            return arguments(name, replacingParams);
        });
    }
}
