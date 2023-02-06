package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.math.BigDecimal;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyPromocodeParameters;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestPromocodeInfo.createPromocodeInfo;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.CAMPAIGN_ID_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.SERVICE_ID_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyPromocodeParameters.PROMOCODES_FIELD_NAME;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyPromocodeValidationServiceValidateRequestTest {
    private static final int DIRECT_SERVICE_ID = 7;

    @Autowired
    private NotifyPromocodeValidationService service;

    @Autowired
    private Steps steps;

    private NotifyPromocodeParameters request = new NotifyPromocodeParameters();

    @Test
    public void validNotifyPromocodeParameters_hasNoErrors() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();

        PromocodeInfo promocodeInfo = createPromocodeInfo();
        BalancePromocodeInfo balancePromocodeInfo = new BalancePromocodeInfo()
                .withId(promocodeInfo.getId())
                .withCode(promocodeInfo.getCode())
                .withInvoiceId(promocodeInfo.getInvoiceId())
                .withInvoiceExternalId(promocodeInfo.getInvoiceExternalId())
                .withInvoiceEnabledAt(promocodeInfo.getInvoiceEnabledAt())
                .withAvailableQty(BigDecimal.TEN)
                .withUniqueUrlNeeded(true);

        request.withServiceId(DIRECT_SERVICE_ID)
                .withCampaignId(campaignInfo.getCampaignId())
                .withPromocodes(singletonList(balancePromocodeInfo));

        assertThat(service.validateRequest(request), hasNoErrors());
    }

    @Test
    public void serviceIdIsNull_cannotBeNull() {
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(SERVICE_ID_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void zeroServiceId_invalidValue() {
        request.withServiceId(0);
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(SERVICE_ID_FIELD_NAME)), DefectIds.INVALID_VALUE)));
    }

    @Test
    public void incorrectServiceId_invalidValue() {
        request.withServiceId(RandomUtils.nextInt(100500, Integer.MAX_VALUE));
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(SERVICE_ID_FIELD_NAME)), DefectIds.INVALID_VALUE)));
    }

    @Test
    public void serviceOrderIdIsNull_cannotBeNull() {
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(CAMPAIGN_ID_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void zeroServiceOrderId_mustBeValidId() {
        request.withCampaignId(0L);
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(CAMPAIGN_ID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID)));
    }

    @Test
    public void nonExistingServiceOrderId_campaignNotFound() {
        request.withCampaignId(RandomUtils.nextLong(Integer.MAX_VALUE, Long.MAX_VALUE));
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(CAMPAIGN_ID_FIELD_NAME)), CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND)));
    }

    @Test
    public void promocodesIsNull_cannotBeNull() {
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(PROMOCODES_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void promocodesContainsNull_cannotBeNull() {
        request.withPromocodes(singletonList(null));
        assertThat(service.validateRequest(request), hasDefectDefinitionWith(
                validationError(path(field(PROMOCODES_FIELD_NAME), index(0)), DefectIds.CANNOT_BE_NULL)));
    }
}
