package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.directapi.model.User;

public class AuthRule extends BaseRule implements NeedsCmdSteps {

    private DirectCmdSteps directCmdSteps;
    private String login;

    public AuthRule() {
        this(Logins.SUPER);
    }

    public AuthRule(String login) {
        this.login = login;
    }

    @Override
    public DirectCmdSteps getDirectCmdSteps() {
        return directCmdSteps;
    }

    @Override
    public AuthRule withDirectCmdSteps(DirectCmdSteps directCmdSteps) {
        this.directCmdSteps = directCmdSteps;
        return this;
    }

    public AuthRule withLogin(String login) {
        this.login = login;
        return this;
    }

    @Override
    protected void start() {
        User user = User.get(login);
        getDirectCmdSteps().authSteps().authenticate(user);
    }
}
