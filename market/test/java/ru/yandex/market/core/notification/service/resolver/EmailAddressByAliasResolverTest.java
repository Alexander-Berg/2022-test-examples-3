package ru.yandex.market.core.notification.service.resolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.core.notification.converter.MbiDestinationToContextConverter;
import ru.yandex.market.core.notification.dao.NotificationEmailAliasDao;
import ru.yandex.market.core.notification.exception.AliasResolvingException;
import ru.yandex.market.core.notification.model.NotificationEmailAlias;
import ru.yandex.market.core.notification.resolver.AliasResolver;
import ru.yandex.market.core.notification.service.resolver.destination.MbiDestinationWithAliases;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.notification.mail.model.address.EmailAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link EmailAddressByAliasResolver}.
 *
 * @author avetokhin 28/07/16.
 */
public class EmailAddressByAliasResolverTest {

    private static final Long NOTIFICATION_TYPE = 1L;

    private static final NotificationEmailAlias ALIAS_TO_1 =
            new NotificationEmailAlias(NOTIFICATION_TYPE, "alias_to_1", EmailAddress.Type.TO);
    private static final NotificationEmailAlias ALIAS_TO_2 =
            new NotificationEmailAlias(NOTIFICATION_TYPE, "alias_to_2", EmailAddress.Type.TO);
    private static final NotificationEmailAlias ALIAS_FROM =
            new NotificationEmailAlias(NOTIFICATION_TYPE, "alias_from", EmailAddress.Type.FROM);
    private static final NotificationEmailAlias ALIAS_CC =
            new NotificationEmailAlias(NOTIFICATION_TYPE, "alias_cc", EmailAddress.Type.CC);

    private static final String ADDRESS_1 = "test1@yandex.ru";
    private static final String ADDRESS_2 = "test2@yandex.ru";
    private static final String ADDRESS_3 = "test3@yandex.ru";
    private static final String ADDRESS_4 = "test4@yandex.ru";

    private static final Set<String> RESTRICTED_RECIPIENTS = new HashSet<>();

    static {
        RESTRICTED_RECIPIENTS.add(ADDRESS_1);
        RESTRICTED_RECIPIENTS.add(ADDRESS_3);
    }

    private final MbiDestinationToContextConverter mbiDestinationToContextConverter =
            new MbiDestinationToContextConverter();

    @Test
    public void resolveTest() throws AliasResolvingException {

        final MbiDestination mbiDestination = MbiDestination.create(10L, null, null);
        var context = mbiDestinationToContextConverter.convert(mbiDestination);

        // Подготовить мок для DAO.
        final NotificationEmailAliasDao aliasDao = mock(NotificationEmailAliasDao.class);
        when(aliasDao.getByNotificationType(NOTIFICATION_TYPE))
                .thenReturn(Arrays.asList(ALIAS_FROM, ALIAS_TO_1, ALIAS_TO_2, ALIAS_CC));

        // Подготовить мок для резолвера алиасов.
        final AliasResolver aliasResolver = mock(AliasResolver.class);

        final String aliasesToResolve1 = ALIAS_FROM.getAlias();
        when(aliasResolver.resolveAddresses(eq(aliasesToResolve1), eq(context)))
                .thenReturn(Collections.singleton(ADDRESS_1));

        final String aliasesToResolve2 = ALIAS_TO_1.getAlias();
        when(aliasResolver.resolveAddresses(eq(aliasesToResolve2), eq(context)))
                .thenReturn(Arrays.asList(ADDRESS_2, ADDRESS_3));

        final String aliasesToResolve3 = ALIAS_CC.getAlias();
        when(aliasResolver.resolveAddresses(eq(aliasesToResolve3), eq(context)))
                .thenReturn(Collections.singleton(ADDRESS_4));

        // ADDRESS_3 - должен быть отфильтрован, так как он в RESTRICTED,
        // однако ADDRESS_1 останется - так как его тип FROM.
        final Set<EmailAddress> expectedResult = new HashSet<>(Arrays.asList(
                EmailAddress.create(ADDRESS_1, EmailAddress.Type.FROM),
                EmailAddress.create(ADDRESS_2, EmailAddress.Type.TO),
                EmailAddress.create(ADDRESS_4, EmailAddress.Type.CC)
        ));

        final EmailAddressByAliasResolver resolver =
                new EmailAddressByAliasResolver(aliasDao, aliasResolver, RESTRICTED_RECIPIENTS,
                        new MbiDestinationToContextConverter());

        final Set<EmailAddress> result = resolver.resolve(NOTIFICATION_TYPE, mbiDestination);

        // Верификация вызовов.
        verify(aliasDao).getByNotificationType(NOTIFICATION_TYPE);
        verify(aliasResolver).resolveAddresses(eq(aliasesToResolve1), eq(context));
        verify(aliasResolver).resolveAddresses(eq(aliasesToResolve2), eq(context));
        verify(aliasResolver).resolveAddresses(eq(aliasesToResolve3), eq(context));

        // Проверка результата.
        assertThat(result, equalTo(expectedResult));
    }

    @Test
    public void resolveMbiDestinationWithAliasesTest() throws AliasResolvingException {

        final MbiDestination mbiDestination = MbiDestinationWithAliases.create(
                10L,
                null, null, null,
                Arrays.asList(ALIAS_FROM, ALIAS_TO_1, ALIAS_TO_2, ALIAS_CC)
        );
        var context = mbiDestinationToContextConverter.convert(mbiDestination);

        // Подготовить мок для DAO.
        final NotificationEmailAliasDao aliasDao = mock(NotificationEmailAliasDao.class);

        // Подготовить мок для резолвера алиасов.
        final AliasResolver aliasResolver = mock(AliasResolver.class);

        final String aliasesToResolve1 = ALIAS_FROM.getAlias();
        when(aliasResolver.resolveAddresses(eq(aliasesToResolve1), eq(context)))
                .thenReturn(Collections.singleton(ADDRESS_1));

        final String aliasesToResolve2 = ALIAS_TO_1.getAlias();
        when(aliasResolver.resolveAddresses(eq(aliasesToResolve2), eq(context)))
                .thenReturn(Arrays.asList(ADDRESS_2, ADDRESS_3));

        final String aliasesToResolve3 = ALIAS_CC.getAlias();
        when(aliasResolver.resolveAddresses(eq(aliasesToResolve3), eq(context)))
                .thenReturn(Collections.singleton(ADDRESS_4));

        // ADDRESS_3 - должен быть отфильтрован, так как он в RESTRICTED,
        // однако ADDRESS_1 останется - так как его тип FROM.
        final Set<EmailAddress> expectedResult = new HashSet<>(Arrays.asList(
                EmailAddress.create(ADDRESS_1, EmailAddress.Type.FROM),
                EmailAddress.create(ADDRESS_2, EmailAddress.Type.TO),
                EmailAddress.create(ADDRESS_4, EmailAddress.Type.CC)
        ));

        final EmailAddressByAliasResolver resolver =
                new EmailAddressByAliasResolver(aliasDao, aliasResolver, RESTRICTED_RECIPIENTS,
                        new MbiDestinationToContextConverter());

        final Set<EmailAddress> result = resolver.resolve(NOTIFICATION_TYPE, mbiDestination);

        // Верификация вызовов.
        verify(aliasDao, never()).getByNotificationType(any());
        verify(aliasResolver).resolveAddresses(eq(aliasesToResolve1), eq(context));
        verify(aliasResolver).resolveAddresses(eq(aliasesToResolve2), eq(context));
        verify(aliasResolver).resolveAddresses(eq(aliasesToResolve3), eq(context));

        // Проверка результата.
        assertThat(result, equalTo(expectedResult));
    }

}
