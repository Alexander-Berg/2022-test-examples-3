package ru.yandex.mail.cerberus.yt.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.mail.cerberus.GroupId;
import ru.yandex.mail.cerberus.client.dto.Group;
import ru.yandex.mail.cerberus.yt.data.YtDepartmentInfo;
import ru.yandex.mail.cerberus.yt.staff.StaffConstants;
import ru.yandex.mail.cerberus.yt.staff.dto.DtoUtils;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDepartmentGroup;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffDepartmentHead;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class DefaultYtDepartmentMapperTest {
    @Inject
    private YtDepartmentMapper mapper;

    private static final GroupId DEPARTMENT_ID = new GroupId(42L);
    private static final long CHIEF_UID = 12312312L;
    private static final Meta META = new Meta(OffsetDateTime.MAX);
    private static final StaffDepartmentGroup STAFF_DEPARTMENT_GROUP;

    static {
        final StaffLocalizedString staffLocalizedString =
                new StaffLocalizedString("Департамент", "Department");
        STAFF_DEPARTMENT_GROUP = createStaffDepartmentGroup(emptyList(),
                DtoUtils.createDepartment(staffLocalizedString, false, Set.of(
                        new StaffDepartmentHead(new StaffDepartmentHead.Person(String.valueOf(CHIEF_UID)))
                )));
    }

    @NotNull
    private static StaffDepartmentGroup createStaffDepartmentGroup(List<StaffDepartmentGroup.Ancestor> ancestors,
                                                                   StaffDepartmentGroup.@NotNull StaffDepartment department) {
        return new StaffDepartmentGroup(
                META,
                DEPARTMENT_ID,
                "name",
                "url",
                StaffDepartmentGroup.Types.DEPARTMENT,
                department,
                ancestors,
                false
        );
    }

    @Test
    @DisplayName("Verify that 'mapToDepartmentData' return correct department data")
    void testMapToDepartmentData() {
        val expectedGroup = new Group<>(
                DEPARTMENT_ID,
                StaffConstants.YT_DEPARTMENT_GROUP_TYPE,
                "Department",
                true,
                new YtDepartmentInfo(Set.of(CHIEF_UID), "url", "Департамент", "Department")
        );
        val data = mapper.mapToGroup(STAFF_DEPARTMENT_GROUP);
        assertThat(data).isEqualTo(expectedGroup);
    }

    @Test
    @DisplayName("Verify that 'mapToDepartmentData' returns next level chief in a case when the department has no own" +
            " one")
    void testMapToDepartmentDataForDepartmentWithoutOwnChief() {
        val departmentGroup = createStaffDepartmentGroup(List.of(
                new StaffDepartmentGroup.Ancestor(
                        false,
                        new StaffDepartmentGroup.Ancestor.Info(
                                Set.of(new StaffDepartmentHead(new StaffDepartmentHead.Person(String.valueOf(CHIEF_UID)))),
                                1
                        )
                )
                ),
                DtoUtils.createDepartment(STAFF_DEPARTMENT_GROUP.getDepartment().getName().getFull(), false, emptySet()));
        val data = mapper.mapToGroup(departmentGroup);
        assertThat(data.getInfo())
                .hasValueSatisfying(info -> {
                    assertThat(info.getChiefUids()).containsExactly(CHIEF_UID);
                });
    }

    @Test
    @DisplayName("Verify that 'mapToDepartmentData' return correct department data")
    void testMapToServiceData() {
        val expectedGroup = new Group<>(
                DEPARTMENT_ID,
                StaffConstants.YT_DEPARTMENT_GROUP_TYPE,
                "Department",
                true,
                new YtDepartmentInfo(Set.of(), "url", "Department", "Department")
        );
        final StaffDepartmentGroup serviceGroup = new StaffDepartmentGroup(
                META,
                DEPARTMENT_ID,
                "Department",
                "url",
                StaffDepartmentGroup.Types.SERVICE,
                DtoUtils.createEmptyDepartament(),
                emptyList(),
                false
        );
        val data = mapper.mapToGroup(serviceGroup);
        assertThat(data).isEqualTo(expectedGroup);
    }
}
