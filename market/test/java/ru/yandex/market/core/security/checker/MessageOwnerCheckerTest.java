package ru.yandex.market.core.security.checker;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.common.util.parameters.ParametersSource;
import ru.yandex.market.core.agency.ContactAndAgencyUserService;
import ru.yandex.market.core.message.MessageService;
import ru.yandex.market.core.message.model.NotificationMessage;
import ru.yandex.market.core.message.model.UserMessageAccess;
import ru.yandex.market.core.message.service.ShopMessageAccessService;
import ru.yandex.market.core.security.model.DualUidable;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Тесты для {@link MessageOwnerChecker}
 */
@ExtendWith(MockitoExtension.class)
public class MessageOwnerCheckerTest {

    private static final Authority AUTHORITY = new Authority();
    private static final long USER_ID = 999L;
    private static final long MESSAGE_ID = 42L;
    private static final long SHOP_ID = 1L;
    private static final long HAS_ACCESS_SHOP_ID = 1L;
    private static final long NO_ACCESS_SHOP_ID = 2L;

    @Mock
    private MessageService messageService;

    @Mock
    private ContactAndAgencyUserService contactAndAgencyUserService;

    @Mock
    private ShopMessageAccessService shopMessageAccessService;

    @Mock
    private EnvironmentService environmentService;

    private MessageOwnerChecker messageOwnerChecker;

    private final ParametersSource parametersSource =
            mock(ParametersSource.class, withSettings().extraInterfaces(DualUidable.class));

    @BeforeEach
    void setUp() {
        messageOwnerChecker = new MessageOwnerChecker(
                messageService, contactAndAgencyUserService, shopMessageAccessService, environmentService);
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                of(false, HAS_ACCESS_SHOP_ID, null, true),
                of(false, NO_ACCESS_SHOP_ID, null, false),
                of(false, HAS_ACCESS_SHOP_ID, 123456789L, false),
                of(true, NO_ACCESS_SHOP_ID, 123456789L, true)
        );
    }

    @ParameterizedTest
    @MethodSource(value = "data")
    public void testMessageAccess(boolean newRoute, long accessShopId, Long accessNotificationType,
                                  boolean expectedResult) {
        when(environmentService.getBooleanValue(eq(MessageOwnerChecker.ENV_NOTIFICATIONS_USE_NEW_ROUTE), anyBoolean()))
                .thenReturn(newRoute);

        when(parametersSource.getParamAsLong(MessageOwnerChecker.PARAM_MESSAGE_ID, 0)).thenReturn(MESSAGE_ID);
        when(((DualUidable) parametersSource).getEffectiveUid()).thenReturn(USER_ID);

        NotificationMessage notificationMessage = new NotificationMessage(SHOP_ID, "subject", "body");
        notificationMessage.setId(MESSAGE_ID);
        when(messageService.getNotificationMessage(MESSAGE_ID, USER_ID))
                .thenReturn(Optional.of(notificationMessage));
        when(shopMessageAccessService.getMessageAccessSetForContact(USER_ID)).thenReturn(Set.of(
                new UserMessageAccess(accessShopId, accessNotificationType)
        ));
        Assertions.assertEquals(messageOwnerChecker.checkTyped(parametersSource, AUTHORITY), expectedResult);
    }

}
