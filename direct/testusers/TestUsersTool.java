package ru.yandex.direct.internaltools.tools.testusers;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.testuser.service.TestUsersService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.internaltools.core.annotations.tool.AccessGroup;
import ru.yandex.direct.internaltools.core.annotations.tool.Category;
import ru.yandex.direct.internaltools.core.annotations.tool.Tool;
import ru.yandex.direct.internaltools.core.enums.InternalToolAccessRole;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.enums.InternalToolType;
import ru.yandex.direct.internaltools.core.exception.InternalToolProcessingException;
import ru.yandex.direct.internaltools.core.implementations.MassInternalTool;
import ru.yandex.direct.internaltools.tools.testusers.container.TestUserInfo;
import ru.yandex.direct.internaltools.tools.testusers.model.TestUsersParameters;
import ru.yandex.direct.staff.client.StaffClient;
import ru.yandex.direct.staff.client.model.json.PersonInfo;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.builder.When;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.constraint.CommonConstraints.notNull;
import static ru.yandex.direct.validation.constraint.StringConstraints.notBlank;

@Tool(
        name = "Тестовые суперпользователи",
        label = "testusers",
        description = "Пользователей можно добавлять и блокировать с помощью формы",
        consumes = TestUsersParameters.class,
        type = InternalToolType.WRITER
)
@Category(InternalToolCategory.TESTING)
@AccessGroup({InternalToolAccessRole.SUPER, InternalToolAccessRole.SUPERREADER})
@ParametersAreNonnullByDefault
public class TestUsersTool extends MassInternalTool<TestUsersParameters, TestUserInfo> {

    private StaffClient staffClient;
    private UserService userService;
    private TestUsersService testUsersService;

    @Autowired
    public TestUsersTool(StaffClient staffClient, UserService userService, TestUsersService testUsersService) {
        this.staffClient = staffClient;
        this.userService = userService;
        this.testUsersService = testUsersService;
    }

    @Override
    public ValidationResult<TestUsersParameters, Defect> validate(TestUsersParameters testUsersParameters) {
        ItemValidationBuilder<TestUsersParameters, Defect> validationBuilder =
                ItemValidationBuilder.of(testUsersParameters);

        validationBuilder.item(testUsersParameters.getDomainLogin(), TestUsersParameters.DOMAIN_LOGIN)
                .check(notNull(), When.isFalse(testUsersParameters.isRetractRole()))
                .check(notBlank(), When.isFalse(testUsersParameters.isRetractRole()));

        return validationBuilder.getResult();
    }

    @Nullable
    @Override
    protected List<TestUserInfo> getMassData() {
        return mapList(testUsersService.getAll(), user -> new TestUserInfo()
                .withLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin())
                .withRole(user.getRole()));
    }

    @Override
    protected List<TestUserInfo> getMassData(TestUsersParameters parameter) {
        Long uid = userService.getUidByLogin(parameter.getLogin());

        if (uid == null) {
            throw new InternalToolProcessingException(String.format("Пользователя %s не существует",
                    parameter.getLogin()));
        }

        if (parameter.isRetractRole()) {
            if (!testUsersService.remove(uid)) {
                throw new InternalToolProcessingException(String.format("У пользователя %s прав уже нет",
                        parameter.getLogin()));
            }
        } else {
            Map<String, PersonInfo> result = staffClient.getStaffUserInfos(List.of(parameter.getDomainLogin()));
            if (!result.containsKey(parameter.getDomainLogin())) {
                throw new InternalToolProcessingException(String.format("Пользователь %s не найден на Стаффе",
                        parameter.getDomainLogin()));
            }

            testUsersService.setRole(uid, parameter.getDomainLogin(), parameter.getRole());
        }

        return getMassData();
    }
}
