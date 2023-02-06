const config = require('yandex-cfg');
const BaseFactory = require('tests/db/factory/base');

class AccountFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            yandexuid: '1518583433759763986',
            avatar: null,
            login: null,
            sex: config.schema.userSexEnum.man,
            birthDate: '2018-02-01',
            isEmailConfirmed: true,
            email: 'darkside@yandex.ru',
            phone: '999',
            city: 'Татуин',
            website: 'https//ya.ru',
            socialAccount: null,
            blog: 'https//ya.ru',
            firstName: 'Энакин',
            lastName: 'Скайуокер',
            middleName: 'Иванович',
            about: 'Я человек, и моё имя — Энакин!',
            jobPlace: 'Орден джедаев',
            jobPosition: 'Джуниор-падаван',
            studyPlace: null,
            school: null,
            isAgreeToReceiveVacancyInfo: false,
            schoolClass: null,
            studySpecialty: null,
            studyCourse: null,
            createdAt: new Date('2018-02-16T11:30:00'),
        };
    }

    static get table() {
        return require('db').account;
    }
}

module.exports = AccountFactory;
