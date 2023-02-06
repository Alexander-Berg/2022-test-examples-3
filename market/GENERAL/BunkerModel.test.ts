import {Request} from 'express';

import {DEFAULT_FEATURE_FLAGS} from '../../constants/featureFlags';
import BunkerModel from './BunkerModel';

describe('BunkerModel', () => {
    describe('Флаг дашборда', () => {
        describe('Выключен', () => {
            test('При отсутствии данных бункера', () => {
                const requestMock = {} as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });

            test('При отсутствии настроек дашборда в бункере', () => {
                const requestMock = {
                    bunker: {
                        'feature-flags': {},
                        'dashboard-settings': {},
                    },
                } as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });

            test('При неправильных данных в бункере', () => {
                const requestMock = {
                    bunker: {
                        'feature-flags': {},
                        'dashboard-settings': [{settings: 'off'}, {widgets: 'on'}],
                    },
                } as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });

            test('При выключении в бункере', () => {
                const requestMock = {
                    bunker: {
                        'dashboard-settings': {
                            enabled: false,
                            widgets: [],
                        },
                    },
                } as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });

            test('При отсутствии виджетов', () => {
                const requestMock = {
                    bunker: {
                        'dashboard-settings': {
                            enabled: true,
                            widgets: [],
                        },
                    },
                } as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });

            test('При отсутствии известных виджетов', () => {
                const requestMock = {
                    bunker: {
                        'dashboard-settings': {
                            enabled: true,
                            widgets: [
                                {
                                    id: 'unknwonWidget',
                                    heading: 'Новый неизведанный виджет',
                                    enabled: true,
                                },
                            ],
                        },
                    },
                } as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });

            test('При отсутствии известных включенных виджетов', () => {
                const requestMock = {
                    bunker: {
                        'dashboard-settings': {
                            enabled: true,
                            widgets: [
                                {
                                    id: 'contract-information',
                                    heading: 'Договоры',
                                    enabled: false,
                                },
                                {
                                    id: 'waiting-approval-marketing-campaigns',
                                    heading: 'Неподтвержденные маркетинговые услуги',
                                    enabled: false,
                                },
                            ],
                        },
                    },
                } as Request;

                const {featureFlags} = new BunkerModel(requestMock);

                expect(featureFlags).toEqual({
                    ...DEFAULT_FEATURE_FLAGS,
                    useDashboard: false,
                });
            });
        });

        test('Включен, если включен в бункере и включен хотя бы 1 известный виджет', () => {
            const requestMock = {
                bunker: {
                    'dashboard-settings': {
                        enabled: true,
                        widgets: [
                            {
                                id: 'contract-information',
                                heading: 'Договоры',
                                enabled: false,
                            },
                            {
                                id: 'waiting-approval-marketing-campaigns',
                                heading: 'Неподтвержденные маркетинговые услуги',
                                enabled: true,
                            },
                        ],
                    },
                },
            } as Request;

            const {featureFlags, dashboardWidgets} = new BunkerModel(requestMock);

            expect(featureFlags).toEqual({
                ...DEFAULT_FEATURE_FLAGS,
                useDashboard: true,
            });

            expect(dashboardWidgets).toEqual([
                {
                    id: 'waiting-approval-marketing-campaigns',
                    heading: 'Неподтвержденные маркетинговые услуги',
                    enabled: true,
                },
            ]);
        });

        test('Порядок виджетов совпадает с бункером', () => {
            const widgetMockOne = {
                id: 'contract-information',
                heading: 'Договоры',
                enabled: true,
            };

            const widgetMockTwo = {
                id: 'waiting-approval-marketing-campaigns',
                heading: 'Неподтвержденные маркетинговые услуги',
                enabled: true,
            };

            const requestMock = {
                bunker: {
                    'dashboard-settings': {
                        enabled: true,
                        widgets: [widgetMockOne, widgetMockTwo],
                    },
                },
            } as Request;

            const requestMockReversed = {
                bunker: {
                    'dashboard-settings': {
                        enabled: true,
                        widgets: [widgetMockTwo, widgetMockOne],
                    },
                },
            } as Request;

            const {dashboardWidgets} = new BunkerModel(requestMock);
            const {dashboardWidgets: dashboardWidgetsReversed} = new BunkerModel(requestMockReversed);

            expect(dashboardWidgets).toEqual([widgetMockOne, widgetMockTwo]);
            expect(dashboardWidgetsReversed).toEqual([widgetMockTwo, widgetMockOne]);
        });
    });

    describe('Фичафлаги', () => {
        test('Дефолтные флаги при отсутствии данных бункера', () => {
            const requestMock = {
                bunker: {},
            } as Request;

            const {featureFlags} = new BunkerModel(requestMock);

            expect(featureFlags).toEqual({
                ...DEFAULT_FEATURE_FLAGS,
                useDashboard: false,
            });
        });

        // На текущий момент нет дефолтных флагов
        //
        // test('Флаги из бункера переопределяют дефолтные', () => {
        //     const requestMock = {
        //         bunker: {
        //             'feature-flags': {
        //                 [name - заменить]: !DEFAULT_FEATURE_FLAGS.useCompensatedSum,
        //             },
        //         },
        //     } as Request;
        //
        //     const {featureFlags} = new BunkerModel(requestMock);
        //
        //     expect(featureFlags).toEqual({
        //         ...DEFAULT_FEATURE_FLAGS,
        //         [name - заменить]: !DEFAULT_FEATURE_FLAGS.useCompensatedSum,
        //         useDashboard: false,
        //     });
        // });

        test('Флаг дашборда добавляется к флагам из бункера', () => {
            const requestMock = {
                bunker: {
                    'feature-flags': {
                        useAssortmentPage: true,
                    },
                },
            } as Request;

            const {featureFlags} = new BunkerModel(requestMock);

            expect(featureFlags).toEqual({
                useAssortmentPage: true,
                useDashboard: false,
            });
        });

        test('Любой флаг из бункера попадет на фронте', () => {
            const requestMock = {
                bunker: {
                    'feature-flags': {
                        testFlagOne: true,
                        testFlagTwo: false,
                        testNonBooleanFlag: '[object Object]',
                    },
                },
            } as Request;

            const {featureFlags} = new BunkerModel(requestMock);

            expect(featureFlags).toEqual({
                ...DEFAULT_FEATURE_FLAGS,
                testFlagOne: true,
                testFlagTwo: false,
                testNonBooleanFlag: '[object Object]',
                useDashboard: false,
            });
        });
    });

    describe('Опросы', () => {
        test('Данных для опроса не будет, если не пришли из бункера', () => {
            const requestMock = {
                bunker: {},
            } as Request;

            const {surveyData} = new BunkerModel(requestMock);

            expect(surveyData).toBeNull();
        });

        test('Данных для опроса не будет, если из бункера пришло что-то неожиданное', () => {
            const requestMock = {
                bunker: {
                    survey: {
                        disabled: 'none',
                        announcement: true,
                        someText: 13,
                    },
                },
            } as Request;

            const {surveyData} = new BunkerModel(requestMock);

            expect(surveyData).toBeNull();
        });

        test('Данных для опроса не будет, если не будет обязательных полей', () => {
            const requestMock1 = {
                bunker: {
                    survey: {
                        enabled: true,
                    },
                },
            } as Request;

            const requestMock2 = {
                bunker: {
                    survey: {
                        enabled: true,
                        slug: '1234abc',
                        announcement: 'Новый опрос',
                    },
                },
            } as Request;

            const requestMock3 = {
                bunker: {
                    survey: {
                        enabled: true,
                        slug: '1234abc',
                        announcement: 'Новый опрос',
                        announcementButton: 'жми',
                    },
                },
            } as Request;

            const {surveyData: surveyData1} = new BunkerModel(requestMock1);
            const {surveyData: surveyData2} = new BunkerModel(requestMock2);
            const {surveyData: surveyData3} = new BunkerModel(requestMock3);

            expect(surveyData1).toBeNull();
            expect(surveyData2).toBeNull();
            expect(surveyData3).toEqual({
                slug: '1234abc',
                announcement: 'Новый опрос',
                announcementButton: 'жми',
            });
        });

        test('Работает только с валидными данными из бункера', () => {
            const requestMock1 = {
                bunker: {
                    survey: {
                        enabled: 'yes',
                        slug: 1234,
                        announcement: ['Новый', 'опрос'],
                        announcementButton: {label: 'жми'},
                        modalHeading: 123,
                    },
                },
            } as Request;

            const requestMock2 = {
                bunker: {
                    survey: {
                        enabled: true,
                        slug: '1234abc',
                        announcement: 'Новый опрос',
                        announcementButton: 'жми',
                        modalHeading: 'Опрос ниже',
                    },
                },
            } as Request;

            const {surveyData: surveyData1} = new BunkerModel(requestMock1);
            const {surveyData: surveyData2} = new BunkerModel(requestMock2);

            expect(surveyData1).toBeNull();
            expect(surveyData2).toEqual({
                slug: '1234abc',
                announcement: 'Новый опрос',
                announcementButton: 'жми',
                modalHeading: 'Опрос ниже',
            });
        });
    });
});
