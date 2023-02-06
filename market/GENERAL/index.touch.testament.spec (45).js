import {waitFor} from '@testing-library/dom';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import allBonuses from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bonuses';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';

import BonusTitlePO from '@self/root/src/widgets/parts/BonusProfile/components/BonusTitle/__pageObject';
import TitlePO from '@self/root/src/uikit/components/Title/__pageObject';
import CoinPO from '@self/root/src/uikit/components/Coin/__pageObject';
import CoinEmptyPO from '@self/root/src/components/CoinEmpty/__pageObject';
import InfoBonusPO from '@self/root/src/widgets/parts/BonusProfile/components/InfoBonus/__pageObject';
import {DefaultBanner as DefaultBannerPO} from '@self/root/src/widgets/parts/BonusProfile/components/NonAuthBonus/__pageObject';
import {Button as ButtonPO} from '@self/root/src/uikit/components/Button/__pageObject';
import LinkPO from '@self/root/src/components/Link/__pageObject';

import {titleLinksCases, emptyCouponCases} from './__mocks__';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

async function makeContext(user = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user,
        request: {
            cookie,
        },
    });
}

describe('Widget: BonusProfile', () => {
    const widgetPath = '@self/root/src/widgets/parts/BonusProfile';

    const widgetOptions = {};

    const coins = [
        allBonuses.FIXED_1000,
        allBonuses.FIXED,
        allBonuses.PERCENT,
    ];

    const setLoyaltyState = async () => {
        await kadavrLayer.setState('Loyalty.collections.bonus', coins);
    };

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirrorTouch({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });

        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        await jestLayer.runCode(() => {
            // eslint-disable-next-line global-require
            const {mockRouterFabric} = require('@self/root/src/helpers/testament/mock');
            mockRouterFabric()({
                'external:passport-auth': '//pass-test.yandex.ru/auth',
                'touch:index': '/market.yandex.ru',
                'external:plus-tld': ({tld}) => `/plus.yandex.${tld}`,
            });
        }, []);

        await jestLayer.doMock(
            require.resolve('@self/root/src/widgets/parts/CoinPopup'),
            () => ({create: () => Promise.resolve(null)})
        );

        await jestLayer.doMock(require.resolve('@self/root/src/utils/unsafe/user.js'), () => {
            const originalModule = jest.requireActual('@self/root/src/utils/unsafe/user.js');

            return {
                __esModule: true,
                ...originalModule,
                // eslint-disable-next-line global-require
                getRegion: jest.fn().mockReturnValue(require('@self/root/src/entities/user/__spec__/mocks').regionMock),
                // eslint-disable-next-line global-require
                getCurrentUser: jest.fn().mockReturnValue(require('@self/root/src/entities/user/__spec__/mocks').userMock),
            };
        });

        await jestLayer.doMock(
            require.resolve('@self/root/src/resources/loyalty'),
            () => {
                const originalModule = jest.requireActual('@self/root/src/resources/loyalty');
                return {
                    __esModule: true,
                    ...originalModule,
                    fetchBonusesForUnauthorized: jest.fn().mockResolvedValue({
                        result: {
                            bonusIds: [],
                            futureBonusIds: [],
                        },
                        collections: {},
                    }),

                };
            }
        );
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe('Заголовок страницы', () => {
        describe('Авторизованный пользователь', () => {
            beforeEach(async () => {
                await makeContext({isAuth: true});
                await setLoyaltyState();
            });
            test('Должен отображаться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const title = container.querySelector(`${BonusTitlePO.root} ${TitlePO.root}`);

                await waitFor(() => expect(title).toBeVisible());
            });
            test('Содержит корректный текст', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const title = container.querySelector(`${BonusTitlePO.root} ${TitlePO.root}`);

                expect(title).toHaveTextContent('Моя коллекция купонов');
            });
        });
        describe('Неавторизованный пользователь', () => {
            beforeEach(async () => {
                await makeContext({isAuth: false});
                await setLoyaltyState();
            });
            test('Должен отображаться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const title = container.querySelector(`${BonusTitlePO.root} ${TitlePO.root}`);

                await waitFor(() => expect(title).toBeVisible());
            });
            test('Содержит корректный текст', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const title = container.querySelector(`${BonusTitlePO.root} ${TitlePO.root}`);

                expect(title).toHaveTextContent('Ваша коллекция купонов');
            });
        });
    });

    describe('Блок ссылок', () => {
        beforeEach(async () => {
            await makeContext({isAuth: true});
            await setLoyaltyState();
        });
        describe('Авторизованный пользователь', () => {
            describe.each(titleLinksCases.map(testLinksCase => [testLinksCase.testTitle, testLinksCase]))(
                'Ссылка "%s"',
                (testTitle, {testTitle: expectedText, selector, expectedUrl}) => {
                    test('Ссылка должна отображаться', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const link = container.querySelector(selector);

                        await waitFor(() => expect(link).toBeVisible());
                    });

                    test(`Ссылка должна содержать текст "${expectedText}"`, async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const link = container.querySelector(selector);

                        expect(link).toHaveTextContent(expectedText);
                    });

                    test('Ссылка содержит корректный путь', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const link = container.querySelector(selector);

                        expect(link).toHaveAttribute('href', expect.stringMatching(expectedUrl));
                    });
                });
        });
        describe('Неавторизованный пользователь', () => {
            describe.each(titleLinksCases.map(testLinksCase => [testLinksCase.testTitle, testLinksCase]))(
                'Ссылка "%s"',
                (testTitle, {testTitle: expectedText, selector, expectedUrl}) => {
                    test('Ссылка должна отображаться', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const link = container.querySelector(selector);

                        await waitFor(() => expect(link).toBeVisible());
                    });

                    test(`Ссылка должна содержать текст "${expectedText}"`, async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const link = container.querySelector(selector);

                        expect(link).toHaveTextContent(expectedText);
                    });

                    test('Ссылка содержит корректный путь', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const link = container.querySelector(selector);

                        expect(link).toHaveAttribute('href', expect.stringMatching(expectedUrl));
                    });
                });
        });
    });

    describe('Список купонов', () => {
        beforeEach(async () => {
            await makeContext({isAuth: true});
            await setLoyaltyState();
        });
        describe('Авторизованный пользователь', () => {
            test('Содержит верное количество купонов', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const coupons = container.querySelectorAll(CoinPO.root);

                expect(coupons.length).toBe(coins.length);
            });

            test('Содержит пустой информационный купон', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const emptyCoupon = container.querySelector(CoinEmptyPO.root);

                await waitFor(() => expect(emptyCoupon).toBeVisible());
            });
        });
    });

    describe('Блок "Что нужно знать о купонах"', () => {
        beforeEach(async () => {
            await makeContext({isAuth: true});
            await setLoyaltyState();
        });
        describe('Авторизованный пользователь', () => {
            test('По-умолчанию отображается', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const infoBonus = container.querySelector(InfoBonusPO.root);

                await waitFor(() => expect(infoBonus).toBeVisible());
            });
        });
        describe('Неавторизованный пользователь', () => {
            test('По-умолчанию отображается', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                const infoBonus = container.querySelector(InfoBonusPO.root);

                await waitFor(() => expect(infoBonus).toBeVisible());
            });
        });
    });

    describe('Баннер', () => {
        beforeEach(async () => {
            await makeContext({isAuth: false});
            await setLoyaltyState();
        });
        describe('Неавторизованный пользователь', () => {
            it('Заголовок содержит текст "Купоны можно копить"', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const bannerTitle = container.querySelector(`${DefaultBannerPO.root} ${TitlePO.root}`);

                expect(bannerTitle).toHaveTextContent('Купоны можно копить');
            });

            it('Кнопка содержит текст "Войти и копить', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const bannerButton = container.querySelector(`${DefaultBannerPO.root} ${ButtonPO.root}`);

                expect(bannerButton).toHaveTextContent('Войти и копить');
            });

            it('Кнопка содержит корректную ссылку', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const bannerButtonLink = container.querySelector(`${DefaultBannerPO.root} ${LinkPO.root}`);

                expect(bannerButtonLink).toHaveAttribute('href', expect.stringMatching('/auth'));
            });
        });
    });

    describe('Карточка пустого купона', () => {
        describe('Авторизованный пользователь', () => {
            beforeAll(async () => {
                await kadavrLayer.setState('Loyalty.bonus', []);
            });

            beforeEach(async () => {
                await makeContext({isAuth: true});
            });

            describe.each(emptyCouponCases.map(emptyCouponCase => [emptyCouponCase.testTitle, emptyCouponCase]))(
                '%s',
                (testTitle, emptyCouponCase) => {
                    beforeAll(async () => {
                        await kadavrLayer.setState('report', {
                            data: {
                                search: {results: []},
                                blueTariffs: {
                                    ...deliveryConditionMock,
                                    tier: emptyCouponCase.tier,
                                },
                            },
                        });
                        await kadavrLayer.setState('Loyalty.collections.perks', emptyCouponCase.perks || []);
                    });

                    test('Текст заголовка отображается корректно', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const emptyCouponTitle = container.querySelector(`${CoinEmptyPO.root} ${CoinEmptyPO.title}`);

                        expect(emptyCouponTitle).toHaveTextContent(emptyCouponCase.title);
                    });

                    test('Текст подзаголовка отображается корректно', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const emptyCouponSubtitle = container.querySelector(`${CoinEmptyPO.root} ${CoinEmptyPO.subtitle}`);

                        expect(emptyCouponSubtitle).toHaveTextContent(emptyCouponCase.subtitle);
                    });

                    test('Текст кнопки отображается корректно', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const emptyCouponButton = container.querySelector(`${CoinEmptyPO.root} ${CoinEmptyPO.button}`);

                        expect(emptyCouponButton).toHaveTextContent(emptyCouponCase.buttonText);
                    });

                    test('Ссылка кнопки имеет корректный путь', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                        const emptyCouponButton = container.querySelector(`${CoinEmptyPO.root} ${CoinEmptyPO.button}`);
                        const expectedUrl = emptyCouponCase.buttonLink.hostname ? emptyCouponCase.buttonLink.hostname : '/';

                        expect(emptyCouponButton).toHaveAttribute('href', expect.stringMatching(expectedUrl));
                    });
                }
            );
        });
    });
});
