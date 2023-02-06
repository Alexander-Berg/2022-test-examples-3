package ru.yandex.market.clab.common.service.audit;

import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.SortOrder;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 26.04.2019
 */
public class AuditRepositoryStub implements AuditRepository {
    private final List<AuditAction> actions = new ArrayList<>();
    private long nextId = 1;

    @Override
    public void writeAction(AuditAction action) {
        action.setId(nextId++);
        actions.add(action);
    }

    @Override
    public void writeActions(Collection<AuditAction> actions) {
        actions.forEach(a -> a.setId(nextId++));
        this.actions.addAll(actions);
    }

    @Override
    public List<AuditAction> findActions(AuditActionFilter filter, Sorting<AuditSortBy> sort, PageFilter page) {
        return actions.stream()
            .filter(filter::test)
            .sorted(comparator(sort))
            .skip(page.getOffset())
            .limit(page.getLimit())
            .collect(Collectors.toList());
    }

    private static Comparator<AuditAction> comparator(Sorting<AuditSortBy> sort) {
        if (sort.isDefault()) {
            return Comparator.comparing(AuditAction::getId);
        }

        Comparator<AuditAction> comparator;
        switch (sort.getField()) {
            case ID:
            default:
                comparator = Comparator.comparing(AuditAction::getId);
                break;
            case ACTION_DATE:
                comparator = Comparator.comparing(AuditAction::getActionDate);
                break;
            case STAFF_LOGIN:
                comparator = Comparator.comparing(AuditAction::getStaffLogin);
                break;
        }
        if (sort.getOrder() == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    @Override
    public long count(AuditActionFilter filter) {
        return findActions(filter, Sorting.defaultSorting(), PageFilter.all()).size();
    }
}
