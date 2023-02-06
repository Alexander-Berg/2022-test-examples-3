package ru.yandex.market.sc.core.util.flow.xdoc;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.sortable.SortableId;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SortableCreation {

    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableRepository sortableRepository;

    private String barcode;
    private SortableType sortableType;
    private Inbound inbound;

    public SortableCreation barcode(String barcode) {
        this.barcode = barcode;
        return this;
    }

    public SortableCreation sortableType(SortableType sortableType) {
        this.sortableType = sortableType;
        return this;
    }

    public SortableCreation inbound(Inbound inbound) {
        this.inbound = inbound;
        return this;
    }

    public SortableId create() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        return sortableTestFactory.createSortable(barcode, sortableType, inbound, sc);
    }

    public Sortable createAndGet() {
        var sortableId = create();
        return sortableRepository.findById(sortableId.getId())
                .orElseThrow(() -> new TplIllegalStateException("Error during sortable creation"));
    }
}
