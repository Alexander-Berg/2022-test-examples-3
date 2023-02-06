const BaseFactory = require('tests/db/factory/base');

class EventsGroupFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            eventId: 1,
            groupId: 1,
        };
    }

    static get table() {
        return require('db').eventsGroup;
    }

    static get subFactories() {
        return {
            groupId: require('tests/db/factory/group'),
            eventId: require('tests/db/factory/event'),
        };
    }
}

module.exports = EventsGroupFactory;
