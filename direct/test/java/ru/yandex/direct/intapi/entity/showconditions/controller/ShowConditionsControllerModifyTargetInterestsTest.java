package ru.yandex.direct.intapi.entity.showconditions.controller;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

/**
 * Тесты для проверки привязки дефектов валидации обновления и удаления таргетов по интересам в intapi
 */
public class ShowConditionsControllerModifyTargetInterestsTest
        extends ShowConditionsControllerModifyRetargetingBaseTest {

    @Before
    public void before() {
        super.before();
        objectId = retargetingInfo.getRetargetingId();
    }

    @Test
    public void update_EqualsRetargetingIdsOnDelete_DuplicatedRetargetingId() {
        ShowConditionsRequest request = buildDeleteRequest(adGroupId, objectId);
        request.getTargetInterests().get(adGroupId).getDeleted().add(objectId);

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_DeleteAndUpdateSameTargetInterest_RequestFailed() {
        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId, new RetargetingItem());
        request.getTargetInterests().get(adGroupId).getDeleted().add(objectId);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat("ожидается ошибка верхнего уровня: некорректные параметры удаления и обновления интересов",
                response.getErrors(), Matchers.not(empty()));
    }

    @Override
    protected ShowConditionsRequest buildDeleteRequest(Long adGroupId, Long targetInterestsId) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        RetargetingModificationContainer container = new RetargetingModificationContainer();
        container.getDeleted().add(targetInterestsId);
        request.getTargetInterests().put(adGroupId, container);

        return request;
    }

    @Override
    protected ShowConditionsRequest buildEditRequest(Long adGroupId, Long targetInterestsId, RetargetingItem item) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        RetargetingModificationContainer container = new RetargetingModificationContainer();
        container.getEdited().put(targetInterestsId, item);
        request.getTargetInterests().put(adGroupId, container);

        return request;
    }

}
