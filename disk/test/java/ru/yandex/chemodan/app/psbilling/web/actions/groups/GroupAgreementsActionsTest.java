package ru.yandex.chemodan.app.psbilling.web.actions.groups;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.web.PsBillingWebTestConfig;
import ru.yandex.chemodan.app.psbilling.web.exceptions.AccessDeniedException;
import ru.yandex.chemodan.app.psbilling.web.model.GroupTypeApi;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryUsersInfoResponse;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.eq;

@ContextConfiguration(classes = {
        PsBillingWebTestConfig.class,
        ru.yandex.chemodan.app.psbilling.web.actions.groups.GroupActionsTest.BalanceMockConfiguration.class
})
public class GroupAgreementsActionsTest extends AbstractPsBillingCoreTest {

    @Autowired
    private GroupAgreementsActions actions;
    @Autowired
    private DirectoryClient directoryClient;

    @Before
    public void setup() {
        featureFlags.getHotFixGroupValidationEnabled().resetValue();
        Mockito.reset(directoryClient);
    }

    @Test
    public void testCheckPermission() {
        featureFlags.getHotFixGroupValidationEnabled().setValue(Boolean.TRUE.toString());
        final String orgId = "BAD_ID";
        final PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);

        Mockito
                .when(directoryClient.getUserInfo(eq(uid.toUid()), eq(orgId)))
                .thenReturn(Option.of(new DirectoryUsersInfoResponse(uid.getUid(), false)));


        Assert.assertThrows(() -> actions.acceptAgreement(uid, GroupTypeApi.ORGANIZATION, orgId, "", Option.empty()),
                AccessDeniedException.class);
    }
}
