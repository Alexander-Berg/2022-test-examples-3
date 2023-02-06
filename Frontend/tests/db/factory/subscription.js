const BaseFactory = require('tests/db/factory/base');

class SubscriptionFactory extends BaseFactory {
    static get defaultData() {
        const { id } = this;

        return {
            id,
            type: 'news',
            unsubscribeCode: null,
            tagId: { id: 5, slug: 'front-talks', name: 'Front talks' },
            accountId: { id, email: `solo${id}@starwars.ru` },
            isActive: true,
            createdAt: new Date(),
        };
    }

    static get table() {
        return require('db').subscription;
    }

    static get subFactories() {
        return {
            tagId: require('tests/db/factory/tag'),
            accountId: require('tests/db/factory/account'),
        };
    }
}

module.exports = SubscriptionFactory;
