import type { IAssetDefinition } from '../../typings';
import { AssetsInserter } from './assetsInserter';
import type { ILoaderFactory } from './assetsInserter';
import { Loader } from './assetsLoader';
import { RequestAnimationFrameMock } from '../../test/requestAnimationFrameMock';

const rafMock = new RequestAnimationFrameMock();
rafMock.useFakeRaf();
rafMock.triggerAllFrames();

function makeDelay(timeout: number) {
    return new Promise<void>(resolve => {
        setTimeout(() => {
            resolve();
        }, timeout);
    });
}

class MockedLoader extends Loader {
    public createAndLoadElement = jest.fn(() => {
        const mockedLoading = this.definition.attrs?.timeout ?
            makeDelay(this.definition.attrs?.timeout as number) : Promise.resolve();
        return mockedLoading.then(() => {
            const div = document.createElement('div');
            div.setAttribute('name', this.definition.name || '');

            return div;
        });
    })
}

describe('Ручное управление ассетами', () => {
    const loaderFactory: ILoaderFactory = definition => {
        return new MockedLoader(definition);
    };

    test('Синхронные ассеты вставляются в строгом порядке', async() => {
        jest.useFakeTimers();
        // timeout в аттрибутах используется в целях тестирования, в продакшене нет подобной логики
        const assets: IAssetDefinition[] = [
            {
                type: 'script',
                url: '/static/long.js',
                name: 'long-asset',
                attrs: {
                    timeout: 1000,
                },
            },
            {
                type: 'script',
                url: '/static/fast.js',
                name: 'fast-asset',
                attrs: {
                    timeout: 5,
                },
            },
            {
                type: 'script',
                name: 'inline-asset',
                content: 'console.log("HOBA")',
            },
            {
                type: 'style',
                url: '/static/style.css',
                name: 'style-asset',
                attrs: {
                    timeout: 2000,
                },
            },
            {
                type: 'script',
                url: '/static/foo.js',
                name: 'foo-asset',
                attrs: {
                    timeout: 2000,
                },
            },
        ];

        const assetsInserter = new AssetsInserter(assets, loaderFactory);
        const container = document.createElement('div');

        const promise = assetsInserter.insert(container);
        jest.runAllTimers();
        jest.useRealTimers();

        return promise
            .then(failedAssets => {
                expect(failedAssets).toHaveLength(0);
                expect(container.children[0].getAttribute('name')).toBe('long-asset');
                expect(container.children[1].getAttribute('name')).toBe('fast-asset');
                expect(container.children[2].getAttribute('name')).toBe('inline-asset');
                expect(container.children[3].getAttribute('name')).toBe('style-asset');
                expect(container.children[4].getAttribute('name')).toBe('foo-asset');
                expect(container.childElementCount).toBe(5);
            });
    });

    test('Асинхронные ассеты вставляются сразу после загрузки', async() => {
        jest.useFakeTimers();
        // timeout в аттрибутах используется в целях тестирования, в продакшене нет подобной логики
        const assets: IAssetDefinition[] = [
            {
                type: 'script',
                url: '/static/long.js',
                name: 'long-asset',
                attrs: {
                    async: true,
                    timeout: 1000,
                },
            },
            {
                type: 'script',
                url: '/static/fast.js',
                name: 'fast-asset',
                attrs: {
                    async: true,
                    timeout: 5,
                },
            },
            // Такое использование не предполагается, просто проверка на всякий случай
            {
                type: 'script',
                name: 'inline-asset',
                content: 'console.log("HOBA")',
                attrs: {
                    async: true,
                },
            },
            // Такое использование не предполагается, просто проверка на всякий случай
            {
                type: 'style',
                name: 'style-asset',
                url: '/static/style.css',
                attrs: {
                    timeout: 500,
                    async: true,
                },
            },
            {
                type: 'script',
                url: '/static/foo.js',
                name: 'foo-asset',
                attrs: {
                    async: true,
                    timeout: 2000,
                },
            },
        ];

        const assetsInserter = new AssetsInserter(assets, loaderFactory);
        const container = document.createElement('div');

        const promise = assetsInserter.insert(container);
        jest.runAllTimers();
        jest.useRealTimers();

        return promise.then(failedAssets => {
            expect(failedAssets).toHaveLength(0);
            expect(container.children[0].getAttribute('name')).toBe('inline-asset');
            expect(container.children[1].getAttribute('name')).toBe('fast-asset');
            expect(container.children[2].getAttribute('name')).toBe('style-asset');
            expect(container.children[3].getAttribute('name')).toBe('long-asset');
            expect(container.children[4].getAttribute('name')).toBe('foo-asset');
            expect(container.childElementCount).toBe(5);
        });
    });

    test('Синхронные ассеты сохраняют очерёдность вперемешку с асинхронными', async() => {
        jest.useFakeTimers();
        // timeout в аттрибутах используется в целях тестирования, в продакшене нет подобной логики
        const assets: IAssetDefinition[] = [
            {
                type: 'script',
                url: '/static/long.js',
                name: 'long-asset',
                attrs: {
                    timeout: 1000,
                },
            },
            {
                type: 'script',
                url: '/static/fast.js',
                name: 'fast-asset',
                attrs: {
                    async: true,
                    timeout: 5,
                },
            },
            {
                type: 'script',
                name: 'inline-asset',
                content: 'console.log("HOBA")',
            },
            {
                type: 'style',
                url: '/static/style.css',
                name: 'style-asset',
                attrs: {
                    timeout: 2000,
                },
            },
            {
                type: 'script',
                url: '/static/foo.js',
                name: 'foo-asset',
                attrs: {
                    timeout: 2000,
                },
            },
            {
                type: 'script',
                url: '/static/bar.js',
                name: 'bar-asset',
                attrs: {
                    async: true,
                    timeout: 500,
                },
            },
        ];

        const assetsInserter = new AssetsInserter(assets, loaderFactory);
        const container = document.createElement('div');

        const promise = assetsInserter.insert(container);
        jest.runAllTimers();
        jest.useRealTimers();

        return promise
            .then(failedAssets => {
                expect(failedAssets).toHaveLength(0);
                expect(container.children[0].getAttribute('name')).toBe('fast-asset');
                expect(container.children[1].getAttribute('name')).toBe('bar-asset');
                expect(container.children[2].getAttribute('name')).toBe('long-asset');
                expect(container.children[3].getAttribute('name')).toBe('inline-asset');
                expect(container.children[4].getAttribute('name')).toBe('style-asset');
                expect(container.children[5].getAttribute('name')).toBe('foo-asset');
                expect(container.childElementCount).toBe(6);
            });
    });
});
