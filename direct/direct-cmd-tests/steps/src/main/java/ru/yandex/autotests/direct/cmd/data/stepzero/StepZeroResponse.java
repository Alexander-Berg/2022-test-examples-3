package ru.yandex.autotests.direct.cmd.data.stepzero;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StepZeroResponse extends ErrorResponse {


    @SerializedName("clients")
    private List<Client> clients;


    public List<Client> getClients() {
        return clients;
    }

    public List<String> getLogins() {
        return clients.stream()
                .map(Client::getLogin)
                .collect(Collectors.toList());
    }

    public StepZeroResponse withClients(List<Client> clients) {
        this.clients = clients;
        return this;
    }

    public StepZeroResponse withLogins(String... logins) {
        return withClients(Stream.of(logins)
                .map(t -> new Client().withLogin(t))
                .collect(Collectors.toList()));
    }

    public class Client {

        @SerializedName("login")
        private String login;

        public String getLogin() {
            return login;
        }

        public Client withLogin(String login) {
            this.login = login;
            return this;
        }
    }
}
