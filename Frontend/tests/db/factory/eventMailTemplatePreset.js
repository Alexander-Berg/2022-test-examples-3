const BaseFactory = require('tests/db/factory/base');

class EventMailTemplateVariablePresetFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            eventId: { id: 42 },
            mailTemplateId: { id: 142 },
            variables: { eventName: 'Fronttalks 2019' },
        };
    }

    static get table() {
        return require('db').eventMailTemplatePreset;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
            mailTemplateId: require('tests/db/factory/mailTemplate'),
        };
    }
}

module.exports = EventMailTemplateVariablePresetFactory;
