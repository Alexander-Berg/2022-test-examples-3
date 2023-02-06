package ru.yandex.market.tpl.core.domain.user;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyEntity;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
public class UserPropertyServiceTest extends TplAbstractTest {

    private final UserPropertyRepository userPropertyRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TestUserHelper testUserHelper;


    private final UserPropertyService userPropertyService;

    @Test
    public void findUsersByPropertyUserPropertyTest() {
        userPropertyRepository.saveAll(List.of(
                UserPropertyEntity.builder()
                        .name(UserProperties.CUSTOM_VERSION_NUMBER.getName())
                        .user(testUserHelper.findOrCreateUser(1L))
                        .value("1.09")
                        .type(TplPropertyType.STRING)
                        .build(),
                UserPropertyEntity.builder()
                        .name(UserProperties.CUSTOM_VERSION_NUMBER.getName())
                        .user(testUserHelper.findOrCreateUser(2L))
                        .value("2.28")
                        .type(TplPropertyType.STRING)
                        .build()));

        Map<User, String> result = Map.of(
                testUserHelper.findOrCreateUser(1L), "1.09",
                testUserHelper.findOrCreateUser(2L), "2.28");

        Assertions.assertEquals(userPropertyService
                .findUsersByPropertyUserProperty(UserProperties.CUSTOM_VERSION_NUMBER), result);
    }

    @Test
    public void findUsersByPropertySortingCenterPropertyTest() {
        sortingCenterPropertyService.save(SortingCenterPropertyEntity.builder()
                .name(UserProperties.CUSTOM_VERSION_NUMBER.getName())
                .type(TplPropertyType.STRING)
                .sortingCenter(testUserHelper.sortingCenter(1L))
                .value("2.0")
                .build());

        testUserHelper.createEmptyShift(testUserHelper.findOrCreateUser(3L),
                testUserHelper.findOrCreateShiftForScWithStatus(LocalDate.now(),
                        1L, ShiftStatus.OPEN));

        testUserHelper.createEmptyShift(testUserHelper.findOrCreateUser(4L),
                testUserHelper.findOrCreateShiftForScWithStatus(LocalDate.now(),
                        1L, ShiftStatus.OPEN));

        Map<User, String> result = Map.of(
                testUserHelper.findOrCreateUser(3L), "2.0",
                testUserHelper.findOrCreateUser(4L), "2.0");

        Assertions.assertEquals(userPropertyService
                .findUsersByPropertySortingCenterProperty(UserProperties.CUSTOM_VERSION_NUMBER), result);
    }

    @Test
    public void findUsersByProperty() {
        userPropertyRepository.save(
                UserPropertyEntity.builder()
                        .name(UserProperties.CUSTOM_VERSION_NUMBER.getName())
                        .user(testUserHelper.findOrCreateUser(1L))
                        .value("1.09")
                        .type(TplPropertyType.STRING)
                        .build());

        sortingCenterPropertyService.save(SortingCenterPropertyEntity.builder()
                .name(UserProperties.CUSTOM_VERSION_NUMBER.getName())
                .type(TplPropertyType.STRING)
                .sortingCenter(testUserHelper.sortingCenter(1L))
                .value("2.0")
                .build());

        testUserHelper.createEmptyShift(testUserHelper.findOrCreateUser(1L),
                testUserHelper.findOrCreateShiftForScWithStatus(LocalDate.now(),
                        1L, ShiftStatus.OPEN));
        Map<User, String> customVersionsMap = new HashMap<>();
        userPropertyService.findUsersByPropertyUserProperty(UserProperties.CUSTOM_VERSION_NUMBER).forEach(
                customVersionsMap::putIfAbsent
        );
        userPropertyService.findUsersByPropertySortingCenterProperty(UserProperties.CUSTOM_VERSION_NUMBER).forEach(
                customVersionsMap::putIfAbsent
        );
        Map<User, String> result = Map.of(
                testUserHelper.findOrCreateUser(1L), "1.09");
        Assertions.assertEquals(customVersionsMap, result);

    }

}
