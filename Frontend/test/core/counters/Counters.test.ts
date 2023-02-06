import { Counters } from '../../../src/core/counters/Counters';

const { getUTMArgs, appendQueryArgs, getParam } = require('../../../core/utils/cgidata');

describe('Counters', () => {
    it('getCountersParams', () => {
        const data = {
            doc: {
                data: {
                    analytics: [
                        { id: '46417413', type: 'Yandex', webvisor: true },
                        { id: '45135375', type: 'Yandex', mode: 'mode' },
                        { id: 'UA-122962992-1', type: 'google' },
                    ],
                },
                pageId: '1234',
            },
        };

        //@ts-ignore в IData требуется слишком много лишнних параметров
        const params = Counters.getCountersParams(data);

        expect(params).toEqual({
            google: [{
                id: 'UA-122962992-1',
                type: 'google',
            }],
            yandex: [{
                id: '46417413',
                mode: undefined,
                params: {
                    pageId: '1234',
                },
                webvisor: true,
            }, {
                id: '45135375',
                mode: 'mode',
                params: {
                    pageId: '1234',
                },
                webvisor: undefined,
            }],
        });
    });

    it('getComponentParams', () => {
        const nodeContext = {
            doc: {
                url: 'http://tass.ru/kultura/4136710',
                title: 'TACC',
            },
            docId: '123',
            isPrerenderSupported: false,
            counters: {
                yandex: [{
                    id: '46417413',
                    params: {
                        pageid: '12',
                    },
                }, {
                    id: '45135375',
                    params: {
                        pageid: '34',
                    },
                }],
                google: [{
                    id: 'UA-122962992-1',
                    type: 'google',
                    url: '/url',
                }],
            },
            data: {
                cgidata: {
                    scheme: 'https',
                    hostname: 'localhost:3333',
                    text: 'exp_flags=turbo_presearch_disable%3D1&stub=metrika%2Fgoals.json&export=json&exp_flags=analytics-disabled%3D0',
                    path: '/turbo',
                    args: {
                        export: ['json'],
                        stub: ['metrika/goals.json'],
                        exp_flags: ['turbo_presearch_disable=1', 'analytics-disabled=0'],
                    },
                },
            },
            reportContext: {
                reqid: 'reqid-123'
            },
        };

        const utils = {
            cgidata: {
                getUTMArgs: getUTMArgs,
                appendQueryArgs: appendQueryArgs,
                getParam: getParam,
            },
        };

        //@ts-ignore в INodeContext требуется слишком много лишнних параметров
        const params = Counters.getComponentParams(nodeContext, {}, utils);

        expect(params).toEqual({
            id: '123',
            isPrerenderSupported: false,
            isTurboSrc: false,
            metrikaInitOnVisible: undefined,
            originalUrl: 'http://tass.ru/kultura/4136710',
            title: 'TACC',
            reqid: 'reqid-123',
        });
    });

    it('getCountersMixes', () => {
        const goals = [{
            type: 'Yandex',
            id: '46417413',
        }, {
            type: 'google',
            id: 'UA-122962992-1',
        }];

        //@ts-ignore
        const mixis = Counters.getCountersMixes({}, goals);
        expect(mixis).toEqual([{
            block: 'metrika',
            elem: 'goal',
            js: {
                goals: [{
                    id: '46417413',
                    type: 'Yandex',
                }],
            } }, {
            block: 'ganalytics',
            elem: 'goal',
            js: {
                goals: [{
                    id: 'UA-122962992-1',
                    type: 'google',
                }],
            } }]);
    });

    describe('getCountersComponents', () => {
        const counters = new Counters();

        const nodeContext = {
            docId: '123',
            counters: {
                yandex: [{
                    id: '46417413',
                    params: {
                        pageid: '12',
                    },
                }, {
                    id: '45135375',
                    params: {
                        pageid: '34',
                    },
                }],
                google: [{
                    id: 'UA-122962992-1',
                    type: 'google',
                    url: '/url',
                }],
            },
            config: {},
            doc: { url: '/url', title: 'title' },
            data: { cgidata: { args: {} } },
            expFlags: {},
            utils: {
                cgidata: {
                    getUTMArgs: getUTMArgs,
                    appendQueryArgs: appendQueryArgs,
                    getParam: getParam,
                },
            },
            isCustomDomain: false,
            isTurbopages: false,
            reportContext: {
                reqid: 'reqid-123'
            },
        };

        it('Возвращает корректный json', () => {
            //@ts-ignore в INodeContext требуется слишком много лишнних параметров
            const components = counters.getCountersComponents(nodeContext, {});

            expect(components).toEqual([
                {
                    block: 'metrika',
                    js: {
                        adv: undefined,
                        counters: [{
                            id: '46417413',
                            params: {
                                pageid: '12',
                            },
                        }, {
                            id: '45135375',
                            params: {
                                pageid: '34',
                            },
                        }],
                        experiments: '',
                        hitOptions: {
                            doc_ui: 'touch-phone',
                            domain_type: 'yandex',
                        },
                        paramsInYM: false,
                        title: 'title',
                        accurateTrackBounce: true,
                    },
                    mods: { v2: false, v3: true },
                }, {
                    block: 'tracking',
                    js: {
                        counters: [{
                            id: 'UA-122962992-1',
                            type: 'google',
                            url: '/url',
                        }],
                        title: 'title',
                    },
                }, {
                    block: 'counters',
                    js: {
                        id: '123',
                        isPrerenderSupported: undefined,
                        isTurboSrc: false,
                        metrikaInitOnVisible: undefined,
                        originalUrl: '/url',
                        title: 'title',
                        reqid: 'reqid-123',
                    },
                }, {
                    block: 'viewport-watcher',
                    js: {
                        offset: { vertical: -0.2,
                        },
                    } },
            ]);
        });

        it('Передает turbopages в метрику на turbopages.org', () => {
            //@ts-ignore в INodeContext требуется слишком много лишнних параметров
            const components = counters.getCountersComponents({
                ...nodeContext,
                isTurbopages: true,
            }, {});

            expect(components[0].js).toEqual(expect.objectContaining({
                hitOptions: {
                    doc_ui: 'touch-phone',
                    domain_type: 'turbopages',
                }
            }));
        });

        it('Передает custom в метрику на кастомных доменах', () => {
            //@ts-ignore в INodeContext требуется слишком много лишнних параметров
            const components = counters.getCountersComponents({
                ...nodeContext,
                isCustomDomain: true,
            }, {});

            expect(components[0].js).toEqual(expect.objectContaining({
                hitOptions: {
                    doc_ui: 'touch-phone',
                    domain_type: 'custom',
                }
            }));
        });

        it('Передает turbo_uid в метрику', () => {
            const components = counters.getCountersComponents({
                ...nodeContext,
                //@ts-ignore в INodeContext.data требуется слишком много лишнних параметров
                data: {
                    ...nodeContext.data,
                    reqdata: { turboUid: 'test-turbo-uid' },
                },
            }, {});

            expect(components[0].js).toEqual(expect.objectContaining({
                hitOptions: {
                    doc_ui: 'touch-phone',
                    domain_type: 'yandex',
                    turbo_uid: 'test-turbo-uid',
                }
            }));
        });
    });
});
