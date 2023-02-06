package ru.yandex.market.core.notification.service.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.market.core.notification.model.PersistentNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.core.notification.matcher.CollectionEqualsMatcher.equalToCollection;

/**
 * Unit тесты для {@link NotificationTypeCacheContainer}.
 *
 * @author avetokhin 11/01/17.
 */
public class NotificationTypeCacheContainerTest {

    private static final int ROLE_1 = 1;
    private static final int ROLE_2 = 2;
    private static final int ROLE_3 = 3;
    private static final int ROLE_4 = 4;

    private static final PersistentNotificationType NNT_1 =
            new PersistentNotificationType(1L, null, NotificationPriority.NORMAL, null);

    private static final PersistentNotificationType NNT_2 =
            new PersistentNotificationType(2L, null, NotificationPriority.LOW, Stream.of(ROLE_1, ROLE_2).collect(Collectors.toSet()));

    private static final PersistentNotificationType NNT_3 =
            new PersistentNotificationType(3L, null, NotificationPriority.HIGH, Stream.of(ROLE_3).collect(Collectors.toSet()));

    private static final PersistentNotificationType NNT_4 =
            new PersistentNotificationType(4L, null, NotificationPriority.HIGH, Collections.emptySet());

    private static final PersistentNotificationType NNT_5 =
            new PersistentNotificationType(5L, null, NotificationPriority.HIGH, null);

    private static final Collection<PersistentNotificationType> NN_TYPES =
            Arrays.asList(NNT_1, NNT_2, NNT_3, NNT_4, NNT_5);

    private static final List<PersistentNotificationType> BY_ROLE_1 = Collections.singletonList(NNT_2);
    private static final List<PersistentNotificationType> BY_ROLE_2 = Collections.singletonList(NNT_2);
    private static final List<PersistentNotificationType> BY_ROLE_3 = Collections.singletonList(NNT_3);


    @Test
    public void testContainer() {
        final NotificationTypeCacheContainer container = new NotificationTypeCacheContainer(NN_TYPES);

        // Получить все
        assertThat(container.getAll(), equalToCollection(NN_TYPES));

        // Поиск по ID
        assertThat(container.getById(NNT_1.getId()), equalTo(Optional.of(NNT_1)));
        assertThat(container.getById(NNT_2.getId()), equalTo(Optional.of(NNT_2)));
        assertThat(container.getById(NNT_3.getId()), equalTo(Optional.of(NNT_3)));

        // Группированные по ролям
        final Map<Integer, Collection<PersistentNotificationType>> groupedByRole = container.getGropedByRole();
        assertThat(groupedByRole, notNullValue());
        assertThat(groupedByRole.size(), equalTo(3));
        assertThat(groupedByRole.get(ROLE_1), equalToCollection(BY_ROLE_1));
        assertThat(groupedByRole.get(ROLE_2), equalToCollection(BY_ROLE_2));
        assertThat(groupedByRole.get(ROLE_3), equalToCollection(BY_ROLE_3));
    }

}
