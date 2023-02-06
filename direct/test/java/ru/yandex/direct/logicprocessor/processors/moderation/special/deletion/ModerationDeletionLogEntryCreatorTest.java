package ru.yandex.direct.logicprocessor.processors.moderation.special.deletion;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.ess.common.circuits.moderation.ModerationDeletionObjectType;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class ModerationDeletionLogEntryCreatorTest {

    static Stream<Arguments> testData() {
        List<ModerationDeletionObjectType> allTypes = List.of(ModerationDeletionObjectType.values());
        List<Arguments> arguments = mapList(allTypes, Arguments::arguments);
        return arguments.stream();
    }

    @ParameterizedTest(name = "ModerationDeletionObjectType = {0}")
    @MethodSource("testData")
    public void canLogAllTypes(ModerationDeletionObjectType type) {
        ModerationDeletionRequest request = new ModerationDeletionRequest();
        request.setId(123L);
        request.setType(type);
        new ModerationDeletionLogEntryCreator().createLogEntry(request);
    }
}
