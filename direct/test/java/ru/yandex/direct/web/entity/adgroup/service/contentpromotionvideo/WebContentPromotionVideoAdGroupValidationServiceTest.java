package ru.yandex.direct.web.entity.adgroup.service.contentpromotionvideo;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotGreaterThan;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionVideoAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class WebContentPromotionVideoAdGroupValidationServiceTest {

    @Autowired
    private WebContentPromotionVideoAdGroupValidationService validationService;

    @Autowired
    private Steps steps;

    @Autowired
    private ClientService clientService;

    private CampaignInfo campaignInfo;
    private Currency clientCurrency;

    @Before
    public void before() {
        campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign();
        clientCurrency = clientService.getWorkCurrency(campaignInfo.getClientId());
    }

    @Test
    public void validate_AdGroupIsNull_ValidationError() {
        List<WebContentPromotionAdGroup> adGroups = singletonList(null);
        ValidationResult<List<WebContentPromotionAdGroup>, Defect> vr =
                validationService.validate(adGroups, clientCurrency);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), notNull())));
    }

    @Test
    public void validate_AdGroupInCampaign_Successful() {
        WebContentPromotionAdGroup webContentPromotionAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId());

        ValidationResult<List<WebContentPromotionAdGroup>, Defect> vr = validationService
                .validate(singletonList(webContentPromotionAdGroup), clientCurrency);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_TooLowGeneralPrice_ValidationError() {
        Money price = Money.valueOf(clientCurrency.getMinPrice().subtract(BigDecimal.valueOf(0.01)),
                clientCurrency.getCode());

        WebContentPromotionAdGroup webContentPromotionAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        ValidationResult<List<WebContentPromotionAdGroup>, Defect> vr = validationService
                .validate(singletonList(webContentPromotionAdGroup), clientCurrency);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebContentPromotionAdGroup.Prop.GENERAL_PRICE)),
                        invalidValueNotLessThan(Money.valueOf(clientCurrency.getMinPrice(), clientCurrency.getCode())))));
    }

    @Test
    public void validate_MinGeneralPrice_Successful() {
        Money price = Money.valueOf(clientCurrency.getMinPrice(), clientCurrency.getCode());

        WebContentPromotionAdGroup webContentPromotionAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        ValidationResult<List<WebContentPromotionAdGroup>, Defect> vr = validationService
                .validate(singletonList(webContentPromotionAdGroup), clientCurrency);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_TooHighGeneralPrice_ValidationError() {
        Money price = Money.valueOf(clientCurrency.getMaxPrice().add(BigDecimal.valueOf(0.01)),
                clientCurrency.getCode());

        WebContentPromotionAdGroup webContentPromotionAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        ValidationResult<List<WebContentPromotionAdGroup>, Defect> vr = validationService
                .validate(singletonList(webContentPromotionAdGroup), clientCurrency);

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(WebContentPromotionAdGroup.Prop.GENERAL_PRICE)),
                        invalidValueNotGreaterThan(Money.valueOf(clientCurrency.getMaxPrice(), clientCurrency.getCode())))));
    }

    @Test
    public void validate_MaxGeneralPrice_Successful() {
        Money price = Money.valueOf(clientCurrency.getMaxPrice(), clientCurrency.getCode());

        WebContentPromotionAdGroup webContentPromotionAdGroup =
                randomNameWebContentPromotionVideoAdGroup(null, campaignInfo.getCampaignId())
                        .withGeneralPrice(price.bigDecimalValue().doubleValue());

        ValidationResult<List<WebContentPromotionAdGroup>, Defect> vr = validationService
                .validate(singletonList(webContentPromotionAdGroup), clientCurrency);

        assertThat(vr, hasNoDefectsDefinitions());
    }
}
