const BaseFactory = require('tests/db/factory/base');

class AccountMailFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            accountId: { id: 42 },
            distributionId: { id: 11 },
            sentAt: new Date(),
            title: 'Вы приглашены на галактическое мероприятие',
            variables: { test: 'test' },
        };
    }

    static get table() {
        return require('db').accountMail;
    }

    static get subFactories() {
        return {
            accountId: require('tests/db/factory/account'),
            distributionId: require('tests/db/factory/distribution'),
        };
    }
}

module.exports = AccountMailFactory;
