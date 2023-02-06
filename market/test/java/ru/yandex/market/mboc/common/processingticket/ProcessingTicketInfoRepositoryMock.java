package ru.yandex.market.mboc.common.processingticket;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;

/**
 * @author york
 * @since 09.07.2020
 */
public class ProcessingTicketInfoRepositoryMock implements ProcessingTicketInfoRepository {

    private int idSeq;
    private Map<Integer, ProcessingTicketInfo> ticketInfoMap = new HashMap<>();

    @Override
    public ProcessingTicketInfo getById(Integer id) {
        ProcessingTicketInfo result = ticketInfoMap.get(id);
        if (result != null) {
            return new ProcessingTicketInfo(result);
        }
        return null;
    }

    @Override
    public Collection<ProcessingTicketInfo> getByIds(Collection<Integer> ids) {
        return ticketInfoMap.entrySet().stream()
            .filter(e -> ids.contains(e.getKey()))
            .map(e -> e.getValue())
            .collect(Collectors.toList());
    }

    @Override
    public Collection<ProcessingTicketInfo> find(TicketFilter filter) {
        return ticketInfoMap.values().stream().filter(t -> matches(t, filter))
            .map(ProcessingTicketInfo::new)
            .collect(Collectors.toList());
    }

    private boolean matches(ProcessingTicketInfo ticket, TicketFilter filter) {
        boolean matches = true;
        if (filter.getTitle() != null) {
            matches = matches && Objects.equals(ticket.getTitle(), filter.getTitle());
        }
        if (filter.getBaseOfferStatus() != null) {
            matches = matches && (ticket.getOfferBaseStatus() != null &&
                Objects.equals(ticket.getOfferBaseStatus().getName(), filter.getBaseOfferStatus()));
        }
        if (filter.getMinDeadline() != null) {
            matches = matches && ticket.getComputedDeadline().compareTo(filter.getMinDeadline()) >= 0;
        }
        if (filter.getMaxDeadline() != null) {
            matches = matches && ticket.getComputedDeadline().compareTo(filter.getMaxDeadline()) <= 0;
        }
        return matches;
    }

    @Override
    public ProcessingTicketInfo save(ProcessingTicketInfo processingTicketInfo) {
        return save(Collections.singletonList(processingTicketInfo)).iterator().next();
    }

    @Override
    public Collection<ProcessingTicketInfo> save(Collection<ProcessingTicketInfo> processingTicketInfos) {
        processingTicketInfos.forEach(t -> {
            if (t.getId() == null) {
                t.setId(++idSeq);
            }
            ticketInfoMap.put(t.getId(), new ProcessingTicketInfo(t));
        });
        return processingTicketInfos;
    }

    @Override
    public int delete(Collection<Integer> ids) {
        ticketInfoMap.keySet().removeAll(ids);
        return 0;
    }
}
