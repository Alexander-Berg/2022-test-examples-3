package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;
import ru.yandex.junitextensions.rules.loginrule.Credentials;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.quoted;

public class LoginRequest implements ImapRequestBuilder<GenericResponse> {
    private final String userName;
    private String password;

    private LoginRequest(String userName, String password, Boolean unwrapped) {
        if (unwrapped) {
            this.userName = userName;
            if (!(password.startsWith("\"") && password.endsWith("\""))) {
                this.password = quoted(password);
            } else {
                this.password = password;
            }
        } else {
            this.userName = literal(userName);
            this.password = literal(password);
        }

    }

    public static LoginRequest login(String userName, String password, Boolean unwrapped) {
        return new LoginRequest(userName, password, unwrapped);
    }

    public static LoginRequest login(String userName, String password) {
        return new LoginRequest(userName, password, false);
    }

    public static LoginRequest login(Credentials acc) {
        return login(acc.getLogin(), acc.getPassword(), false);
    }

    public static LoginRequest login(String loginGroup) {
        return login(props().account(loginGroup));
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.LOGIN, userName, password);
    }
}
