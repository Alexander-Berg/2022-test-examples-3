const config = require('yandex-cfg');

const BaseFactory = require('tests/db/factory/base');
const { operationTypes, entityTypes } = config.history;

class EventHistoryFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            authorLogin: 'saaaaaaaaasha',
            operation: operationTypes.update,
            entityType: entityTypes.event,
            data: { registrationStatus: 'opened' },
        };
    }

    static get table() {
        return require('db').history;
    }

    static get subFactories() {
        return {
            entityId: require('tests/db/factory/event'),
        };
    }
}

module.exports = EventHistoryFactory;
