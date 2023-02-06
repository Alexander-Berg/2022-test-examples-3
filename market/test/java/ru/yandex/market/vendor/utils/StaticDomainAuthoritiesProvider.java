package ru.yandex.market.vendor.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StaticDomainAuthoritiesProvider {

    private final Map<String, Set<Long>> rolesMap = new HashMap<>();

    public void addRole(long uid, String role) {
        if (!rolesMap.containsKey(role)) {
            rolesMap.put(role, new HashSet<>());
        }
        rolesMap.get(role).add(uid);
    }

    public void reset() {
        rolesMap.clear();
    }

    Map<String, Set<Long>> getRoles() {
        return rolesMap;
    }

}
