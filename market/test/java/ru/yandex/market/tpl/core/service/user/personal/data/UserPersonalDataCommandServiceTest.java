package ru.yandex.market.tpl.core.service.user.personal.data;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
class UserPersonalDataCommandServiceTest extends TplAbstractTest {

    private final UserPersonalDataCommandService commandService;
    private final TestUserHelper helper;
    private final UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = helper.findOrCreateUser(1000L);
    }

    @Test
    void create() {
        //given
        var command = buildCreateCommand();
        //when
        UserPersonalData createdEntity = commandService.createOrUpdate(command);

        //then
        assertsEntity(createdEntity, command);
    }

    @Test
    void update() {
        //given
        var updateCommand = buildUpdateCommand();
        //when
        commandService.createOrUpdate(buildCreateCommand());
        UserPersonalData updatedEntity = commandService.createOrUpdate(updateCommand);

        //then
        assertsEntity(updatedEntity, updateCommand);
    }

    private UserPersonalDataCommand.CreateOrUpdate buildCreateCommand() {
        return getInitBuilder()
                .build();
    }

    private UserPersonalDataCommand.CreateOrUpdate buildUpdateCommand() {
        return getInitBuilder()
                .expiredAt(LocalDate.of(2021, 12, 12))
                .build();
    }

    private UserPersonalDataCommand.CreateOrUpdate.CreateOrUpdateBuilder getInitBuilder() {
        return UserPersonalDataCommand.CreateOrUpdate
                .builder()
                .userId(user.getId())
                .birthdayDate(LocalDate.of(1990, 1, 1))
                .firstVaccinationDate(LocalDate.of(2021, 5, 1))
                .secondVaccinationDate(LocalDate.of(2021, 5, 21))
                .hasVaccination(true)
                .link("link")
                .nationality("nationality")
                .passport("passport");
    }

    private void assertsEntity(UserPersonalData entity, UserPersonalDataCommand.CreateOrUpdate command) {
        assertEquals(command.getUserId(), entity.getUserId());
        assertEquals(command.getBirthdayDate(), entity.getBirthdayDate());
        assertEquals(command.getFirstVaccinationDate(), entity.getFirstVaccinationDate());
        assertEquals(command.getSecondVaccinationDate(), entity.getSecondVaccinationDate());
        assertEquals(command.getHasVaccination(), entity.getHasVaccination());
        assertEquals(command.getLink(), entity.getLink());
        assertEquals(command.getNationality(), entity.getNationality());
        assertEquals(command.getPassport(), entity.getPassport());
        assertEquals(command.getExpiredAt(), entity.getExpiredAt());

        User user = userRepository.findByIdOrThrow(command.getUserId());
        assertEquals(command.getHasVaccination(), user.getHasVaccination());
    }
}
