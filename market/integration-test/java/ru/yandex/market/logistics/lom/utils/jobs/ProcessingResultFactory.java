package ru.yandex.market.logistics.lom.utils.jobs;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class ProcessingResultFactory {
    public static ProcessingResult processingResult(ProcessingResultStatus processingResultStatus, String comment) {
        return ProcessingResult.builder()
            .status(processingResultStatus)
            .comment(comment)
            .build();
    }
}
