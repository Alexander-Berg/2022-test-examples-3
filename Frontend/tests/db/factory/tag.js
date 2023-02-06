const BaseFactory = require('tests/db/factory/base');
const { schema } = require('yandex-cfg');

class TagFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            slug: 'javascript',
            name: 'JavaScript',
            description: 'Awesome programming language',
            isPublished: true,
            isVisibleInCatalog: false,
            order: 0,
            category: null,
            categoryId: null,
            type: schema.eventTypeEnum.event,
        };
    }

    static get table() {
        return require('db').tag;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
            categoryId: require('tests/db/factory/tagCategory'),
        };
    }
}

module.exports = TagFactory;
