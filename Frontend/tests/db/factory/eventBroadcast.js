const BaseFactory = require('tests/db/factory/base');

class EventBroadcastFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            eventId: { id: 42 },
            title: 'Watch this',
            iframeWidth: 480,
            iframeHeight: 320,
            iframeUrl: 'http://some.url.com',
            streamId: 'event-broadcast',
            streamType: 'comdi',
        };
    }

    static get table() {
        return require('db').eventBroadcast;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
        };
    }
}

module.exports = EventBroadcastFactory;
