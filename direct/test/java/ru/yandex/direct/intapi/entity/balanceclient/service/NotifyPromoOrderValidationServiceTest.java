package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.yandexagencyorder.model.Status;
import ru.yandex.direct.core.entity.yandexagencyorder.model.YandexAgencyOrder;
import ru.yandex.direct.core.testing.data.TestYandexAgencyOrders;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyPromoOrderValidationServiceTest {
    @Autowired
    private NotifyPromoOrderValidationService notifyPromoOrderValidationService;

    @Autowired
    private UserSteps userSteps;

    @Test
    public void preValidateNullOrder() {
        List<NotifyOrderParameters> orders = Collections.singletonList(null);
        ValidationResult<List<NotifyOrderParameters>, Defect> vr =
                notifyPromoOrderValidationService.preValidate(orders);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(CollectionDefectIds.Gen.CANNOT_CONTAIN_NULLS)));
    }

    @Test
    public void preValidateNullOrderList() {
        List<NotifyOrderParameters> orders = null;
        ValidationResult<List<NotifyOrderParameters>, Defect>
                vr = notifyPromoOrderValidationService.preValidate(orders);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void preValidateTooLongOrderList() {
        List<NotifyOrderParameters> orders =
                Arrays.asList(new NotifyOrderParameters(), new NotifyOrderParameters());

        ValidationResult<List<NotifyOrderParameters>, Defect>
                vr = notifyPromoOrderValidationService.preValidate(orders);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)));
    }

    @Test
    public void preValidateCorrectOrderList() {
        List<NotifyOrderParameters> orders = Collections
                .singletonList(new NotifyOrderParameters());
        ValidationResult<List<NotifyOrderParameters>, Defect>
                vr = notifyPromoOrderValidationService.preValidate(orders);
        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateOrderAlreadyPaid() {
        UserInfo user = userSteps.createDefaultUser();
        YandexAgencyOrder yandexAgencyOrder =
                TestYandexAgencyOrders.defaultYandexAgencyOrder(user.getClientInfo().getClientId(),
                        user.getClientInfo().getClientId().asLong()).withYaOrderStatus(Status.PAID);
        List<YandexAgencyOrder> orders =
                Collections.singletonList(yandexAgencyOrder);
        ValidationResult<List<YandexAgencyOrder>, Defect>
                vr = notifyPromoOrderValidationService.validateOrdersAreNotCompleted(orders);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.INVALID_VALUE)));
    }

    @Test
    public void validateOrderWithEmptyCurrentOrders() {
        List<YandexAgencyOrder> orders = emptyList();
        List<Long> ids = singletonList(1L);
        ValidationResult<List<Long>, Defect>
                vr = notifyPromoOrderValidationService.validateOrderIdsExist(ids, orders);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION)));
    }

    @Test
    public void validateOrderStatusCorrectOrderList() {
        UserInfo user = userSteps.createDefaultUser();
        YandexAgencyOrder yandexAgencyOrder =
                TestYandexAgencyOrders.defaultYandexAgencyOrder(user.getClientInfo().getClientId(),
                        user.getClientInfo().getClientId().asLong());
        List<YandexAgencyOrder> orders =
                Collections.singletonList(yandexAgencyOrder);
        ValidationResult<List<YandexAgencyOrder>, Defect>
                vr = notifyPromoOrderValidationService.validateOrdersAreNotCompleted(orders);

        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateOrderIdsCorrectOrderList() {
        UserInfo user = userSteps.createDefaultUser();
        YandexAgencyOrder yandexAgencyOrder =
                TestYandexAgencyOrders.defaultYandexAgencyOrder(user.getClientInfo().getClientId(),
                        user.getClientInfo().getClientId().asLong());
        List<YandexAgencyOrder> orders =
                Collections.singletonList(yandexAgencyOrder);
        List<Long> ids = Collections.singletonList(yandexAgencyOrder.getId());
        ValidationResult<List<Long>, Defect>
                vr = notifyPromoOrderValidationService.validateOrderIdsExist(ids, orders);

        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }
}
