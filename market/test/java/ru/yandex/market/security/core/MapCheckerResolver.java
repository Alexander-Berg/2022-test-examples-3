/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 28.07.2006</p>
 * <p>Time: 12:17:41</p>
 */

package ru.yandex.market.security.core;

import java.util.Map;

import org.springframework.beans.factory.annotation.Required;

import ru.yandex.market.security.AuthorityChecker;
import ru.yandex.market.security.CheckerResolver;

/**
 * @author Alexey Shevenkov ashevenkov@yandex-team.ru
 */
public class MapCheckerResolver implements CheckerResolver {

    private Map<String, AuthorityChecker> resolvers;

    @Required
    public void setResolvers(Map<String, AuthorityChecker> resolvers) {
        this.resolvers = resolvers;
    }

    public AuthorityChecker resolveChecker(String domain, String authorityName) {
        return resolvers.get(authorityName);
    }
}
