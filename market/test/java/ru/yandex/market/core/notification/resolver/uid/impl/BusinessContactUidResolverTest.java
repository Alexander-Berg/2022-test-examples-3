package ru.yandex.market.core.notification.resolver.uid.impl;


import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.context.IBusinessNotificationContext;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;
import ru.yandex.market.core.notification.exception.AliasResolvingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверим резолв пользователей с ролями в бизнесе.
 */
@DbUnitDataSet(before = "BusinessContactUidResolverTest.before.csv")
class BusinessContactUidResolverTest extends FunctionalTest {
    private static final ThrowsException DEFAULT_ANSWER = new ThrowsException(new RuntimeException());

    @Autowired
    private BusinessContactUidResolver businessContactUidResolver;

    private static IBusinessNotificationContext getMockNotificationContext(long businessId) {
        IBusinessNotificationContext notificationContext = Mockito.mock(IBusinessNotificationContext.class,
                DEFAULT_ANSWER);
        Mockito.doReturn(businessId).when(notificationContext).getBusinessId();
        return notificationContext;
    }

    @Test
    void checkResolveUidsForBusiness() {
        Collection<Long> result = businessContactUidResolver.resolveUids(null, getMockNotificationContext(834448L));
        List<Long> expectedUids = List.of(1002269535L);
        assertThat(result).hasSameElementsAs(expectedUids);
    }

    @Test
    void checkResolveNoUidsForBusiness() {
        Collection<Long> result = businessContactUidResolver.resolveUids(null, getMockNotificationContext(1010L));
        assertThat(result).isEmpty();
    }

    @Test
    void checkResolveUidsForBusinessException() {
        assertThatThrownBy(() -> businessContactUidResolver.resolveUids(null, new ShopNotificationContext(777L)))
                .isInstanceOf(AliasResolvingException.class);
    }
}
