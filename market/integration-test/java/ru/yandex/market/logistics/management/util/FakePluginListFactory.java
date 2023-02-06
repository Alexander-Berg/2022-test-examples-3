package ru.yandex.market.logistics.management.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.yandex.market.logistics.admin.pluggable.MenuPage;
import ru.yandex.market.logistics.admin.pluggable.Page;
import ru.yandex.market.logistics.admin.pluggable.Plugin;
import ru.yandex.market.logistics.admin.pluggable.Role;
import ru.yandex.market.logistics.front.library.dto.ViewType;

public final class FakePluginListFactory {
    private static final String PLUGIN_NAME = "test";

    private static final String USER_AUTHORITY = "ROLE_USER_AUTHORITY";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN_AUTHORITY";

    private static final String USER_CODE = "USER";
    private static final String ADMIN_CODE = "ADMIN";

    private FakePluginListFactory() {
        throw new UnsupportedOperationException();
    }

    public static List<Plugin> getPluginList() {
        return Collections.singletonList(new Plugin() {
            @Override
            public String getName() {
                return PLUGIN_NAME;
            }

            @Override
            public String getDescription() {
                return "pluginDescription";
            }

            @Override
            public String getSlug() {
                return PLUGIN_NAME;
            }

            @Override
            public Set<Role> getRoles() {
                Role roleUser = new Role(
                    USER_CODE,
                    "Досутп на просмотр",
                    USER_AUTHORITY
                );

                Role roleAdmin = new Role(
                    ADMIN_CODE,
                    "Админский доступ",
                    ADMIN_AUTHORITY
                );

                Set<Role> roles = new HashSet<>();

                roles.add(roleUser);
                roles.add(roleAdmin);

                return roles;
            }

            @Override
            public List<Page> getAllPages() {
                MenuPage userMenu = new MenuPage(
                    USER_CODE.toLowerCase() + " item slug",
                    USER_CODE.toLowerCase() + " item title",
                    ViewType.GRID,
                    USER_AUTHORITY
                );

                MenuPage adminMenu = new MenuPage(
                    ADMIN_CODE.toLowerCase() + " item slug",
                    ADMIN_CODE.toLowerCase() + " item title",
                    ViewType.DEFAULT,
                    ADMIN_AUTHORITY
                );

                return Arrays.asList(userMenu, adminMenu);
            }
        });
    }
}
