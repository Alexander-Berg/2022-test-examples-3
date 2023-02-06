package ru.yandex.mail.cerberus.yt.staff.dto;

import java.util.Set;

public class DtoUtils {
    private DtoUtils() {
    }

    public static StaffDepartmentGroup.StaffDepartment createDepartment(
            StaffLocalizedString staffLocalizedString,
            boolean deleted, Set<StaffDepartmentHead> heads) {
        return new StaffDepartmentGroup.StaffDepartment(
                new StaffDepartmentGroup.StaffDepartment.Name(staffLocalizedString),
                "url",
                deleted,
                heads
        );
    }

    public static StaffDepartmentGroup.StaffDepartment createEmptyDepartament() {
        return new StaffDepartmentGroup.StaffDepartment(
                null,
                null,
                false,
                null
        );
    }
}
