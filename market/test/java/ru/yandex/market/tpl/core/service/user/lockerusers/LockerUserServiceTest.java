package ru.yandex.market.tpl.core.service.user.lockerusers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.locker.boxbot.request.BoxBotUserDto;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.BoxBotUserState;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class LockerUserServiceTest {

    @InjectMocks
    private LockerUserService lockerUserService;

    @Mock
    private LockerDeliveryService lockerDeliveryService;

    @Mock
    private UserRepository userRepository;

    /**
     * Если в ББ нет юзера и у нас он не уволен - должно быть вызвано создание юзера
     */
    @Test
    void processNotExistingTest() {
        var tplUser = mockUser(false);
        var payload = new LockerUserPayload(tplUser.getId(), null);
        var requestCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);

        lockerUserService.processPayload(payload);

        verify(lockerDeliveryService, times(1)).createUser(requestCaptor.capture());
        checkUserData(tplUser, requestCaptor.getValue());
    }

    /**
     * Если в ББ не юзера и у нас он уволен - не должно быть вызвано ничего
     */
    @Test
    void processUpdateFiredNotExistingTest() {
        var tplUser = mockUser(true);
        var payload = new LockerUserPayload(tplUser.getId(), null);

        lockerUserService.processPayload(payload);
        verify(lockerDeliveryService, times(1)).getUser(eq(tplUser.getId()));
        verifyNoMoreInteractions(lockerDeliveryService);
    }

    /**
     * Если в ББ есть юзер и его данные отличаются - должно быть вызвано обновление юзера
     */
    @Test
    void processUpdateExistingTest() {
        var tplUser = mockUser(false);
        var bbUser = createBBUser(tplUser);
        bbUser.setName("old name");
        var payload = new LockerUserPayload(tplUser.getId(), null);
        var requestCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);

        lockerUserService.processPayload(payload);

        verify(lockerDeliveryService, times(1)).updateUser(requestCaptor.capture());
        checkUserData(tplUser, requestCaptor.getValue());
    }

    /**
     * Если в ББ есть юзер, его данные отличаются, у нас он уволен, в ББ нет - должно быть вызвано обновление и удаление
     */
    @Test
    void processUpdateFiredExistingTest() {
        var tplUser = mockUser(true);
        var bbUser = createBBUser(tplUser);
        bbUser.setName("old name");
        bbUser.setState(BoxBotUserState.ACTIVE.getCode());
        var payload = new LockerUserPayload(tplUser.getId(), null);
        var requestCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);

        lockerUserService.processPayload(payload);

        verify(lockerDeliveryService, times(1)).updateUser(requestCaptor.capture());
        verify(lockerDeliveryService, times(1)).deleteUser(requestCaptor.capture());

        requestCaptor.getAllValues().forEach(request -> checkUserData(tplUser, request));
    }

    /**
     * Если юзер есть в ББ, у нас он уволен, данные не поменялись - должно быть вызвано только удаление
     */
    @Test
    void processDeleteExistingTest() {
        var tplUser = mockUser(true);
        var bbUser = createBBUser(tplUser);
        bbUser.setState(BoxBotUserState.ACTIVE.getCode());
        var payload = new LockerUserPayload(tplUser.getId(), null);
        var requestCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);

        lockerUserService.processPayload(payload);

        verify(lockerDeliveryService, times(1)).deleteUser(requestCaptor.capture());
        checkUserData(tplUser, requestCaptor.getValue());
    }

    /**
     * Если данные юзера поменялись, у нас он уволен, в ББ заблокирован, то должен быть вызван только апдейт
     */
    @Test
    void processUpdateDeletedTest() {
        var tplUser = mockUser(true);
        var bbUser = createBBUser(tplUser);
        bbUser.setName("old name");
        var payload = new LockerUserPayload(tplUser.getId(), null);
        var requestCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);

        lockerUserService.processPayload(payload);

        verify(lockerDeliveryService, times(1)).getUser(eq(tplUser.getId()));
        verify(lockerDeliveryService, times(1)).updateUser(requestCaptor.capture());
        verifyNoMoreInteractions(lockerDeliveryService);
        checkUserData(tplUser, requestCaptor.getValue());
    }

    /**
     * При активации и обновлении данных заблокированного в ББ юзера должен быть вызван create метод для снятия
     * блокировки в ББ и update для обновлени данных
     */
    @Test
    void processActivateUserTest() {
        var tplUser = mockUser(false);
        var bbUser = createBBUser(tplUser);
        bbUser.setState(BoxBotUserState.BLOCKED.getCode());
        bbUser.setName("old name");
        var payload = new LockerUserPayload(tplUser.getId(), null);
        var requestCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);

        lockerUserService.processPayload(payload);

        verify(lockerDeliveryService, times(1)).getUser(eq(tplUser.getId()));
        verify(lockerDeliveryService, times(1)).createUser(requestCaptor.capture());
        verify(lockerDeliveryService, times(1)).updateUser(requestCaptor.capture());
        verifyNoMoreInteractions(lockerDeliveryService);
        requestCaptor.getAllValues().forEach(bbDto -> checkUserData(tplUser, bbDto));
    }

    private User mockUser(boolean deleted) {
        var user = mock(User.class);
        when(user.getId()).thenReturn(123L);
        when(user.getUid()).thenReturn(23456L);
        when(user.getName()).thenReturn("user name");
        when(user.getPhone()).thenReturn("+75552225555");
        when(user.isDeleted()).thenReturn(deleted);
        when(userRepository.findByIdOrThrow(eq(user.getId()))).thenReturn(user);
        return user;
    }

    private BoxBotUserDto createBBUser(User tplUser) {
        var user = new BoxBotUserDto(
                tplUser.getId(),
                tplUser.getUid(),
                tplUser.getName(),
                tplUser.getPhone(),
                tplUser.isDeleted() ? BoxBotUserState.BLOCKED.getCode() : BoxBotUserState.ACTIVE.getCode()
        );
        when(lockerDeliveryService.getUser(eq(tplUser.getId()))).thenReturn(user);
        return user;
    }

    private void checkUserData(User tplUser, BoxBotUserDto bbUser) {
        assertThat(bbUser.getExtId()).isEqualTo(tplUser.getId());
        assertThat(bbUser.getUid()).isEqualTo(tplUser.getUid());
        assertThat(bbUser.getName()).isEqualTo(tplUser.getName());
        assertThat(bbUser.getPhone()).isEqualTo(tplUser.getPhone());
    }
}
