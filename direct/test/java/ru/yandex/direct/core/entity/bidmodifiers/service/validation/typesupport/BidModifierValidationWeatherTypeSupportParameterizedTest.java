package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationWeatherTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(Parameterized.class)
public class BidModifierValidationWeatherTypeSupportParameterizedTest {
    public ClientInfo clientInfo;
    public BidModifierWeather modifier;
    public BidModifierValidationWeatherTypeSupport service;

    @Parameterized.Parameter()
    public WeatherType parameter;

    @Parameterized.Parameter(1)
    public OperationType operation;

    @Before
    public void setUp() {
        clientInfo = new ClientInfo().withClient(new Client().withClientId(1L));
        service = new BidModifierValidationWeatherTypeSupport();
    }

    @Parameterized.Parameters(name = "{0}, {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WeatherType.TEMP, OperationType.LE},
                {WeatherType.TEMP, OperationType.GE},
                {WeatherType.CLOUDNESS, OperationType.EQ},
                {WeatherType.PREC_STRENGTH, OperationType.EQ},
        });
    }

    @Test
    public void validateAddStep1_PercentValidation() {
        modifier = new BidModifierWeather()
                .withAdGroupId(1L)
                .withWeatherAdjustments(
                        Collections.singletonList(new BidModifierWeatherAdjustment()
                                .withExpression(Collections.singletonList(Collections.singletonList(
                                        new BidModifierWeatherLiteral()
                                                .withParameter(parameter)
                                                .withOperation(operation)
                                                .withValue(50)
                                )))
                                .withPercent(110))
                );

        ValidationResult<BidModifierWeather, Defect> vr = service.validateAddStep1(modifier,
                CampaignType.CPM_BANNER, new CpmOutdoorAdGroup().withType(AdGroupType.CPM_OUTDOOR),
                clientInfo.getClientId(), null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
