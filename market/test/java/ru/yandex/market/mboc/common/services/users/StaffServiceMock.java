package ru.yandex.market.mboc.common.services.users;

import java.util.HashSet;
import java.util.Set;

/**
 * @author s-ermakov
 */
public class StaffServiceMock implements StaffService {
    private final Set<String> apiUsers = new HashSet<>();
    private boolean allExists;

    public void addApiUser(String staffLogin) {
        this.apiUsers.add(staffLogin);
    }

    @Override
    public boolean checkExists(String staffLogin) {
        return allExists || apiUsers.contains(staffLogin);
    }

    public StaffServiceMock setAllExists(boolean allExists) {
        this.allExists = allExists;
        return this;
    }
}
