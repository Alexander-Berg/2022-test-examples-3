import {waitFor} from '@testing-library/dom';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver, mockScrollBy} from '@self/root/src/helpers/testament/mock';
import CoinPopupPO from '@self/root/src/components/CoinPopup/View/__pageObject';
import SimpleCoinPO from '@self/root/src/components/CoinPopup/SimpleCoin/__pageObject';
import LinkPO from '@self/root/src/components/Link/__pageObject';
import BonusLinkPO from '@self/root/src/components/CoinPopup/BonusLink/__pageObject';
import CoinInfoPO from '@self/root/src/components/CoinPopup/Info/__pageObject';
import CoinHeadPO from '@self/root/src/components/CoinPopup/Head/__pageObject';
import {COIN_STATUSES, COIN_REASONS} from '@self/root/src/entities/coin';

import {couponCases, refCases} from './__mocks__';

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

describe('Widget: CoinPopup', () => {
    const widgetPath = '@self/root/src/widgets/parts/CoinPopup';


    beforeAll(async () => {
        mockIntersectionObserver();
        mockScrollBy();

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
                'touch:order': ({orderId}) => `/my/order/${orderId}`,
                'touch:orders': '/my/orders',
                'touch:index': '/',
                'touch:search': ({hid, bonusId}) => {
                    const path = '/search';
                    let queryStr = '';
                    if (hid) {
                        queryStr += `hid=${hid}`;
                    }
                    if (bonusId) {
                        queryStr += `bonusId=${bonusId}`;
                    }
                    return queryStr ? `${path}?${queryStr}` : path;
                },
            });
        }, []);
    });

    afterAll(() => {
        mirror.destroy();
    });

    describe.each(couponCases.map(couponCase => [couponCase.testTitle, couponCase]))(
        '%s',
        (testTitle, {bonusIds, selectedBonusId, bonus: bonusCollection}) => {
            const widgetOptions = {
                bonusIds,
                selectedBonusId,
                bonus: bonusCollection,
            };
            const bonus = bonusCollection[selectedBonusId];

            beforeEach(async () => {
                await makeContext({isAuth: true});
            });

            test('Отображается карточка купона', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const coinInsidePopup = container.querySelector(`${CoinPopupPO.root} ${SimpleCoinPO.root}`);

                await waitFor(() => expect(coinInsidePopup).toBeVisible());
            });

            test('Заголовок должен отображaться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const coinInfoTitle = container.querySelector(`${CoinPopupPO.root} ${CoinInfoPO.title}`);

                await waitFor(() => expect(coinInfoTitle).toBeVisible());
            });

            test('Заголовок должен быть корректным', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const coinInfoTitle = container.querySelector(`${CoinPopupPO.root} ${CoinInfoPO.title}`);

                expect(coinInfoTitle).toHaveTextContent(bonus.title);
            });

            describe('Корректное состояние кнопки действия в попапе', () => {
                if (bonus.status !== COIN_STATUSES.INACTIVE) {
                    test('Кнопка "Выбрать товары" должна содержать корректный текст', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                        const coinBonusLink = container.querySelector(`${CoinPopupPO.root} ${BonusLinkPO.root} ${LinkPO.root}`);

                        expect(coinBonusLink).toHaveTextContent('Выбрать товары');
                    });
                } else if (bonus.reason !== COIN_REASONS.ORDER) {
                    test('Кнопка должна быть скрыта', async () => {
                        const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                        const coinBonusLink = container.querySelector(`${CoinPopupPO.root} ${BonusLinkPO.root} ${LinkPO.root}`);

                        expect(coinBonusLink).not.toBeInTheDocument();
                    });
                } else if (bonus.reasonOrderIds) {
                    if (bonus.reasonOrderIds.length === 1) {
                        test('Ссылка на отслеживание заказа должна содержать текст "Посмотреть детали заказа"', async () => {
                            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                            const coinBonusLink = container.querySelector(`${CoinPopupPO.root} ${BonusLinkPO.root} ${LinkPO.root}`);

                            expect(coinBonusLink).toHaveTextContent('Посмотреть детали заказа');
                        });
                    }

                    if (bonus.reasonOrderIds.length > 1) {
                        test('Ссылка на отслеживание заказа должна содержать текст "Отследить заказы"', async () => {
                            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                            const coinBonusLink = container.querySelector(`${CoinPopupPO.root} ${BonusLinkPO.root} ${LinkPO.root}`);

                            expect(coinBonusLink).toHaveTextContent('Отследить заказы');
                        });
                    }
                }
            });

            describe('Корректный статус купона', () => {
                test('Статус купона должен отображaться', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    const coinHead = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root}`);

                    await waitFor(() => expect(coinHead).toBeVisible());
                });

                if (bonus.status !== COIN_STATUSES.INACTIVE) {
                    describe('Активный бонус', () => {
                        test('Срок действия должен содержать корректный текст', async () => {
                            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                            const coinHead = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root}`);

                            expect(coinHead).toHaveTextContent(/до \d/);
                        });
                    });
                } else if (bonus.reason !== COIN_REASONS.ORDER) {
                    describe('Неактивный бонус не за заказ', () => {
                        test('Причина неактивности должна отображаться', async () => {
                            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                            const coinHead = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root}`);

                            expect(coinHead).toHaveTextContent(bonus.inactiveDescription);
                        });
                    });
                } else if (bonus.reasonOrderIds) {
                    if (bonus.reasonOrderIds.length === 1) {
                        describe('Неактивный бонус за заказ', () => {
                            test('Условие активации должно содержать корректный текст', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHead = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root}`);
                                const orderId = bonus.reasonOrderIds[0];

                                expect(coinHead).toHaveTextContent(`Начнет действовать после доставки заказа ${orderId}`);
                            });

                            test('Текст ссылки на заказ должен быть корректным', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHeadFirstLink = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root} ${CoinHeadPO.getOrderLinkSelector(0)}`);
                                const orderId = bonus.reasonOrderIds[0];

                                expect(coinHeadFirstLink).toHaveTextContent(orderId);
                            });

                            test('Ссылка должна содержать корректный путь', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHeadFirstLink = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root} ${CoinHeadPO.getOrderLinkSelector(0)}`);
                                const orderId = bonus.reasonOrderIds[0];

                                expect(coinHeadFirstLink).toHaveAttribute('href', expect.stringMatching(`/my/order/${orderId}`));
                            });
                        });
                    } else if (bonus.reasonOrderIds.length > 1) {
                        describe('Неактивный бонус за заказы', () => {
                            test('Условие активации должно содержать корректный текст', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHead = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root}`);
                                const orderIds = bonus.reasonOrderIds;

                                expect(coinHead).toHaveTextContent(`Начнет действовать после доставки заказов ${orderIds.join(', ')}`);
                            });

                            test('Текст первой ссылки на заказ должен быть корректным', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHeadFirstLink = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root} ${CoinHeadPO.getOrderLinkSelector(0)}`);
                                const orderIds = bonus.reasonOrderIds;

                                expect(coinHeadFirstLink).toHaveTextContent(String(orderIds[0]));
                            });

                            test('Первая ссылка должна содержать корректный путь', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHeadFirstLink = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root} ${CoinHeadPO.getOrderLinkSelector(0)}`);
                                const orderIds = bonus.reasonOrderIds;

                                expect(coinHeadFirstLink).toHaveAttribute('href', expect.stringMatching(`/my/order/${orderIds[0]}`));
                            });

                            test('Текст второй ссылки на заказ должен быть корректным', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHeadSecondLink = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root} ${CoinHeadPO.getOrderLinkSelector(1)}`);
                                const orderIds = bonus.reasonOrderIds;

                                expect(coinHeadSecondLink).toHaveTextContent(String(orderIds[1]));
                            });

                            test('Вторая ссылка должна содержать корректный путь', async () => {
                                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                                const coinHeadSecondLink = container.querySelector(`${CoinPopupPO.root} ${CoinHeadPO.root} ${CoinHeadPO.getOrderLinkSelector(1)}`);
                                const orderIds = bonus.reasonOrderIds;

                                expect(coinHeadSecondLink).toHaveAttribute('href', expect.stringMatching(`/my/order/${orderIds[1]}`));
                            });
                        });
                    }
                }
            });

            test('Описание должно отображaться', async () => {
                const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                const coinInfoDescription = container.querySelector(`${CoinPopupPO.root} ${CoinInfoPO.description}`);

                await waitFor(() => expect(coinInfoDescription).toBeVisible());
            });
        });

    describe('Ссылка из купона', () => {
        describe.each(refCases.map(refCase => [refCase.testTitle, refCase]))(
            '%s',
            (title, {bonusIds, selectedBonusId, bonus: bonusCollection, expectedText, expectedLink}) => {
                const widgetOptions = {
                    bonusIds,
                    selectedBonusId,
                    bonus: bonusCollection,
                };

                beforeEach(async () => {
                    await makeContext({isAuth: true});
                });

                test(`Текст ссылки должен быть ${expectedText}`, async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    const coinBonusLink = container.querySelector(`${CoinPopupPO.root} ${BonusLinkPO.root} ${LinkPO.root}`);

                    expect(coinBonusLink).toHaveTextContent(expectedText);
                });

                test('Ссылка должна содеражть корректный путь', async () => {
                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    const coinBonusLink = container.querySelector(`${CoinPopupPO.root} ${BonusLinkPO.root} ${LinkPO.root}`);

                    expect(coinBonusLink).toHaveAttribute('href', expectedLink);
                });
            });
    });
});
