package ru.yandex.market.mbo.mdm.common.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;

/**
 * @author albina-gima
 * @date 8/2/21
 */
public class MdmQueueInfoBaseUtils {
    private MdmQueueInfoBaseUtils() {
    }

    public static <T> List<T> keys(List<? extends MdmQueueInfoBase<T>> unprocessedRowsFromQueue) {
        return unprocessedRowsFromQueue.stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
    }

    public static <T> List<Long> ids(List<? extends MdmQueueInfoBase<T>> unprocessedRowsFromQueue) {
        return unprocessedRowsFromQueue.stream()
            .map(MdmQueueInfoBase::getId)
            .collect(Collectors.toList());
    }

    public static <T> List<MdmEnqueueReason> reasons(List<? extends MdmQueueInfoBase<T>> unprocessedRowsFromQueue) {
        return unprocessedRowsFromQueue.stream()
            .map(MdmQueueInfoBase::getOnlyReasons)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }
}
