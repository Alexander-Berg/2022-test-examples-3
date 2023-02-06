package ru.yandex.market.tsup.service.data_provider.author;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.model.CreatedEntityType;
import ru.yandex.market.tsup.service.user_process.AuthorFilter;
import ru.yandex.market.tsup.service.user_process.AuthorsMap;
import ru.yandex.market.tsup.service.user_process.EntityAuthorProvider;

import static org.assertj.core.api.Assertions.assertThat;

@DatabaseSetup("/repository/entity_author/before/entity_author_collection.xml")
public class EntityAuthorProviderTest extends AbstractContextualTest {

    @Autowired
    private EntityAuthorProvider entityAuthorProvider;

    @Test
    void onlyRequiredIds() {
        var actual = provide(new AuthorFilter(Set.of(1L, 2L), CreatedEntityType.ROUTE_SCHEDULE));
        assertThat(actual)
            .isEqualTo(
                Map.of(
                    "1", "staff-login-1",
                    "2", "staff-login-2"
                )
            );
    }

    @Test
    void onEmptyIdsFilterReturnAllEntityAuthorsForEntityType() {
        var actual = provide(new AuthorFilter(Collections.emptySet(), CreatedEntityType.ROUTE));
        assertThat(actual)
            .isEqualTo(
                Map.of(
                    "1", "staff-login-1",
                    "2", "staff-login-2",
                    "3", "staff-login-3"
                )
            );
    }

    private Map<String, String> provide(AuthorFilter filter) {
        AuthorsMap authorsMap = entityAuthorProvider.provide(filter, null);
        return authorsMap.entryStream().toMap();
    }

}
