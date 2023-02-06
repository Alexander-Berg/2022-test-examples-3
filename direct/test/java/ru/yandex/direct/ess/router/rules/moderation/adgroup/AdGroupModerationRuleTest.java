package ru.yandex.direct.ess.router.rules.moderation.adgroup;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.core.entity.moderation.model.TransportStatus;
import ru.yandex.direct.ess.logicobjects.moderation.adgroup.AdGroupModerationEventObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.PhrasesTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class AdGroupModerationRuleTest {
    @Autowired
    private AdGroupModerationRule rule;

    static Stream<Arguments> parameters() {
        return Stream.of(
                arguments(INSERT, false, TransportStatus.Ready, true),
                arguments(INSERT, true, TransportStatus.Ready, true),
                arguments(INSERT, false, TransportStatus.No, true),
                arguments(INSERT, true, TransportStatus.No, false),
                arguments(INSERT, false, TransportStatus.Yes, true),
                arguments(INSERT, true, TransportStatus.Yes, false),
                arguments(INSERT, false, TransportStatus.Sending, false),
                arguments(INSERT, true, TransportStatus.Sending, false),
                arguments(INSERT, false, TransportStatus.Sent, false),
                arguments(INSERT, true, TransportStatus.Sent, false),

                arguments(UPDATE, false, TransportStatus.Ready, true),
                arguments(UPDATE, false, TransportStatus.No, false),
                arguments(UPDATE, false, TransportStatus.Yes, false),
                arguments(INSERT, false, TransportStatus.Sending, false),
                arguments(INSERT, false, TransportStatus.Sent, false)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void mapBinlogEventTest(Operation operation, boolean isResharding,
                            TransportStatus statusModerate, boolean needModeration) {
        var change = new PhrasesTableChange().withCid(1).withPid(2);
        if (operation == INSERT) {
            change.addInsertedColumn(PHRASES.STATUS_MODERATE, statusModerate.name());
        } else if (operation == UPDATE) {
            change.addChangedColumn(PHRASES.STATUS_MODERATE, TransportStatus.New.name(), statusModerate.name());
        }
        var binLogEvent = PhrasesTableChange.createPhrasesEvent(Collections.singletonList(change), operation);
        binLogEvent.setResharding(isResharding);

        List<AdGroupModerationEventObject> events = rule.mapBinlogEvent(binLogEvent);
        if (needModeration) {
            assertThat(events).isNotEmpty();
        } else {
            assertThat(events).isEmpty();
        }
    }
}
