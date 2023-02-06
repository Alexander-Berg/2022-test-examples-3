package ru.yandex.market.tpl.api.facade;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.model.user.pro.ProUserDto;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxDisplayAvatar;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxDisplayDto;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxDisplayName;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserFacadeTest extends BaseApiTest {

    private final UserFacade userFacade;

    private final TestUserHelper testUserHelper;

    private final TransactionTemplate transactionTemplate;

    private final UserRepository userRepository;

    private final BlackboxClient blackboxClient;


    @Test
    void getForPro() {
        var blackBoxResult = new BlackboxDisplayDto();
        var displayName = new BlackboxDisplayName();
        var displayAvatar = new BlackboxDisplayAvatar();
        displayAvatar.setEmpty(false);
        displayAvatar.setAvatarId("ajssfsdfgdfhgdfgs");
        displayName.setAvatar(displayAvatar);
        blackBoxResult.setDisplayName(displayName);
        Mockito.doReturn(blackBoxResult).when(blackboxClient).getDisplayInfo(Mockito.anyString());

        User user = testUserHelper.findOrCreateUser(47603269L);
        ProUserDto result = userFacade.getForPro(user);

        transactionTemplate.execute(status -> {
            User userUpdate = userRepository.getById(user.getId());
            Assertions.assertThat(result.getUid()).isEqualTo(userUpdate.getUid());
            Assertions.assertThat(result.getEmail()).isEqualTo(userUpdate.getEmail());
            Assertions.assertThat(result.getName()).isEqualTo(userUpdate.getName());
            Assertions.assertThat(result.getRole()).isEqualTo(userUpdate.getRole());
            Assertions.assertThat(result.getBindingType()).isNull();
            Assertions.assertThat(result.getPhoneNumber()).isEqualTo(userUpdate.getPhone());
            Assertions.assertThat(result.getType()).isEqualTo(userUpdate.getUserType());
            Assertions.assertThat(result.getCompany().getId()).isEqualTo(userUpdate.getCompany().getId());
            Assertions.assertThat(result.getCompany().getName()).isEqualTo(userUpdate.getCompany().getName());
            Assertions.assertThat(result.getAvatarUrl()).isEqualTo(displayAvatar.getAvatarId());

            return status;
        });
    }
}
