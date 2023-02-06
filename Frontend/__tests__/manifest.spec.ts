import { YA_COUNTER } from '../lib/metrika';

describe('Manifest', () => {
    let data = {};
    beforeEach(() => {
        data = {
            env: {
                expFlags: {},
            },
            cgidata: {
                path: '/turbo',
                args: {},
                scheme: 'https',
                hostname: 'yandex.ru',
            },
            reqdata: {
                reqid: 'reqid-test',
                language: 'en',
            },
            app_host: {
                result: {
                    docs: [
                        {
                            analytics: [{
                                type: 'google', id: 'GA_01',
                            }, {
                                type: 'yandex', id: '10000',
                            }],
                            url: 'https://mirm.ru',
                            ograph: {
                                site_name: 'Мир Музыки',
                            },
                            product_info: { turbo_shop_id: 'mirm.ru' },
                            merged_host_data: {
                                turbo_app: { title: 'Мир Музыки' },
                            },
                        },
                    ],
                },
            },
        };
    });

    beforeAll(() => {
        function mockFS() {
            const original = require.requireActual('fs');
            return {
                ...original, //Pass down all the exported objects
                existsSync: (pathToRead: string) => pathToRead.endsWith('tap-assets.json') || pathToRead.endsWith('assets-config.json'),
                readFileSync: (pathToRead: string) => {
                    if (pathToRead.endsWith('tap-assets.json') || pathToRead.endsWith('assets-config.json')) {
                        return JSON.stringify({
                            ecom: {
                                css: '/static/turbo/pages/spa/ecom/hashed_02eea3a1e331aeb2d004.ecom.css',
                                js: '/static/turbo/pages/spa/ecom/hashed_558085c4ba58f18cb35d.ecom.js',
                            },
                            metadata: {
                                version: '20200616.1700',
                                env: 'testing',
                                staticVersion: 'turbo@some-static-version',
                                rum: {
                                    inline: '',
                                    invoke: '',
                                    bundle: '',
                                },
                                counters: {
                                    error: '',
                                    helpers: '',
                                },
                            },
                        });
                    }

                    return '';
                },
            };
        }

        jest.mock('fs', () => mockFS());
    });

    afterAll(() => {
        jest.unmock('fs');
    });

    describe('createEcomManifest()', () => {
        it('Генерирует корректный манифест', () => {
            const createEcomManifest = require('../manifest').default;

            // @ts-ignore
            expect(createEcomManifest(data)).toStrictEqual({
                name: 'Мир Музыки',
                short_name: 'Мир Музыки',
                description: '',
                start_url: 'https://yandex.ru/turbo/mirm.ru/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1',
                theme_color: '#fff',
                background_color: '#fff',
                yandex: {
                    splash_screen_color: '#ffffff',
                    base_url: 'https://yandex.ru/turbo/mirm.ru/',
                    app_version: expect.stringMatching(/^(\d+\.)+\d+$/),
                    manifest_version: 1,
                    app_id: 'mirm.ru',
                    metrika_id: 10000,
                    cache: {
                        resources: expect.arrayContaining([
                            'https://yandex.ru/turbo/mirm.ru/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1',
                            'https://mc.yandex.ru/metrika/tag_turboapp.js',
                            'https://yastatic.net/s3/gdpr/popup/v2/en.js',
                        ]),
                    },
                    prefetch: {
                        entries: [
                            {
                                credentials_mode: 'include',
                                period_minutes: 30,
                                url: 'https://yandex.ru/turbo/mirm.ru/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1&isAjax=true',
                            }, {
                                credentials_mode: 'include',
                                period_minutes: 600,
                                url: 'https://yandex.ru/turbo/mirm.ru/n/yandexturbocatalog/about/?isAjax=true',
                            },
                        ],
                    },
                },
                icons: [],
            });
        });

        it('Гнерирует манифест с правильным metrika_id', () => {
            const createEcomManifest = require('../manifest').default;

            data.app_host.result.docs[0].analytics = undefined;
            expect(createEcomManifest(data).yandex.metrika_id).toBeUndefined();

            data.app_host.result.docs[0].analytics = [{
                type: 'Yandex', id: YA_COUNTER,
            }];
            expect(createEcomManifest(data).yandex.metrika_id).toBeUndefined();

            data.app_host.result.docs[0].analytics = [{
                type: 'Yandex', id: YA_COUNTER,
            }, {
                type: 'Yandex', id: '1000',
            }];
            expect(createEcomManifest(data).yandex.metrika_id).toBe(1000);
        });
    });

    describe('getManifestPath()', () => {
        it('Возвращает корректный путь к манифесту', () => {
            const { getManifestPath } = require('../manifest');

            // @ts-ignore
            expect(getManifestPath(data)).toStrictEqual(
                'https://yandex.ru/turbo/mirm.ru/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1&manifest=1'
            );
        });
    });
});
