package ru.yandex.market.tsum.pipelines.test_data;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 05.06.17
 */
public class TestRepositoryFactory {
    private TestRepositoryFactory() {
    }

    public static Repository repository(String owner, String name) {
        Repository repository = new Repository();
        User ownerUser = new User();
        ownerUser.setLogin(owner);
        repository.setOwner(ownerUser);
        repository.setName(name);
        repository.setHtmlUrl("https://github.yandex-team.ru/" + owner + "/" + name);
        return repository;
    }
}
