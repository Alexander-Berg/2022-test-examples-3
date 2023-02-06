package ru.yandex.market.reporting.generator.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import ru.yandex.market.reporting.common.domain.tree.Region;
import ru.yandex.market.reporting.generator.service.domain.SelectedTreeItem;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
public class SelectedTreeItemTest {

    @Test
    public void findExclusions() throws Exception {
        Region москва = new Region(1011L, "Москва", 0L, ImmutableList.of());
        Region мо = new Region(101L, "Москва и МО", 0L, ImmutableList.of(
                москва
        ));
        Region свердлобл = new Region(102L, "Свердловская область", 0L, ImmutableList.of());
        Region россия = new Region(10L, "Россия", 0L, ImmutableList.of(
                мо,
                свердлобл

        ));
        Region украина = new Region(11L, "Украина", 0L, ImmutableList.of());
        Region земля = new Region(1L, "Земля", 0L, ImmutableList.of(
                россия,
                украина
        ));

        ImmutableSet<Long> selectedRegions = ImmutableSet.of(10L, 11L, 101L, 1011L);

        List<SelectedTreeItem<Region>> selectedTreeItems = SelectedTreeItem.findExclusions(земля, selectedRegions);

        assertThat(selectedTreeItems.stream().map(r -> r.getItem().getId()).collect(toList()),
                containsInAnyOrder(selectedRegions.toArray()));

        assertThat(selectedTreeItems.stream()
                        .filter(r -> r.getItem() == россия)
                        .flatMap(r -> r.getExcludeDescendants().stream())
                        .map(SelectedTreeItem::getItem)
                        .collect(toList()),
                containsInAnyOrder(москва, мо));
    }

}