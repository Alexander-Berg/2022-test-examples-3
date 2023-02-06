package ru.yandex.market.mbo.utils.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author s-ermakov
 */
public class GroupOperationStatusAssertions
    extends AbstractObjectAssert<GroupOperationStatusAssertions, GroupOperationStatus> {
    public GroupOperationStatusAssertions(GroupOperationStatus groupOperationStatus) {
        super(groupOperationStatus, GroupOperationStatusAssertions.class);
    }

    public GroupOperationStatusAssertions isOk() {
        super.isNotNull();
        if (!actual.isOk()) {
            failWithMessage("Expected status to be ok. Actual is failure.\n%s", getInfo(actual));
        }
        return super.myself;
    }

    public GroupOperationStatusAssertions isFailure() {
        super.isNotNull();
        if (!actual.isFailure()) {
            failWithMessage("Expected status to be failure. Actual is Ok.\n%s", getInfo(actual));
        }
        return super.myself;
    }

    public GroupOperationStatusAssertions hasStatus(OperationStatusType statusType) {
        super.isNotNull();
        if (actual.getStatus() != statusType) {
            failWithMessage("Expected status to be %s. Actual is %s.\n%s",
                statusType, actual.getStatus(), getInfo(actual));
        }
        return super.myself;
    }

    public AbstractStringAssert<?> getInfo() {
        super.isNotNull();
        return Assertions.assertThat(getInfo(actual));
    }

    private static String getInfo(GroupOperationStatus groupOperationStatus) {
        String shortMessage = toStringShort(groupOperationStatus);
        String fullMessage = toStringFull(groupOperationStatus);

        return "--- Short message --\n" + shortMessage + "\n-- Full message ---\n" + fullMessage + "\n-----";
    }

    private static String toStringShort(GroupOperationStatus groupOperationStatus) {
        return groupOperationStatus.getAllModelStatuses().stream()
            .map(status -> {
                String statusMessage = status.getStatusMessage();
                OperationStatusType statusType = status.getStatus();
                Stream<Word> messages = status.getLocalizedMessage().stream();
                Stream<Word> validationErrors = status.getValidationErrors().stream()
                    .flatMap(e -> e.getLocalizedMessage().stream());
                String resultMessages = Stream.concat(messages, validationErrors)
                    .map(Word::getWord)
                    .collect(Collectors.joining("; "));
                return statusType + ": " + String.join("; ", resultMessages, statusMessage);
            })
            .collect(Collectors.joining("\n"));
    }

    private static String toStringFull(GroupOperationStatus status) {
        return "GroupOperationStatus{" +
            "isFailure=" + status.isFailure() +
            ", failedModelIds=" + status.getFailedModelIds() +
            ", status=" + status.getStatus() +
            ", statusMessage='" + status.getStatusMessage() +
            ", requestedModelStatuses=" + status.getRequestedModelStatuses() +
            ", additionalModelStatues=" + status.getAdditionalModelStatues() +
            ", validationErrors=" + status.getValidationErrors() + '\'' +
            '}';
    }
}
