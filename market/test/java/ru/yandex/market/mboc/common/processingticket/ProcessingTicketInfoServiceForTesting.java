package ru.yandex.market.mboc.common.processingticket;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jooq.JSONB;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;

/**
 * @author york
 * @since 10.07.2020
 */
public class ProcessingTicketInfoServiceForTesting extends ProcessingTicketInfoService {
    public ProcessingTicketInfoServiceForTesting(ProcessingTicketInfoRepository repository) {
        super(repository);
    }

    @Override
    public boolean setCounts(Map<Long, Integer> map, ProcessingTicketInfo ticketInfo,
                                Function<ProcessingTicketInfo, JSONB> getter,
                                BiConsumer<ProcessingTicketInfo, JSONB> setter) {
        return super.setCounts(map, ticketInfo, getter, setter);
    }

    public void delete(ProcessingTicketInfo processingTicketInfo) {
        super.remove(processingTicketInfo);
    }
}
