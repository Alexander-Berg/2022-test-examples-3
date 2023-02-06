package ru.yandex.market.markup2.core.stubs.persisters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.dao.FrozenOffersPersister;
import ru.yandex.market.markup2.utils.offer.OfferFreezingService;

/**
 * @author york
 * @since 07.09.2020
 */
public class FrozenOffersPersisterStub extends FrozenOffersPersister implements IPersisterStub {
    private List<Markup.FrozenGroup> groupList;

    public FrozenOffersPersisterStub() {
        this(new ArrayList<>());
    }

    private FrozenOffersPersisterStub(List<Markup.FrozenGroup> groupList) {
        this.groupList = groupList;
    }

    @Override
    public FrozenOffersPersisterStub copy() {
        return new FrozenOffersPersisterStub(groupList);
    }

    @Override
    public void persist(Markup.FrozenGroup value) {
        groupList.add(value);
    }

    @Override
    public List<Markup.FrozenGroup> getByFilter(OfferFreezingService.Filter filter) {
        return groupList.stream().filter(gr -> {
            if (filter.getProcessingTicketId() != null && gr.hasProcessingTicketId() &&
                    gr.getProcessingTicketId() != filter.getProcessingTicketId()) {
                return false;
            }
            if (filter.getTicket() != null && gr.hasTicket() &&
                    !gr.getTicket().equals(filter.getTicket())) {
                return false;
            }
            if (filter.getSupplierId() != null && gr.hasSupplierId() &&
                    filter.getSupplierId() != gr.getSupplierId()) {
                return false;
            }
            if (filter.getCategoryId() != null && gr.hasCategoryId() &&
                    filter.getCategoryId() != gr.getCategoryId()) {
                return false;
            }
            if (filter.isActual()) {
                long currentTime = System.currentTimeMillis();
                if (gr.getFreezeStartTime() > currentTime || gr.getFreezeFinishTime() < currentTime) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

}
