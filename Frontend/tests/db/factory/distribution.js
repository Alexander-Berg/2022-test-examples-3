const BaseFactory = require('tests/db/factory/base');

class DistributionFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            eventId: { id: 42 },
            variables: {},
            templateId: { id: 11 },
            filters: { invitationStatus: ['invite'] },
            createdAt: new Date(),
            authorLogin: 'art00',
        };
    }

    static get table() {
        return require('db').distribution;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
            templateId: require('tests/db/factory/mailTemplate'),
        };
    }
}

module.exports = DistributionFactory;
