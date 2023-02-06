const BaseFactory = require('tests/db/factory/base');

class RegistrationFactory extends BaseFactory {
    static get defaultData() {
        const { id } = this;

        return {
            id,
            formAnswerId: 535297,
            source: 'forms',
            invitationStatus: 'not_decided',
            visitStatus: 'not_come',
            answers: {
                email: {
                    label: 'Email',
                    value: 'saaaaaaaaasha@yandex-team.ru',
                },
                // eslint-disable-next-line camelcase
                choices_72343: {
                    label: 'Ваша специальность',
                    value: 'javascript, go',
                },
            },
            createdAt: new Date(),
            confirmationEmailCode: null,
            eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
            accountId: { id, email: `solo${id}@starwars.ru` },
            isEmailConfirmed: false,
            isRegisteredBefore: false,
            yandexuid: null,
            comment: null,
        };
    }

    static get table() {
        return require('db').registration;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
            accountId: require('tests/db/factory/account'),
        };
    }
}

module.exports = RegistrationFactory;
