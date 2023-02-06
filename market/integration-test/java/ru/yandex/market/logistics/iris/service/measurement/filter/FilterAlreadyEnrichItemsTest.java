package ru.yandex.market.logistics.iris.service.measurement.filter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;

public class FilterAlreadyEnrichItemsTest extends AbstractContextualTest {

    @Autowired
    private FilterAlreadyEnrichItems filterAlreadyEnrichItems;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/measurement/filter/1.xml")
    public void shouldSuccessExcludeOneItems() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 13);

        Set<ItemIdentifier> expectedIdentifiers = filterAlreadyEnrichItems.filter(createItemIdentifiers(), fromDate);

        assertions().assertThat(expectedIdentifiers).hasSize(2);
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("1", "sku2"))).isTrue();
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("2", "sku2"))).isTrue();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/measurement/filter/1.xml")
    public void shouldSuccessExcludeTwoItems() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 11);

        Set<ItemIdentifier> expectedIdentifiers = filterAlreadyEnrichItems.filter(createItemIdentifiers(), fromDate);

        assertions().assertThat(expectedIdentifiers).hasSize(1);
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("2", "sku2"))).isTrue();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/measurement/filter/empty.xml")
    public void shouldNotExcludeIfMeasurementAuditIsEmpty() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 11);

        Set<ItemIdentifier> expectedIdentifiers = filterAlreadyEnrichItems.filter(createItemIdentifiers(), fromDate);

        assertions().assertThat(expectedIdentifiers).hasSize(3);
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("1", "sku1"))).isTrue();
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("1", "sku2"))).isTrue();
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("2", "sku2"))).isTrue();
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/measurement/filter/1.xml")
    public void shouldNotExcludeAnyOneIfListOfInputIdentifiersIsEmpty() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 11);

        Set<ItemIdentifier> expectedIdentifiers = filterAlreadyEnrichItems.filter(Collections.emptySet(), fromDate);

        assertions().assertThat(expectedIdentifiers).hasSize(0);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/measurement/filter/2.xml")
    public void shouldSuccessExcludeIfMeasurementAuditContainsDuplicate() {
        final LocalDate fromDate = LocalDate.of(2020, 9, 13);

        Set<ItemIdentifier> expectedIdentifiers = filterAlreadyEnrichItems.filter(createItemIdentifiers(), fromDate);

        assertions().assertThat(expectedIdentifiers).hasSize(2);
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("1", "sku2"))).isTrue();
        assertions().assertThat(expectedIdentifiers.contains(ItemIdentifier.of("2", "sku2"))).isTrue();
    }

    private Set<ItemIdentifier> createItemIdentifiers() {
        return ImmutableSet.of(
                ItemIdentifier.of("1", "sku1"),
                ItemIdentifier.of("1", "sku2"),
                ItemIdentifier.of("2", "sku2"));
    }
}
