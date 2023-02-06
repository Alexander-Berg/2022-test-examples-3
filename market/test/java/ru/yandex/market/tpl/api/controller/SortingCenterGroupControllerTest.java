package ru.yandex.market.tpl.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.mapper.SortingCenterGroupMapper;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroup;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroupRepository;
import ru.yandex.market.tpl.core.service.partner.SortingCenterGroupService;
import ru.yandex.market.tpl.server.model.SortingCenterGroupDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SortingCenterGroupControllerTest extends BaseApiTest {
    private final SortingCenterGroupRepository sortingCenterGroupRepository;
    private final SortingCenterGroupService sortingCenterGroupService;
    private final SortingCenterGroupMapper sortingCenterGroupMapper;

    @Test
    void sortingCenterGroupsGet() {
        List<String> names = new ArrayList<>();
        names.add(createSortingCenterGroup("Тест 1", false).getName());
        names.add(createSortingCenterGroup("Тест 2", false).getName());
        createSortingCenterGroup("Тест 3", true);
        createSortingCenterGroup("Тест 4", true);
        names.add(createSortingCenterGroup("Тест 5", false).getName());
        var result = sortingCenterGroupMapper.map(sortingCenterGroupService.findAll());
        List<String> namesResult =
                result.getData().stream().map(SortingCenterGroupDto::getName).collect(Collectors.toList());
        Assertions.assertThat(namesResult).containsExactlyInAnyOrderElementsOf(names);
    }


    @Transactional
    private SortingCenterGroup createSortingCenterGroup(String name, boolean delete) {
        SortingCenterGroup sortingCenterGroup = new SortingCenterGroup();
        sortingCenterGroup.setName(name);
        sortingCenterGroup.setDeleted(delete);
        return sortingCenterGroupRepository.save(sortingCenterGroup);
    }

}
