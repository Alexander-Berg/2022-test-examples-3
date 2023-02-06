import type { IAssetDefinition } from '../../typings';
import { ScriptLoader, StyleLoader } from './assetsLoader';

describe('Загрузчики', () => {
    beforeAll(() => {
        global.fetch = jest.fn();
    });

    afterAll(() => {
        // @ts-ignore
        delete global.fetch;
    });

    describe('Загрузчик скриптов', () => {
        test('Загружает скрипт по url', async() => {
            // @ts-ignore
            fetch.mockImplementationOnce(() => {
                return Promise.resolve({
                    ok: true,
                    text: () => Promise.resolve('console.log("HOBA!")'),
                });
            });

            const assetDefinition: IAssetDefinition = {
                type: 'script',
                name: 'foo',
                url: '/static/foo.js',
                attrs: {
                    foo: 'bar',
                    'data-test': 'test',
                },
            };

            const loader = new ScriptLoader(assetDefinition);

            return loader.createAndLoadElement().then(element => {
                expect(element.tagName).toBe('SCRIPT');
                // @ts-ignore
                expect(element.src).toBe('');
                expect(element.dataset.name).toBe('foo');
                expect(element.dataset.test).toBe('test');
                expect(element.innerHTML).toBe('console.log("HOBA!")');
            });
        });

        test('Работает с inline-скриптом', async() => {
            const assetDefinition: IAssetDefinition = {
                type: 'script',
                name: 'foo',
                content: 'console.log("HOBA!")',
                attrs: {
                    foo: 'bar',
                    'data-test': 'test',
                },
            };

            const loader = new ScriptLoader(assetDefinition);

            return loader.createAndLoadElement().then(element => {
                expect(element.tagName).toBe('SCRIPT');
                expect(element.dataset.test).toBe('test');
                expect(element.dataset.name).toBe('foo');
                expect(element.innerHTML).toBe('console.log("HOBA!")');
            });
        });

        test('Возвращает definition, если запрос неуспешен', () => {
            // @ts-ignore
            fetch.mockImplementationOnce(() => {
                return Promise.resolve({
                    ok: false,
                });
            });

            const assetDefinition: IAssetDefinition = {
                type: 'script',
                name: 'foo',
                url: '/static/foo.js',
                attrs: {
                    foo: 'bar',
                    'data-test': 'test',
                },
            };

            const loader = new ScriptLoader(assetDefinition);

            return loader.createAndLoadElement().catch(definition => {
                expect(definition).toEqual(assetDefinition);
            });
        });
    });

    describe('Загрузчик стилей', () => {
        test('Загружает стиль по url', async() => {
            // @ts-ignore
            fetch.mockImplementationOnce(() => {
                return Promise.resolve({
                    ok: true,
                    text: () => Promise.resolve('html{background-color: red;}'),
                });
            });

            const assetDefinition: IAssetDefinition = {
                type: 'style',
                name: 'foo',
                url: '/static/foo.css',
                attrs: {
                    foo: 'bar',
                    'data-test': 'test',
                },
            };

            const loader = new StyleLoader(assetDefinition);

            return loader.createAndLoadElement().then(element => {
                expect(element.tagName).toBe('STYLE');
                // @ts-ignore
                expect(element.src).toBe(undefined);
                expect(element.dataset.test).toBe('test');
                expect(element.dataset.name).toBe('foo');
                expect(element.innerHTML).toBe('html{background-color: red;}');
            });
        });

        test('Работает с inline-стилем', async() => {
            const assetDefinition: IAssetDefinition = {
                type: 'style',
                name: 'foo',
                content: 'html{background-color: red;}',
                attrs: {
                    foo: 'bar',
                    'data-test': 'test',
                },
            };

            const loader = new StyleLoader(assetDefinition);

            return loader.createAndLoadElement().then(element => {
                expect(element.tagName).toBe('STYLE');
                expect(element.dataset.test).toBe('test');
                expect(element.dataset.name).toBe('foo');
                expect(element.innerHTML).toBe('html{background-color: red;}');
            });
        });

        test('Возвращает definition, если запрос неуспешен', () => {
            // @ts-ignore
            fetch.mockImplementationOnce(() => {
                return Promise.resolve({
                    ok: false,
                });
            });

            const assetDefinition: IAssetDefinition = {
                type: 'style',
                name: 'foo',
                url: '/static/foo.css',
                attrs: {
                    foo: 'bar',
                    'data-test': 'test',
                },
            };

            const loader = new ScriptLoader(assetDefinition);

            return loader.createAndLoadElement().catch(definition => {
                expect(definition).toEqual(assetDefinition);
            });
        });
    });
});
