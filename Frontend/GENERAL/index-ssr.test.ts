import path from 'path';
import { buildClientSchema, GraphQLSchema } from 'graphql';
import { addMocksToSchema, createMockStore } from '@graphql-tools/mock';
import { IMockStore } from '@graphql-tools/mock/types';

import * as introspectionResult from '../graphql.schema.json';
import { tests as commonTests, getTestsConfig } from './__mocks__/app.common';
import { loadHtml, loadBundle, render, ssr } from './__mocks__/ssr-core';

import { ApolloClient, SchemaLink, onError, getCache, from } from './index-ssr';
import { ExpFlags } from './types';

const tests: typeof commonTests = [...commonTests].filter(test => test.screen !== 'maps');
const requiredMinimalAbtFlags = [
    { flags: {
        // необходимо скрыть Skeleton рекламы
        is_autotest_adv: true,
    } as Required<ExpFlags> }
] as typeof commonTests[number]['abt'];

tests.push({
    ...commonTests[0],
    name: 'should render with htmlClass=font_loaded lang=$lang',
    toPresent: [
        ...commonTests[0].toPresent,
        /<html([^<]*)class="font_loaded"/, // должен быть класс на html
        /<style>([^<]*)fonts\/ys\/\d+\/text-regular\.woff2/, // должен инлайниться стиль с шрифтом
        /<script defer data-chunk="main" src="([^"]+)"><\/script>/ // должны быть defer-подключены скрипты страницы
    ],
    abt: requiredMinimalAbtFlags,
    cookies: { [ssr.fonts.cookieName]: ssr.fonts.fontVersion }
},
{
    ...commonTests[0],
    name: 'should render with font preload',
    toPresent: [
        /<link rel="preload" href="([^"]+)\.woff2" as="font"/, // прелоад шрифтов
        new RegExp(`document\\.documentElement\\.className\\+=" ${ssr.fonts.className}"`) // загрузчик шрифтов
    ],
    abt: requiredMinimalAbtFlags,
    langs: ['ru']
});

let lastPresentError: string;

declare global {
    namespace jest {
        interface Matchers<R> {
            toPresent(find: string | RegExp): R
        }
    }
}

expect.extend({
    toPresent(input, search) {
        const matches = input.match(search);

        if (!matches || !matches.length) {
            lastPresentError = input;

            return {
                message: () => `expected input isn't contain any '${search}'`,
                pass: false
            };
        }

        return {
            message: () => `expected input contain ${matches.length} '${search}'`,
            pass: true
        };
    },
});

describe('ssr', () => {
    let schema: GraphQLSchema;
    let mockStore: IMockStore;
    let mockSchema: GraphQLSchema;

    beforeAll(async() => {
        await loadBundle({ basePath: path.join(__dirname, '..', 'build'), ssr })
            .then(ssr => Promise.all([ssr.bundle.i18nPreload(), loadHtml(ssr)]));

        // @ts-ignore
        schema = buildClientSchema(introspectionResult);

        const mocks = {
            // пришлось переопределить, чтобы moon не моргал, в будущем надо нормальные моки сделать
            Int: () => Math.floor(Math.random() * 16),
            Url: () => 'https://mock.url/foo.bar',
            Timestamp: () => (Math.round((new Date('2022-05-11T05:13:20.000Z')).getTime() / 1000)).toString(10),
            Time: () => '2022-06-21T00:00:00+03:00'
        };

        mockStore = createMockStore({
            schema,
            mocks
        });

        mockSchema = addMocksToSchema({
            schema,
            store: mockStore,
            mocks
        });

        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation(query => ({
                matches: false,
                media: query,
                onchange: null,
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    afterAll(() => {
        if (lastPresentError) {
            // eslint-disable-next-line no-console
            console.log(lastPresentError);
        }
    });

    it('should be polyfilled', () => {
        expect(window).toBeDefined();
        expect(Ya).toBeDefined();
        expect(navigator.userAgent).toBe('SSR: default');
    });

    getTestsConfig(tests).forEach(testConfig => {
        const { testName, location, data, toPresent, lang, cookies, allowSkeleton } = testConfig;

        it(testName, async() => {
            const { html } = await render({
                location,
                data,
                cookies,
                graphql: {
                    client: new ApolloClient({
                        ssrMode: true,
                        cache: getCache(),
                        link: from([
                            onError(console.error),
                            new SchemaLink({ schema: mockSchema })
                        ])
                    })
                }
            });

            expect(html).toBeDefined();
            expect(html.slice(0, 100)).toMatch(`<html lang="${lang}"`);

            if (!allowSkeleton) {
                expect(html.match(/(.{0,200}Skeleton.{0,200})/) ? RegExp.$1 : null).toBeNull();
            }

            toPresent.forEach((searchValue: string | RegExp) => {
                expect(html).toPresent(searchValue);
            });
        });
    });
});
