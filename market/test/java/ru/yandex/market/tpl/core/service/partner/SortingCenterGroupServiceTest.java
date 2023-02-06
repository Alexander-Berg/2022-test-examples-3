package ru.yandex.market.tpl.core.service.partner;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroup;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Slf4j
public class SortingCenterGroupServiceTest extends TplAbstractTest {

    private final SortingCenterGroupService sortingCenterGroupService;
    private final static String SC_GROUP_NAME = "СЦ Тестонский";

    @Test
    void shouldCreateSortingCenterGroup() {
        SortingCenterGroup sortingCenterGroup = new SortingCenterGroup();
        sortingCenterGroup.setName(SC_GROUP_NAME);
        sortingCenterGroupService.save(sortingCenterGroup);

        List<SortingCenterGroup> sortingCenterGroups = sortingCenterGroupService.findAll();

        assertThat(sortingCenterGroups).hasSize(1);
        assertThat(sortingCenterGroups.get(0).getName()).isEqualTo(SC_GROUP_NAME);

    }
}
