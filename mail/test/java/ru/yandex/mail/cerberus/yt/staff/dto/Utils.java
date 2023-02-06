package ru.yandex.mail.cerberus.yt.staff.dto;

import java.util.Set;

public class Utils {
    private Utils() {
    }

    public static StaffDepartmentGroup.StaffDepartment createDepartment(
            StaffLocalizedString staffLocalizedString,
            boolean deleted, Set<StaffDepartmentHead> heads, String url) {
        return new StaffDepartmentGroup.StaffDepartment(
                new StaffDepartmentGroup.StaffDepartment.Name(staffLocalizedString),
                url,
                deleted,
                heads
        );
    }
}
