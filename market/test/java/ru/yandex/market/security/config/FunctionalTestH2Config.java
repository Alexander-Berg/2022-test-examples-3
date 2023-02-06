package ru.yandex.market.security.config;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import ru.yandex.market.common.test.spring.H2Config;

import static ru.yandex.market.security.data.JavaSecTest.script;

@Configuration
public class FunctionalTestH2Config extends H2Config {
    private static final List<Resource> CORE_TABLES_LIST = Arrays.asList(
            script("schema.sql"),
            script("S_ID.sql"),
            script("AUTH_LINK.sql"),
            script("AUTH_LINK_AUDIT.sql"),
            script("AUTH_LINK_TEMP.sql"),
            script("AUTH_LINK_TEMP_AUDIT.sql"),
            script("AUTHORITY.sql"),
            script("AUTHORITY_AUDIT.sql"),
            script("AUTHORITY_CHECKER.sql"),
            script("AUTHORITY_CHECKER_AUDIT.sql"),
            script("DOMAIN.sql"),
            script("DOMAIN_ADMINS.sql"),
            script("DOMAIN_ADMINS_AUDIT.sql"),
            script("DOMAIN_AUDIT.sql"),
            script("MIGRATION_HISTORY.sql"),
            script("OP_DESC.sql"),
            script("OP_DESC_AUDIT.sql"),
            script("OP_PERM.sql"),
            script("OP_PERM_AUDIT.sql"),
            script("PERM_AUTH.sql"),
            script("PERM_AUTH_AUDIT.sql"),
            script("SERVICES_DOMAINS.sql"),
            script("SERVICES_TREE.sql"),
            script("STAFF_USERS_INFO.sql"),
            script("STATIC_AUTH.sql"),
            script("STATIC_AUTH_AUDIT.sql"),
            script("STATIC_DOMAIN_AUTH.sql"),
            script("STATIC_DOMAIN_AUTH_AUDIT.sql"),
            script("TESTERS.sql"),
            script("TMP_AUTH_CHECKER.sql"),
            script("USER_PASSPORTS.sql"));


    @Nonnull
    @Override
    protected List<Resource> databaseResources() {
        return CORE_TABLES_LIST;
    }

}
