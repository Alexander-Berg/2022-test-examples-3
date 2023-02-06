const BaseFactory = require('tests/db/factory/base');

class SpeakerFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            avatar: null,
            email: `darkside${this.id}@yandex.ru`,
            phone: '999',
            city: 'Татуин',
            socialAccount: null,
            firstName: 'Энакин',
            lastName: 'Скайуокер',
            middleName: 'Иванович',
            about: 'Я человек, и моё имя — Энакин!',
            jobPlace: 'Орден джедаев',
            jobPosition: 'Джуниор-падаван',
            createdAt: new Date(),
        };
    }

    static get table() {
        return require('db').speaker;
    }
}

module.exports = SpeakerFactory;
