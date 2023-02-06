const BaseFactory = require('tests/db/factory/base');

class TagCategoryFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            name: 'Виды мероприятий',
            order: 0,
        };
    }

    static get table() {
        return require('db').tagCategory;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
        };
    }
}

module.exports = TagCategoryFactory;
