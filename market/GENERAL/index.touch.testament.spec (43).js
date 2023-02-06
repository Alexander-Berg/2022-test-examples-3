import {screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {mockLocation, mockRouterFabric} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import * as actionsNavigate from '@self/root/src/actions/navigate';
import * as actionsCartServiceProxy from '@self/root/src/actions/cartServiceProxy';

import {
    checkCashbackTextTestCase,
} from './cashback/testCases/';
import {
    baseMockFunctionality,
    mockGetDataFromGarsonForCMSLink,
    mockGetDataFromGarsonWithCashback,
    mockGetDataFromGarsonWithExtraCashback,
} from './cashback/mockFunctionality';
import {getWidgetParams, baseNormalizedGarsonResponse, normalizedOffer} from './fixtures/data';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
    mockRouterFabric(require.resolve('@self/root/src/utils/router'))();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
});

afterAll(() => {
    mirror.destroy();
});

describe('RootScrollBox', () => {
    // Расскип https://st.yandex-team.ru/MARKETFRONT-73016
    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('CashbackInfo', () => {
        beforeAll(async () => {
            await baseMockFunctionality(jestLayer);
        });

        describe('Кэшбэк', () => {
            test('Содержит корректный текст', () => checkCashbackTextTestCase(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockGetDataFromGarsonWithCashback,
                '500'
            ));
        });

        describe('Экстра Кэшбэк', () => {
            test('Содержит корректный текст', () => checkCashbackTextTestCase(
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockGetDataFromGarsonWithExtraCashback,
                '500'
            ));
        });
    });

    describe('CMS Params', () => {
        const widgetPath = '@self/root/src/widgets/content/RootScrollBox';

        beforeEach(async () => {
            await mandrelLayer.initContext();
        });

        describe('Параметр Title', () => {
            describe('text', () => {
                beforeAll(async () => {
                    await baseMockFunctionality(jestLayer);
                    await mockGetDataFromGarsonWithCashback(jestLayer);
                });

                it('Получаем из cms', async () => {
                    const TitleText = 'С тем что вы заказывали';
                    const params = getWidgetParams({
                        wrapperProps: {
                            title: {
                                text: TitleText,
                            },
                        },
                        props: {
                            useTitleFromCms: true,
                        },
                    });
                    await apiaryLayer.mountWidget(widgetPath, params);

                    screen.getByText(new RegExp(TitleText, 'i'));
                });

                it('Получаем с бекэнда', async () => {
                    const TitleText = 'С тем что вы заказывали';
                    const params = getWidgetParams({
                        wrapperProps: {
                            title: {
                                text: TitleText,
                            },
                        },
                    });
                    await apiaryLayer.mountWidget(widgetPath, params);

                    screen.getByText(new RegExp(baseNormalizedGarsonResponse.additionalParams.title, 'i'));
                });
            });


            describe('link', () => {
                /** @type {jest.SpyInstance} */
                let actionNavigate;
                beforeAll(async () => {
                    await baseMockFunctionality(jestLayer);
                    await mockGetDataFromGarsonForCMSLink(jestLayer);
                    actionNavigate = jest.spyOn(actionsNavigate, 'navigate');
                });

                it('Получаем из cms', async () => {
                    const user = userEvent.setup();
                    const linkTitle = 'Смотреть все';
                    const params = getWidgetParams({
                        wrapperProps: {
                            title: {
                                text: 'Some title',
                                link: {
                                    url: '/test',
                                    params: {},
                                    text: linkTitle,
                                },
                            },
                        },
                        props: {
                            useTitleFromCms: true,
                        },
                    });

                    await apiaryLayer.mountWidget(widgetPath, params);
                    const link = screen.getByText(new RegExp(linkTitle, 'i'));
                    expect(window.location.pathname).not.toEqual('/test');
                    await user.click(link);
                    expect(actionNavigate.mock.calls).toHaveLength(1);
                    expect(actionNavigate.mock.calls[0][0]).toEqual({url: '/test'});
                });
            });
        });

        describe('Параметр Snippet', () => {
            beforeAll(async () => {
                await baseMockFunctionality(jestLayer);
                await mockGetDataFromGarsonWithCashback(jestLayer);
            });

            it('Показывает заголовок сниппетов', async () => {
                const params = getWidgetParams({
                    props: {
                        snippets: {
                            withTitle: true,
                        },
                    },
                });
                await apiaryLayer.mountWidget(widgetPath, params);

                screen.getByText(new RegExp(normalizedOffer.titles.raw, 'i'));
            });

            /** Базовая тема не поддерживает скрытие тайтла для сниппета */
            // SKIPPED MARKETFRONT-96354
            // eslint-disable-next-line jest/no-disabled-tests
            it.skip('Не показывает заголовок сниппетов', async () => {
                const params = getWidgetParams({
                    props: {
                        snippets: {
                            withTitle: false,
                        },
                    },
                });
                await apiaryLayer.mountWidget(widgetPath, params);

                expect(() => screen.getByText(new RegExp(normalizedOffer.titles.raw, 'i'))).toThrow();
            });

            describe('Кнопка "В корзину"', () => {
                /** @type {jest.SpyInstance} */
                let addOfferToCart;
                beforeAll(() => {
                    mockLocation('market:product');
                    addOfferToCart = jest.spyOn(actionsCartServiceProxy, 'addOfferToCart');
                });

                it('Показывает', async () => {
                    const params = getWidgetParams({
                        props: {
                            snippets: {
                                withTitle: true,
                                withCartButton: true,
                            },
                        },
                    });
                    await apiaryLayer.mountWidget(widgetPath, params);

                    screen.getByText(/В корзину/i);
                });

                it('Не показывает', async () => {
                    const params = getWidgetParams({
                        props: {
                            snippets: {
                                withTitle: false,
                                withCartButton: false,
                            },
                        },
                    });
                    await apiaryLayer.mountWidget(widgetPath, params);

                    expect(() => screen.getByText(/В корзину/i)).toThrow();
                });

                it('Добавляет товар в корзину', async () => {
                    const user = userEvent.setup();
                    const params = getWidgetParams({
                        props: {
                            snippets: {
                                withTitle: false,
                                withCartButton: true,
                            },
                        },
                    });
                    await apiaryLayer.mountWidget(widgetPath, params);
                    expect(addOfferToCart.mock.calls).toHaveLength(0);
                    await user.click(screen.getByText(/В корзину/i));
                    expect(addOfferToCart.mock.calls).toHaveLength(1);
                });
            });
        });
    });
});
