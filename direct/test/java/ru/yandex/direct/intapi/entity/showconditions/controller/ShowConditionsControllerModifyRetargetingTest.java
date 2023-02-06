package ru.yandex.direct.intapi.entity.showconditions.controller;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertTrue;

/**
 * Тесты для проверки привязки дефектов валидации обновления и удаления ретаргетингов и переводов в intapi
 * <p>
 * следующие дефекты не проверяются в ручке, т.к. при стандартном вызове нельзя передать невалидные данные:
 * BidsDefects.Ids.CONTEXT_PRICE_IS_NOT_SET_FOR_MANUAL_STRATEGY
 * BidsDefects.Ids.PRIORITY_IS_NOT_SET_FOR_AUTO_STRATEGY
 */
public class ShowConditionsControllerModifyRetargetingTest extends ShowConditionsControllerModifyRetargetingBaseTest {

    @Before
    public void before() {
        super.before();
        objectId = retargetingInfo.getRetargetingId();
    }

    @Test
    public void update_EmptyRequest() {
        ShowConditionsRequest request = new ShowConditionsRequest();

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertTrue(response.isSuccessful());
        assertThat(response.getItems().entrySet(), empty());
    }

    @Test
    public void update_EqualsRetargetingIdsOnDelete_DuplicatedRetargetingId() {
        ShowConditionsRequest request = buildDeleteRequest(adGroupId, objectId);
        request.getRetargetings().get(adGroupId).getDeleted().add(objectId);

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_DeleteAndUpdateSameRetargeting_RequestFailed() {
        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId, new RetargetingItem());
        request.getRetargetings().get(adGroupId).getDeleted().add(objectId);

        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat("ожидается ошибка верхнего уровня: некорректные параметры удаления и обновления ретаргетингов",
                response.getErrors(), Matchers.not(empty()));
    }
}
