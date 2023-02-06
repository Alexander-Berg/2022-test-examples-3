const _ = require('lodash');

const Adapter = require('../../core/adapter');

const emptyDocumentFromConstruct = {
    construct: [{}],
    subtitle: undefined,
    title: undefined,
    url: undefined,
    theme: {
        preset: 'default',
        cover: {
            divider: {
                color: 'rgba(0, 0, 0, 0.1)',
            },
        },
    },
    data: {},
    content: [{
        content_type: 'cover',
        content: [
            {
                content_type: 'header',
                type: 'host',
                url: undefined,
                content: {
                    content: undefined,
                    content_type: 'header-title',
                },
            },
            {
                content_type: 'divider',
            },
            {
                content_type: 'title',
                content: '',
            },
            false,
            '',
            '',
        ],
    }, ''],
    isInfinityFeed: false,
    ograph: {
        type: 'article',
        description: undefined,
        image: undefined,
        site_name: '',
        title: undefined,
        url: undefined,
    },
    publisher: {},
};

const emptyDocumentFromContent = {
    content: [],
    theme: {
        preset: 'default',
        cover: {
            divider: {
                color: 'rgba(0, 0, 0, 0.1)',
            },
        },
    },
    isInfinityFeed: false,
    ograph: {
        type: 'article',
        description: undefined,
        image: undefined,
        site_name: '',
        title: undefined,
        url: undefined,
    },
    publisher: {},
};

emptyDocumentFromContent.data = emptyDocumentFromContent;

describe('isValidDocument', () => {
    describe('Не валидный документ', () => {
        it('Без doc', function() {
            expect(Adapter.isValidDocument()).toBe(false);
            expect(Adapter.isValidDocument('')).toBe(false);
        });

        it('Пустой construct', function() {
            expect(Adapter.isValidDocument({ construct: [] })).toBe(false);
        });

        it('Ошибочный content', function() {
            expect(Adapter.isValidDocument({ content: {} })).toBe(false);
        });
    });

    describe('Валидный документ', () => {
        it('C construct', function() {
            expect(Adapter.isValidDocument({ construct: [{}] })).toBe(true);
        });

        it('С content', function() {
            expect(Adapter.isValidDocument({ content: [] })).toBe(true);
        });
    });
});

describe('extractDocument', () => {
    function makeIt(msg, doc, refDoc) {
        const data = doc !== undefined ? _.set({}, 'app_host.result.docs.0', doc) : doc;

        return it(msg, function() {
            expect(Adapter.extractDocument(data), 'Not extracted').toEqual(refDoc);
        });
    }

    makeIt('Без аргументов', undefined);
    makeIt('Без construct', {});
    makeIt('С ошибочным construct', { construct: {} });
    makeIt('С пустым construct', { construct: [] });
    makeIt('С непустым construct', { construct: [{}] }, emptyDocumentFromConstruct);
    makeIt('С ошибочным content', { content: true });
    makeIt('С content', { content: [] }, emptyDocumentFromContent);
});

describe('stubDocument', () => {
    it('С ошибочным stub', function() {
        const data = {};
        _.set(data, 'cgidata.args.stub', ['my_fake_stub']);
        _.set(data, 'reqdata.device', 'touch');
        _.set(data, 'reqdata.is_yandex_net', true);
        const t = Adapter.stubDocument(data);

        expect(t).toBeUndefined();
    });

    it('Пример footer', function() {
        const data = {};
        _.set(data, 'cgidata.args.stub', ['footer/default.json']);
        _.set(data, 'reqdata.device', 'touch');
        _.set(data, 'reqdata.is_yandex_net', true);
        const t = Adapter.stubDocument(data);

        const fields = ['title', 'content'];

        fields.forEach(field => {
            expect(t, `Нет поля ${field}`).toHaveProperty(field);
        });
    });

    // TODO: раскомментить когда закроем доступ для всех кроме асессоров
    // it('Из внешней сети', function() {
    //     const data = {};
    //     _.set(data, 'cgidata.args.stub', ['footer/default']);
    //     _.set(data, 'reqdata.device', 'touch');
    //     _.set(data, 'reqdata.is_yandex_net', false);
    //     assert.deepEqual(Adapter.stubDocument(data), undefined);
    // });
});
