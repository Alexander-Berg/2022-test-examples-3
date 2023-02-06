const BaseFactory = require('tests/db/factory/base');

class ProgramItemFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            startDate: '2018-02-16T04:00:00.000Z',
            endDate: '2018-02-16T11:30:00.000Z',
            isTalk: true,
            title: 'Эксперимент как инструмент для принятия решений',
            description: 'Виктор расскажет о подходе, который помогает определять.',
            sectionId: null,
            presentations: [],
            videos: [],
        };
    }

    static get table() {
        return require('db').programItem;
    }

    static get subFactories() {
        return {
            eventId: require('tests/db/factory/event'),
            sectionId: require('tests/db/factory/section'),
        };
    }
}

module.exports = ProgramItemFactory;
