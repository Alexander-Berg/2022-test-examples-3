package ru.yandex.market.tpl.core.task;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.task.flow.ActionPrecondition;
import ru.yandex.market.tpl.core.task.flow.AfterActionHandler;
import ru.yandex.market.tpl.core.task.flow.Context;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;

/**
 * @author sekulebyakin
 */
@UtilityClass
public class TestTaskFlowUtils {

    public static final String RESET_ERROR_MESSAGE = "Cannon reset progress - it is test";

    public AfterActionHandler<EmptyPayload> emptyHandler() {
        return new AfterActionHandler<EmptyPayload>() {
            @Override
            public void doAfterAction(Context context, EmptyPayload actionOutput) {
                // do nothing
            }
        };
    }

    public ActionPrecondition alwaysTruePrecondition() {
        return new ActionPrecondition() {
            @Override
            public boolean test(Context context) {
                return true;
            }
        };
    }

    public ActionPrecondition alwaysFalsePrecondition() {
        return new ActionPrecondition() {
            @Override
            public boolean test(Context context) {
                return false;
            }
        };
    }

}
