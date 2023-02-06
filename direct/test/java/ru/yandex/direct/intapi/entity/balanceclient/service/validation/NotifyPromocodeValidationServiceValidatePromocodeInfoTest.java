package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.result.DefectIds;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo.AVAILABLE_PROMOCODE_QTY_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo.NEED_UNIQUE_URLS_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo.PROMOCODE_ID_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo.START_DATE_TIME_FIELD_NAME;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class NotifyPromocodeValidationServiceValidatePromocodeInfoTest {

    private final NotifyPromocodeValidationService service;
    private BalancePromocodeInfo info = new BalancePromocodeInfo();

    @SuppressWarnings("ConstantConditions")
    public NotifyPromocodeValidationServiceValidatePromocodeInfoTest() {
        service = new NotifyPromocodeValidationService(0, mock(CampaignService.class));
    }


    @Test
    public void validPromocodeInfo_withUniqueUrlNeeded_hasNoErrors() {
        info.withId(RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .withAvailableQty(BigDecimal.ZERO)
                .withUniqueUrlNeeded(true)
                .withInvoiceEnabledAt(LocalDateTime.now());
        assertThat(service.validatePromocodeInfo(info), hasNoErrors());
    }

    @Test
    public void validPromocodeInfo_withoutUniqueUrlNeeded_hasNoErrors() {
        info.withId(RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .withAvailableQty(BigDecimal.ZERO)
                .withUniqueUrlNeeded(false)
                .withInvoiceEnabledAt(LocalDateTime.now());
        assertThat(service.validatePromocodeInfo(info), hasNoErrors());
    }

    @Test
    public void validPromocodeInfo_withSomeAvailableQty_hasNoErrors() {
        info.withId(RandomUtils.nextLong(1, Integer.MAX_VALUE))
                .withAvailableQty(BigDecimal.TEN)
                .withUniqueUrlNeeded(true)
                .withInvoiceEnabledAt(LocalDateTime.now());
        assertThat(service.validatePromocodeInfo(info), hasNoErrors());
    }

    @Test
    public void promocodeIdIsNull_cannotBeNull() {
        assertThat(service.validatePromocodeInfo(info), hasDefectDefinitionWith(
                validationError(path(field(PROMOCODE_ID_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void needUniqueUrlIsNull_cannotBeNull() {
        assertThat(service.validatePromocodeInfo(info), hasDefectDefinitionWith(
                validationError(path(field(NEED_UNIQUE_URLS_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void startDtIsNull_cannotBeNull() {
        assertThat(service.validatePromocodeInfo(info), hasDefectDefinitionWith(
                validationError(path(field(START_DATE_TIME_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void availableQtyIsNull_cannotBeNull() {
        assertThat(service.validatePromocodeInfo(info), hasDefectDefinitionWith(
                validationError(path(field(AVAILABLE_PROMOCODE_QTY_FIELD_NAME)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void zeroPromocodeId_mustBeValidId() {
        info.withId(0L);
        assertThat(service.validatePromocodeInfo(info), hasDefectDefinitionWith(
                validationError(path(field(PROMOCODE_ID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID)));
    }

    @Test
    public void negativePromocodeId_mustBeValidId() {
        info.withId(-1L);
        assertThat(service.validatePromocodeInfo(info), hasDefectDefinitionWith(
                validationError(path(field(PROMOCODE_ID_FIELD_NAME)), DefectIds.MUST_BE_VALID_ID)));
    }

    @Test
    public void availableQtyIsNegative_cannotBeNull() {
        info.withAvailableQty(BigDecimal.ONE.negate());
        assertThat(service.validatePromocodeInfo(info),
                hasDefectDefinitionWith(validationError(
                        path(field(AVAILABLE_PROMOCODE_QTY_FIELD_NAME)),
                        NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)));
    }
}
