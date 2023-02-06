const _ = require('../../core/lodash');

const config = require('../../core/utils/config');

describe('config', () => {
    let ctx;

    beforeEach(() => {
        ctx = {
            _: {
                prop: _.prop,
            },
        };
    });

    describe('getServiceName', () => {
        it('Возвращает unknown в любой непонятной ситуации (нет данных)', () => {
            expect(config.getServiceName(ctx)).toEqual('unknown');
        });

        it('Возвращает корректное название сервиса для Новостей', () => {
            _.set(ctx, 'data.turbo_source', 'v-news');
            expect(config.getServiceName(ctx)).toEqual('news');

            _.set(ctx, 'data.turbo_source', 'news');
            expect(config.getServiceName(ctx)).toEqual('news');

            _.set(ctx, 'data.turbo_source', undefined);
            _.set(ctx, 'data.doc.pageId', 'news-story');
            expect(config.getServiceName(ctx)).toEqual('news');
        });

        it('Возвращает корректное название сервиса для Спорт', () => {
            _.set(ctx, 'data.turbo_source', 'v-sport');
            expect(config.getServiceName(ctx)).toEqual('sport');

            _.set(ctx, 'data.turbo_source', 'sport');
            expect(config.getServiceName(ctx)).toEqual('sport');
        });

        it('Возвращает корректное название сервиса для Здоровья', () => {
            _.set(ctx, 'data.turbo_source', 'health');
            expect(config.getServiceName(ctx)).toEqual('health');
        });

        it('Возвращает корректное название сервиса для Погоды', () => {
            _.set(ctx, 'data.doc.pageId', 'weather-index');
            expect(config.getServiceName(ctx)).toEqual('weather');

            _.set(ctx, 'data.doc.pageId', 'weather-whatever');
            expect(config.getServiceName(ctx)).toEqual('weather');
        });

        it('Возвращает корректное название сервиса для Лендингов', () => {
            _.set(ctx, 'data.doc.url', 'https://yandex.ru/turbo?text=lpc%2F1933525d65d56efbbb04a5b203b79ffa2ee3f25b5a6e4bc2a666b6472018f41c');
            expect(config.getServiceName(ctx)).toEqual('lpc');

            _.set(ctx, 'data.doc.url', 'https://yandex.ru/turbo?text=lpc/1933525d65d56efbbb04a5b203b79ffa2ee3f25b5a6e4bc2a666b6472018f41c');
            expect(config.getServiceName(ctx)).toEqual('lpc');

            _.set(ctx, 'data.turbo_source', 'lpc');
            _.set(ctx, 'data.doc.url', 'https://yandex.ru/test');
            expect(config.getServiceName(ctx)).toEqual('lpc');
        });

        it('Возвращает корректное название сервиса для Вакансий', () => {
            _.set(ctx, 'data.turbo_source', 'lpc'); // Вакансии работают под сервисом Лендингов
            _.set(ctx, 'data.doc.url', 'https://yandex.ru/jobs');
            expect(config.getServiceName(ctx)).toEqual('jobs');
        });

        it('Возвращает корректное название сервиса для ТВ-Программы', () => {
            _.set(ctx, 'data.doc.url', 'https://tv.yandex.ru/program/3894340?eventId=136765227');
            expect(config.getServiceName(ctx)).toEqual('tv');

            _.set(ctx, 'data.doc.url', 'https://tv.yandex.ua');
            expect(config.getServiceName(ctx)).toEqual('tv');
        });

        it('Возвращает корректное название сервиса для Паблишеров', () => {
            _.set(ctx, 'data.doc.url', 'https://yandex.ru/turbo?text=https%3A%2F%2Falivespace.ru%2Fraketa-na-antiveshhestve%2F');
            expect(config.getServiceName(ctx)).toEqual('publishers');

            _.set(ctx, 'data.doc.url', 'https://lenta.ru/news/2019/04/17/sravnil/');
            expect(config.getServiceName(ctx)).toEqual('publishers');
        });
    });

    describe('getPageType', () => {
        it('Возвращает turbo в любой непонятной ситуации (нет данных)', () => {
            expect(config.getPageType(ctx)).toEqual('turbo');
        });

        it('Возвращает pageId если он есть', () => {
            _.set(ctx, 'data.doc.pageId', 'ololo');
            expect(config.getPageType(ctx)).toEqual('ololo');
        });

        it('Обрезает префиксы для pageId Погоды', () => {
            _.set(ctx, 'data.doc.pageId', 'weather-index');
            expect(config.getPageType(ctx)).toEqual('index');

            _.set(ctx, 'data.doc.pageId', 'weather-search');
            expect(config.getPageType(ctx)).toEqual('search');
        });

        it('Обрезает префиксы для pageId Новостей', () => {
            _.set(ctx, 'data.doc.pageId', 'news-story');
            expect(config.getPageType(ctx)).toEqual('story');
        });

        it('Обрезает префиксы про платформу', () => {
            _.set(ctx, 'data.doc.pageId', 'touch.rubric');
            expect(config.getPageType(ctx)).toEqual('rubric');

            _.set(ctx, 'data.doc.pageId', 'desktop.rubric');
            expect(config.getPageType(ctx)).toEqual('rubric');
        });

        it('Возвращает хостнейм для Паблишеров', () => {
            _.set(ctx, 'data.doc.url', 'https://lenta.ru/news/2019/04/17/sravnil/');
            expect(config.getPageType(ctx)).toEqual('lenta.ru');

            _.set(ctx, 'data.doc.url', 'https://ru.wikipedia.org/wiki/%D0%92%D0%B8%D0%BA%D0%B8');
            expect(config.getPageType(ctx)).toEqual('ru.wikipedia.org');
        });

        it('Возвращает правильный тип страницы для Лендингов', () => {
            _.set(ctx, 'data.doc.url', 'https://yandex.ru/turbo?text=lpc%2F1933525d65d56efbbb04a5b203b79ffa2ee3f25b5a6e4bc2a666b6472018f41c');
            expect(config.getPageType(ctx)).toEqual('turbo');

            _.set(ctx, 'data.doc.page_type', '-invalid_');
            expect(config.getPageType(ctx)).toEqual('turbo');

            _.set(ctx, 'data.doc.page_type', 'неправильное имя');
            expect(config.getPageType(ctx)).toEqual('turbo');

            _.set(ctx, 'data.doc.page_type', '-invalid_');
            expect(config.getPageType(ctx)).toEqual('turbo');

            _.set(ctx, 'data.doc.page_type', 'lpc_page_type');
            expect(config.getPageType(ctx)).toEqual('lpc_page_type');

            _.set(ctx, 'data.doc.page_type', 'u42_43_a.b.c-3a');
            expect(config.getPageType(ctx)).toEqual('u42_43_a.b.c-3a');
        });
    });
});
