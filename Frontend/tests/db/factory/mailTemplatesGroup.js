const BaseFactory = require('tests/db/factory/base');

class EventsGroupFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            mailTemplateId: 1,
            groupId: 1,
        };
    }

    static get table() {
        return require('db').mailTemplatesGroup;
    }

    static get subFactories() {
        return {
            groupId: require('tests/db/factory/group'),
            mailTemplateId: require('tests/db/factory/mailTemplate'),
        };
    }
}

module.exports = EventsGroupFactory;
