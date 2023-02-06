const BaseFactory = require('tests/db/factory/base');

class PresentationFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            downloadUrl: 'https://yadi.sk/i/AEZ9I74I3UmjcW',
        };
    }

    static get table() {
        return require('db').presentation;
    }

    static get subFactories() {
        return {
            programItemId: require('tests/db/factory/programItem'),
        };
    }
}

module.exports = PresentationFactory;
