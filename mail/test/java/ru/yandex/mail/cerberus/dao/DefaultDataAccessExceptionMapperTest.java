package ru.yandex.mail.cerberus.dao;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import ru.yandex.mail.cerberus.ResourceTypeName;
import ru.yandex.mail.cerberus.Uid;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.mail.cerberus.dao.grant.GrantEntity;
import ru.yandex.mail.cerberus.dao.grant.GrantRepository;
import ru.yandex.mail.cerberus.dao.user.UserEntity;
import ru.yandex.mail.cerberus.dao.user.UserRepository;
import ru.yandex.mail.cerberus.exception.ResourceTypeNotFoundException;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.DaoConstants.nextval;
import static ru.yandex.mail.cerberus.dao.DefaultDataAccessExceptionMapperTest.DB_NAME;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class DefaultDataAccessExceptionMapperTest {
    static final String DB_NAME = "exception_mapper_db";

    @Inject
    private UserRepository userRepository;

    @Inject
    private GrantRepository grantRepository;

    @Inject
    private DataAccessExceptionMapper mapper;

    @Test
    @DisplayName("Verify that DataAccessExceptionMapper correctly identify constraint violation")
    void test() {
        val user = new UserEntity(new Uid(123L), UserType.YT, "login", Optional.empty());
        userRepository.insert(user);

        val nonExistentResourceTypename = new ResourceTypeName("some");
        val grant = new GrantEntity(nextval(), Optional.of(user.getUid()), Optional.empty(), Optional.empty(), Optional.empty(),
            nonExistentResourceTypename, Optional.empty(), Set.of("11"));

        assertThatThrownBy(() -> grantRepository.insert(grant))
            .isInstanceOfSatisfying(DataIntegrityViolationException.class, e -> {
                assertThat(mapper.map(e))
                    .containsInstanceOf(ResourceTypeNotFoundException.class);
            });
    }
}
