package ru.yandex.market.pers.notify.push;

import java.util.HashMap;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.notify.model.push.MobilePlatform;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 16.03.2018
 */
class AppNamePushingServiceTest {
    @Test
    void chooseCorrectPusherService() {
        String appName1 = "appName1";
        String appName21 = "appName21";
        String appName22 = "appName22";
        PusherService service1 = pusherServiceMock();
        PusherService service2 = pusherServiceMock();
        AppNamePushingService appNamePushingService = new AppNamePushingService(new HashMap<String, PusherService>() {{
            put(appName1, service1);
            put(appName21, service2);
            put(appName22, service2);
        }});

        Function<AppNameTest, Void> testAllCases = (test) -> {
            test.testWithReset(appNamePushingService, appName1, service1);
            test.testWithReset(appNamePushingService, appName21, service2);
            test.testWithReset(appNamePushingService, appName22, service2);
            return null;
        };

        testAllCases.apply(this::testRegister);
        testAllCases.apply(this::testUnregister);
    }

    interface AppNameTest {
        default void testWithReset(AppNamePushingService appNamePushingService,
                              String appName, PusherService expectedService) {
            reset(expectedService);
            test(appNamePushingService, appName, expectedService);
        }

        void test(AppNamePushingService appNamePushingService,
                  String appName, PusherService expectedService);
    }

    private void testRegister(AppNamePushingService appNamePushingService,
                              String appName, PusherService expectedService) {
        long userId = 1L;
        String uuid = "uuid1";
        MobilePlatform platform = MobilePlatform.ANDROID;
        String pushToken = "pushToken1";
        appNamePushingService.register(userId, uuid, appName, platform, pushToken);
        verify(expectedService).register(eq(userId), eq(uuid), eq(appName), eq(platform), eq(pushToken));
    }

    private void testUnregister(AppNamePushingService appNamePushingService,
                                String appName, PusherService expectedService) {
        long userId = 1L;
        String uuid = "uuid1";
        appNamePushingService.unregister(userId, uuid, appName);
        verify(expectedService).unregister(eq(userId), eq(uuid));
    }


    private PusherService pusherServiceMock() {
        return mock(PusherService.class);
    }
}
