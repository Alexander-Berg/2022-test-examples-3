package ru.yandex.market.markup2.utils;

import com.google.common.base.Preconditions;
import org.mockito.Mockito;
import org.springframework.scheduling.annotation.AsyncResult;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.utils.users.UsersService;
import ru.yandex.market.mbo.users.MboUsers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 30.08.2019
 */
public class Mocks {

    private Mocks() { }

    public static TovarTreeProvider mockTovarTreeProvider() {
        TovarTreeProvider tovarTreeProvider = Mockito.mock(TovarTreeProvider.class);
        when(tovarTreeProvider.getCategoryName(anyInt())).thenAnswer(invocation -> {
            int categoryId = invocation.getArgument(0);
            return "Cat" + categoryId;
        });
        when(tovarTreeProvider.getUniqueCategoryName(anyInt())).thenAnswer(invocation -> {
            int categoryId = invocation.getArgument(0);
            return "Unique cat" + categoryId;
        });
        return tovarTreeProvider;
    }

    public static UsersService mockUsersService(Map<String, Long> userMap) {
        UsersService usersService = Mockito.mock(UsersService.class);
        when(usersService.getUidByWorkerIdOrDefault(anyString()))
            .then(invocation -> Preconditions.checkNotNull(userMap.get(invocation.<String>getArgument(0)),
                "Can't find mapping for workerId %s", invocation.<String>getArgument(0)));
        when(usersService.getUserByWorkerId(Mockito.anyString())).then(invocation -> {
            String workerId = invocation.getArgument(0);
            Long uid = userMap.get(workerId);
            if (uid != null) {
                return MboUsers.MboUser.newBuilder()
                        .setUid(uid)
                        .setStaffLogin("staff_" + uid)
                        .build();
            }
            return null;
        });
        when(usersService.convertToWorkerIdsOrThrow(Mockito.anyList())).then(invocation -> {
            List<Long> users = invocation.getArgument(0);
            List<String> result = userMap.entrySet().stream()
                .filter(e -> users.contains(e.getValue()))
                .map(e -> e.getKey())
                .collect(Collectors.toList());
            if (result.size() < users.size()) {
                throw new IllegalArgumentException();
            }
            return result;
        });

        return usersService;
    }


    public static ScheduledExecutorService instantExecutorService() {
        ScheduledExecutorService result = Mockito.mock(ScheduledExecutorService.class);
        when(result.submit(Mockito.any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return new AsyncResult<>(null);
        });

        when(result.submit(Mockito.any(Callable.class))).thenAnswer(invocation -> {
            Callable callable = invocation.getArgument(0);
            return new AsyncResult<>(callable.call());
        });
        return result;
    }
}
