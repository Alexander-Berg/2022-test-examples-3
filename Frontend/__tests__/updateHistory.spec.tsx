import { extendHistory, pushHistory } from '../updateHistory';

import { restoreDom, clearNamespace } from '../../__tests__/tests-lib';

const initHistory = () => window.history.replaceState({ initialParam: 1 }, '', location.href);

describe('Турбо-оверлей', () => {
    describe('Утилиты', () => {
        describe('Создание истории', () => {
            beforeEach(() => { restoreDom(); initHistory() });
            afterEach(restoreDom);
            afterAll(clearNamespace);

            it('Обновляет текущую историю', () => {
                const length = window.history.length;
                extendHistory({
                    displayUrl: 'https://yandex.ru/turbo',
                    title: 'Турбо-страница',
                    state: { data: 'some-data' },
                });

                expect(window.location.href, 'Урл не изменился')
                    .toEqual('https://yandex.ru/turbo');

                expect(window.history.state, 'state не изменился')
                    .toEqual({ data: 'some-data', initialParam: 1 });

                expect(document.title, 'Тайтл не изменился')
                    .toEqual('Турбо-страница');

                expect(length, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Обновляет текущую историю кроме урла', () => {
                const length = window.history.length;
                extendHistory({
                    title: 'Турбо-страница',
                    state: { data: 'some-data' },
                });

                expect(window.location.href, 'Урл изменился')
                    .toEqual('https://yandex.ru/');

                expect(window.history.state, 'state не изменился')
                    .toEqual({ data: 'some-data', initialParam: 1 });

                expect(document.title, 'Тайтл не изменился')
                    .toEqual('Турбо-страница');

                expect(length, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Обновляет текущую историю кроме тайтла', () => {
                const length = window.history.length;
                extendHistory({
                    displayUrl: 'https://yandex.ru/turbo?text=1',
                    state: { data: 'some-data' },
                });

                expect(window.location.href, 'Урл не изменился')
                    .toEqual('https://yandex.ru/turbo?text=1');

                expect(window.history.state, 'State не изменился')
                    .toEqual({ data: 'some-data', initialParam: 1 });

                expect(document.title, 'Тайтл изменился')
                    .toEqual('Яндекс');

                expect(length, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Обновляет текущую историю кроме состояния', () => {
                const length = window.history.length;
                extendHistory({
                    displayUrl: 'https://yandex.ru/turbo?text=1',
                    title: 'Турбо-страницы',
                    state: null,
                });

                expect(window.location.href, 'Урл не изменился')
                    .toEqual('https://yandex.ru/turbo?text=1');

                expect(window.history.state, 'State изменился')
                    .toEqual({ initialParam: 1 });

                expect(document.title, 'Тайтл не изменился')
                    .toEqual('Турбо-страницы');

                expect(length, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Пушит новую историю', () => {
                const length = window.history.length;
                pushHistory({
                    displayUrl: 'https://yandex.ru/turbo',
                    title: 'Турбо-страница',
                    state: { data: 'some-data' },
                });

                expect(window.location.href, 'Урл не изменился')
                    .toEqual('https://yandex.ru/turbo');

                expect(window.history.state, 'state не изменился')
                    .toEqual({ data: 'some-data' });

                expect(document.title, 'Тайтл не изменился')
                    .toEqual('Турбо-страница');

                expect(length + 1, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Пушит новую историю не изменяя урла', () => {
                const length = window.history.length;
                pushHistory({
                    displayUrl: null,
                    title: 'Турбо-страница',
                    state: { data: 'some-data' },
                });

                expect(window.location.href, 'Урл изменился')
                    .toEqual('https://yandex.ru/');

                expect(window.history.state, 'state не изменился')
                    .toEqual({ data: 'some-data' });

                expect(document.title, 'Тайтл не изменился')
                    .toEqual('Турбо-страница');

                expect(length + 1, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Пушит новую историю не изменяя тайтл', () => {
                const length = window.history.length;
                pushHistory({
                    displayUrl: 'https://yandex.ru/turbo?text=1',
                    title: null,
                    state: { data: 'some-data' },
                });

                expect(window.location.href, 'Урл не изменился')
                    .toEqual('https://yandex.ru/turbo?text=1');

                expect(window.history.state, 'State не изменился')
                    .toEqual({ data: 'some-data' });

                expect(document.title, 'Тайтл изменился')
                    .toEqual('Яндекс');

                expect(length + 1, 'Размер истории отличается')
                    .toBe(window.history.length);
            });

            it('Пушит новую историю не изменяя состояния', () => {
                const length = window.history.length;
                pushHistory({
                    displayUrl: 'https://yandex.ru/turbo?text=1',
                    title: 'Турбо-страницы',
                    state: null,
                });

                expect(window.location.href, 'Урл не изменился')
                    .toEqual('https://yandex.ru/turbo?text=1');

                expect(window.history.state, 'State изменился')
                    .toEqual({});

                expect(document.title, 'Тайтл не изменился')
                    .toEqual('Турбо-страницы');

                expect(length + 1, 'Размер истории отличается')
                    .toBe(window.history.length);
            });
        });
    });
});
