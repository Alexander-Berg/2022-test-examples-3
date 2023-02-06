package ru.yandex.market.core.notification.resolver.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.context.impl.BusinessNotificationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем {@link BusinessContactsResolver}.
 */
@DbUnitDataSet(before = "BusinessRolesContactResolverTest.before.csv")
public class BusinessContactsResolverTest extends FunctionalTest {
    @Autowired
    private BusinessContactsResolver businessContactsResolver;

    @Test
    void testBusinessEmails() {
        assertThat(businessContactsResolver.resolveAddresses("BusinessAdmins",
                new BusinessNotificationContext(10)))
                .containsExactlyInAnyOrder("business@yandex.ru");
    }

    @Test
    void testNoBusinessEmails() {
        assertThat(businessContactsResolver.resolveAddresses("BusinessAdmins",
                new BusinessNotificationContext(20)))
                .isEmpty();
    }
}
