package ru.yandex.market.mbo.db.transfer;


import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.DestinationCategory;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.SourceCategory;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.UserManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 09.09.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class UserServiceTest {
    private Map<Long, MboUser> users = new HashMap<>();
    private UserService userService;

    @Before
    public void setUp() throws Exception {
        for (long i = 1; i < 10; i++) {
            putUser(new MboUser("login" + i, i, "fullname" + i, "email" + i, "stuffLogin" + i));
        }
        UserManager userManager = mock(UserManager.class);
        when(userManager.getUserInfo(anyLong())).thenAnswer(args -> {
            long uid = args.getArgument(0);
            return users.get(uid);
        });
        userService = new UserService(userManager);
    }

    @Test
    public void testUpdateModelTransferUsers() {
        ModelTransferBuilder builder = ModelTransferBuilder.newBuilder()
            .author(new User(1L))
            .manager(new User(2L))
            .userModified(new User(3L));

        SourceCategory sc1 = new SourceCategory();
        sc1.setUserModified(new User(4L));
        SourceCategory sc2 = new SourceCategory();
        sc2.setUserModified(new User(5L));
        builder.sourceCategory(sc1).sourceCategory(sc2);

        DestinationCategory dc1 = new DestinationCategory();
        dc1.setUserModified(new User(6L));
        DestinationCategory dc2 = new DestinationCategory();
        dc2.setUserModified(new User(7L));
        builder.destinationCategory(dc1).destinationCategory(dc2);

        ModelTransfer modelTransfer = builder.build();

        userService.updateModelTransferUsers(modelTransfer);

        userService.getModelTransferUsers(modelTransfer).forEach((uid, user) ->
            assertThat(userEquals(user, users.get(uid))).isTrue());
    }

    @Test
    public void testUpdateModelTransferStepInfo() {
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setResponsibleUser(new User(1L));
        stepInfo.setUserModified(new User(2L));

        ResultInfo ri1 = new ResultInfo();
        ri1.setUser(new User(3L));
        ResultInfo ri2 = new ResultInfo();
        ri2.setUser(new User(4L));
        stepInfo.setExecutionResultInfos(Arrays.asList(ri1, ri2));

        ResultInfo ri3 = new ResultInfo();
        ri3.setUser(new User(5L));
        ResultInfo ri4 = new ResultInfo();
        ri4.setUser(new User(6L));
        stepInfo.setValidationResultInfos(Arrays.asList(ri3, ri4));

        userService.updateStepInfoUsers(stepInfo);

        userService.getStepInfoUsers(stepInfo).forEach((uid, user) ->
            assertThat(userEquals(user, users.get(uid))).isTrue());
    }

    private void putUser(MboUser user) {
        users.put(user.getUid(), user);
    }

    private boolean userEquals(User user, MboUser mboUser) {
        return user.getId() == mboUser.getUid()
            && user.getLogin().equals(mboUser.getLogin())
            && user.getName().equals(mboUser.getFullname());
    }
}
