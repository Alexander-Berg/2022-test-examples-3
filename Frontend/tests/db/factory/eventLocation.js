const config = require('yandex-cfg');
const BaseFactory = require('tests/db/factory/base');

class EventLocationFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            eventId: { id: 42 },
            description: 'Come here',
            place: 'Evercity, Sunlane 12',
            city: 'Evercity',
            timezone: config.defaultTimezone,
            lat: 57.234,
            lon: 34.452,
            zoom: 13,
        };
    }

    static get table() {
        return require('db').eventLocation;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
        };
    }
}

module.exports = EventLocationFactory;
