package ru.yandex.market.crm.triggers.services.bpm.correlation;

import java.util.Map;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewQuestionOnModel;

/**
 * @author apershukov
 */
class CorrelationTestUtils {

    static UidBpmMessage message(Uid uid) {
        NewQuestionOnModel model = new NewQuestionOnModel(123, "321", 111);

        return new UidBpmMessage(
                MessageTypes.NEW_QUESTION_ON_REVIEWED_MODEL,
                uid,
                Map.of(),
                Map.of(
                        ProcessVariablesNames.Event.NEW_QUESTION_ON_MODEL,
                        model
                )
        );
    }

    static UidBpmMessage message() {
        return message(Uid.asPuid(111L));
    }
}
