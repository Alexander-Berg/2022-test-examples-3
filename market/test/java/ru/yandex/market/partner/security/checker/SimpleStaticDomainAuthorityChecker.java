package ru.yandex.market.partner.security.checker;

import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.security.checker.abs.ReflectionTypedChecker;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.Uidable;

/**
 * Простой читатель idm-ных ролей из БД. Здесь нужен для тестов, так как в classpath нет адекватного читателя
 * static_domain_auth, которым можно было бы заменить HttpStaticDomainAuthoritiesLoader.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class SimpleStaticDomainAuthorityChecker extends ReflectionTypedChecker<Uidable> {

    private final JdbcTemplate jdbcTemplate;

    public SimpleStaticDomainAuthorityChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected boolean checkTyped(Uidable data, Authority authority) {
        //noinspection ConstantConditions
        return jdbcTemplate.queryForObject("select count(*) " +
                        " from java_sec.static_domain_auth " +
                        " join java_sec.domain on domain.id = static_domain_auth.domain_id" +
                        "  where domain.name = ? and auth_name = ? and user_id = ?",
                Boolean.class,
                authority.getDomain(), authority.getName(), data.getUid());
    }
}
