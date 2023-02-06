package ru.yandex.market.checkout.checkouter.yauslugi.model;

import java.util.function.Function;

/**
 * @author zagidullinri
 * @date 21.09.2021
 */
public abstract class ClientDtoProvider {

    public static final String DEFAULT_FIRST_NAME = "someFirstName";
    public static final String DEFAULT_LAST_NAME = "someLastName";
    public static final String DEFAULT_MIDDLE_NAME = "someMiddleName";
    public static final String DEFAULT_PHONE = "+79991234567";
    public static final String DEFAULT_EMAIL = "client@mail.com";

    public static ClientDto defaultClientDto() {
        return builder()
                .configure(ClientDtoProvider::applyDefaults)
                .build();
    }

    public static ClientDtoBuilder applyDefaults(ClientDtoBuilder clientDtoBuilder) {
        return clientDtoBuilder
                .withFirstName(DEFAULT_FIRST_NAME)
                .withLastName(DEFAULT_LAST_NAME)
                .withMiddleName(DEFAULT_MIDDLE_NAME)
                .withPhone(DEFAULT_PHONE)
                .withEmail(DEFAULT_EMAIL);
    }

    public static ClientDtoBuilder builder() {
        return new ClientDtoBuilder();
    }

    public static class ClientDtoBuilder {

        private String firstName;
        private String lastName;
        private String middleName;
        private String phone;
        private String email;

        private ClientDtoBuilder() {

        }

        public ClientDtoBuilder configure(Function<ClientDtoBuilder, ClientDtoBuilder> function) {
            return function.apply(this);
        }

        public ClientDtoBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public ClientDtoBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public ClientDtoBuilder withMiddleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public ClientDtoBuilder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public ClientDtoBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public ClientDto build() {
            ClientDto clientDto = new ClientDto();
            clientDto.setFirstName(firstName);
            clientDto.setLastName(lastName);
            clientDto.setMiddleName(middleName);
            clientDto.setPhone(phone);
            clientDto.setEmail(email);
            return clientDto;
        }
    }
}
