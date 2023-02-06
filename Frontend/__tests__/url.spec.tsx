import { getCacheKey, getHost, isSameHost, updateTurboUrls, getSearch } from '../url';
import { memoize } from '../url/memoize';

import { restoreDom, clearNamespace } from '../../__tests__/tests-lib';
import { getPathname } from '../url/getCacheKey';

describe('Турбо-оверлей', () => {
    describe('Утилиты', () => {
        describe('Работа с урлом', () => {
            beforeEach(restoreDom);
            afterEach(restoreDom);
            afterAll(clearNamespace);

            it('Корректно получает хост из урла', () => {
                const expected = 'yandex.ru';
                expect(getHost('https://yandex.ru'))
                    .toEqual(expected);

                expect(getHost('http://yandex.ru'))
                    .toEqual(expected);

                expect(getHost('https://www.yandex.ru'))
                    .toEqual(expected);

                expect(getHost('http://www.yandex.ru'))
                    .toEqual(expected);

                expect(getHost('https://yandex.ru/turbo'))
                    .toEqual(expected);

                expect(getHost('http://yandex.ru/turbo'))
                    .toEqual(expected);

                expect(getHost('https://www.yandex.ru/turbo'))
                    .toEqual(expected);

                expect(getHost('http://www.yandex.ru/turbo'))
                    .toEqual(expected);

                expect(getHost('https://www.yandex.ru/turbo/some-path?and=query'))
                    .toEqual(expected);

                expect(getHost('http://www.yandex.ru/turbo/some-path?and-query'))
                    .toEqual(expected);
            });

            it('Корректно получает ключ для кэширования', () => {
                const trashQuery = '&some-query&parent-reqid=1&new_overlay=1&fallback=1&trbsrc=wb&check_swipe=1&no_bolver=123';
                expect(getCacheKey(`https://yandex.ru/turbo?text=about${trashQuery}`)).toEqual('turbo?some-query=1&text=about');

                expect(getCacheKey(`https://yandex.ru/turbo/s/rg.ru/2020/04/20/important-news.html?new_overlay=1&check_swipe=1${trashQuery}`))
                    .toEqual('turbo/s/rg.ru/2020/04/20/important-news.html?some-query=1');

                expect(getCacheKey(`//yandex.ru/turbo/n/host.ru/important-piece-of-news.html?new_overlay=1${trashQuery}`))
                    .toEqual('turbo/n/host.ru/important-piece-of-news.html?some-query=1');

                expect(getCacheKey(`https://yandex.ru/turbo?stub=test_news${trashQuery}`))
                    .toEqual('turbo?some-query=1&stub=test_news');

                expect(getCacheKey(`https://yandex.ru/turbo?text=about&page=1${trashQuery}`))
                    .toEqual('turbo?page=1&some-query=1&text=about');

                expect(getCacheKey(`https://yandex.ru/turbo?stub=test_news&page=1${trashQuery}`))
                    .toEqual('turbo?page=1&some-query=1&stub=test_news');

                expect(getCacheKey(`https://yandex.ru/pogoda?stub=test_news&page=1${trashQuery}`)).toEqual('pogoda');

                expect(getCacheKey('https://yandex.ru/pogoda/moscow/details/day-3?lat=55.734188&lon=37.587017&via=dnav'))
                    .toEqual('pogoda/moscow/details/day-3');

                expect(getCacheKey('https://yandex.ru/turbo')).toEqual('turbo?');

                expect(getCacheKey(`/turbo?text=about${trashQuery}`)).toEqual('turbo?some-query=1&text=about');

                expect(getCacheKey(`/turbo/s/rg.ru/2020/04/20/important-news.html?new_overlay=1&check_swipe=1${trashQuery}`))
                    .toEqual('turbo/s/rg.ru/2020/04/20/important-news.html?some-query=1');

                expect(getCacheKey(`/turbo?stub=test_news${trashQuery}`)).toEqual('turbo?some-query=1&stub=test_news');

                expect(getCacheKey(`/turbo?text=about&page=1${trashQuery}`)).toEqual('turbo?page=1&some-query=1&text=about');

                expect(getCacheKey(`/turbo?stub=test_news&page=1${trashQuery}`))
                    .toEqual('turbo?page=1&some-query=1&stub=test_news');

                expect(getCacheKey(`//yandex.ru/redir/GAkkM7lQwz?stub=test_news&page=1${trashQuery}`))
                    .toEqual('redir/GAkkM7lQwz?page=1&some-query=1&stub=test_news');

                expect(getCacheKey(`/pogoda?stub=test_news&page=1${trashQuery}`)).toEqual('pogoda');

                expect(getCacheKey('/pogoda/moscow/details/day-3?lat=55.734188&lon=37.587017&via=dnav'))
                    .toEqual('pogoda/moscow/details/day-3');

                expect(getCacheKey('/turbo')).toEqual('turbo?');
            });
        });

        it('Корректно матчит хосты', () => {
            expect(isSameHost('yandex.ru'), 'Ориджин не сматчился с локации')
                .toBe(true);
            expect(isSameHost('hamster.yandex.ru'), 'Ориджин сматчился некорректно')
                .toBe(false);
        });

        it('Корректно перемалывает изначальные урлы', () => {
            const initialUrls = [
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://yandex.ru',
                    originalUrl: 'https://yandex.ru',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://yandex.ru',
                },
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://kp.ru/a/b/c',
                    originalUrl: 'https://kp.ru/a/b/c',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://kp.ru/a/b/c',
                },
            ];

            const expectedUrlsWithSameHost = [
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://yandex.ru',
                    originalUrl: 'https://yandex.ru',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://yandex.ru' +
                        '&test_param_name=test_param_value',
                },
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://kp.ru/a/b/c',
                    originalUrl: 'https://kp.ru/a/b/c',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://kp.ru/a/b/c' +
                        '&test_param_name=test_param_value',
                },
            ];

            const turboUrls = updateTurboUrls({
                turboUrl: null,
                urls: initialUrls,
                newCgiParams: [{
                    name: 'test_param_name',
                    value: 'test_param_value',
                }],
            });
            expect(turboUrls, 'Изменяются урлы, когда нет турбо-хоста').toEqual(expectedUrlsWithSameHost);

            const loginHost = 'login-1-ws3.ci-tunneler.yandex.ru';
            const expectedUrlsWithNewHost = [
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://yandex.ru',
                    originalUrl: 'https://yandex.ru',
                    frameUrl: `https://${loginHost}/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://yandex.ru` +
                        '&test_param_name=test_param_value',
                },
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://kp.ru/a/b/c',
                    originalUrl: 'https://kp.ru/a/b/c',
                    frameUrl: `https://${loginHost}/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://kp.ru/a/b/c` +
                        '&test_param_name=test_param_value',
                },
            ];

            const turboUrlsWithTurboHost = updateTurboUrls({
                turboUrl: loginHost,
                urls: initialUrls,
                newCgiParams: [{
                    name: 'test_param_name',
                    value: 'test_param_value',
                }],
            });
            expect(turboUrlsWithTurboHost, 'Не изменяются урлы, когда есть турбо-хост')
                .toEqual(expectedUrlsWithNewHost);

            expect(expectedUrlsWithNewHost, 'Урлы мутировались').not.toEqual(initialUrls);
        });

        it('Правильно обновляет урлы, содержащие хэш', () => {
            const initial = [
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://yandex.ru',
                    originalUrl: 'https://yandex.ru',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://yandex.ru#1234',
                },
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://kp.ru/a/b/c',
                    originalUrl: 'https://kp.ru/a/b/c',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://kp.ru/a/b/c#1234',
                },
            ];

            const expected = [
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://yandex.ru',
                    originalUrl: 'https://yandex.ru',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://yandex.ru' +
                        '&test_param_name=test_param_value#1234',
                },
                {
                    displayUrl: 'https://yandex.ru/turbo?text=https://kp.ru/a/b/c',
                    originalUrl: 'https://kp.ru/a/b/c',
                    frameUrl: 'https://yandex.ru/turbo?some-query&parent-reqid=1&no_bolver=123&text=https://kp.ru/a/b/c' +
                        '&test_param_name=test_param_value#1234',
                },
            ];

            const turboUrls = updateTurboUrls({
                turboUrl: null,
                urls: initial,
                newCgiParams: [{
                    name: 'test_param_name',
                    value: 'test_param_value',
                }],
            });
            expect(turboUrls, 'Изменяются урлы, когда нет турбо-хоста').toEqual(expected);
        });

        it('getQuery', () => {
            expect(getSearch('https://yandex.ru')).toEqual({});
            expect(getSearch('https://yandex.ru?text=about')).toEqual({ text: 'about' });
            expect(getSearch('https://yandex.ru?hello-world')).toEqual({ 'hello-world': '1' });
            expect(getSearch('https://yandex.ru?abc=3')).toEqual({ abc: '3' });
        });

        it('getPathname', () => {
            expect(getPathname('https://yandex.ru')).toEqual('');
            expect(getPathname('https://yandex.ru/turbo')).toEqual('turbo');
            expect(getPathname('https://yandex.ru/turbo?text=123')).toEqual('turbo');
            expect(getPathname('https://yandex.ru/turbo/turbo?text=123')).toEqual('turbo/turbo');
            expect(getPathname('//yandex.ru/turbo/turbo?text=123')).toEqual('turbo/turbo');
            expect(getPathname('/turbo/turbo?text=123')).toEqual('turbo/turbo');
        });

        describe('Мемоизация дорогих операций с урлом', () => {
            it('Мемоизирует операции, когда они не выходят за пределы лимита', () => {
                const testFunction = jest.fn(x => `${x}_`);
                const memoizedFn = memoize(testFunction);

                for (let i = 0; i < 20; i++) {
                    const value = memoizedFn(String(i % 10));
                    expect(value, 'Мемоизированная функция возвращает некорректный результат')
                        .toEqual(`${i % 10}_`);
                }

                const calls = Array.from({ length: 10 }, (_, i) => [String(i)]);
                expect(testFunction.mock.calls.length).toBe(10);
                expect(testFunction.mock.calls).toEqual(calls);
            });

            it('Вытесняет первые элементы из кэша', () => {
                const testFunction = jest.fn(x => `${x}_`);
                const memoizedFn = memoize(testFunction, 10);

                expect(memoizedFn('100')).toBe('100_');
                expect(memoizedFn('101')).toBe('101_');

                for (let i = 0; i < 20; i++) {
                    const value = memoizedFn(String(i % 10));
                    expect(value, 'Мемоизированная функция возвращает некорректный результат')
                        .toEqual(`${i % 10}_`);
                }

                expect(memoizedFn('100')).toBe('100_');
                expect(memoizedFn('101')).toBe('101_');

                const calls = [
                    ['100'],
                    ['101'],
                    ...Array.from({ length: 10 }, (_, i) => [String(i)]),
                    ['100'],
                    ['101'],
                ];

                expect(testFunction.mock.calls.length).toBe(14);
                expect(testFunction.mock.calls).toEqual(calls);
            });
        });
    });
});
