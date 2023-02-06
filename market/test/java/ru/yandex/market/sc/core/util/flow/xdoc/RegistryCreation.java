package ru.yandex.market.sc.core.util.flow.xdoc;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.test.SortableTestFactory;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RegistryCreation {

    public static final String REG_EXTERNAL_ID = "ext-reg-918";

    private final Sort sort;
    private final SortableTestFactory sortableTestFactory;
    private final Set<String> pallets = new HashSet<>();
    private final Set<String> boxes = new HashSet<>();
    private String externalId = REG_EXTERNAL_ID;
    private Outbound outbound;

    void outbound(Outbound outbound) {
        this.outbound = outbound;
    }

    public RegistryCreation externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public RegistryCreation addRegistryBoxes(String... barcodes) {
        addPalletsBoxes(Collections.emptySet(), Set.of(barcodes));
        return this;
    }

    public RegistryCreation addRegistryBoxes(int amount) {
        addPalletsBoxes(Collections.emptySet(), XDocFlow.generateBarcodes(amount, XDocFlow.BOX_PREFIX));
        return this;
    }

    public RegistryCreation addRegistryPallets(String... pallets) {
        addPalletsBoxes(Set.of(pallets), Collections.emptySet());
        return this;
    }

    public RegistryCreation addRegistryPallets(int amount) {
        addPalletsBoxes(XDocFlow.generateBarcodes(amount, XDocFlow.PALLET_PREFIX), Collections.emptySet());
        return this;
    }

    public Sort buildRegistry(String... pallets) {
        addPalletsBoxes(Set.of(pallets), Collections.emptySet());
        buildRegistry();
        return sort;
    }

    private void addPalletsBoxes(Set<String> pallets, Set<String> boxes) {
        this.boxes.addAll(boxes);
        this.pallets.addAll(pallets);
    }

    public Sort buildRegistry() {
        this.sortableTestFactory.createOutboundRegistry(
                SortableTestFactory.CreateOutboundRegistryParams.builder()
                        .sortingCenter(this.outbound.getSortingCenter())
                        .registryExternalId(this.externalId)
                        .outboundExternalId(this.outbound.getExternalId())
                        .sortables(List.of())
                        .pallets(this.pallets)
                        .boxes(this.boxes)
                        .build()
        );
        return sort;
    }

    public Outbound buildRegistryAndGetOutbound() {
        this.sortableTestFactory.createOutboundRegistry(
                SortableTestFactory.CreateOutboundRegistryParams.builder()
                        .sortingCenter(this.outbound.getSortingCenter())
                        .registryExternalId(this.externalId)
                        .outboundExternalId(this.outbound.getExternalId())
                        .sortables(List.of())
                        .pallets(this.pallets)
                        .boxes(this.boxes)
                        .build()
        );
        return outbound;
    }
}
