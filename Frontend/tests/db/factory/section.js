const config = require('yandex-cfg');
const BaseFactory = require('tests/db/factory/base');

class SectionFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            title: 'Фронтэнд',
            order: 1,
            slug: 'front',
            isPublished: true,
            type: config.schema.sectionTypeEnum.section,
        };
    }

    static get table() {
        return require('db').section;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
        };
    }
}

module.exports = SectionFactory;
