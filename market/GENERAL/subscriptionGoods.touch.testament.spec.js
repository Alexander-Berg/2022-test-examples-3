// @flow
// flowlint untyped-import:off

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {EXP_FLAG_ID} from '@self/root/src/resolvers/plus/constants';
import OrderTotalPO from '@self/root/src/components/OrderTotal/__pageObject';
import PriceBasePO from '@self/root/src/uikit/components/PriceBase/__pageObject';
import {CancellationButton as CancellationButtonPO} from '@self/root/src/components/OrderActions/Actions/CancellationButton/__pageObject';
import CartButtonPO from '@self/root/src/components/CartButton/__pageObject';
import {ActionLink as ActionLinkPO} from '@self/root/src/components/OrderActions/Actions/ActionLink/__pageObject';

import type {Options} from '../index';
import {ORDER_ID, ORDERS} from './__mocks__/subscriptionGoodsMocks';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const widgetPath = require.resolve('@self/root/src/widgets/content/orders/OrderCard');
const MONTH_POSTFIX: '/мес' = '/мес';

beforeAll(async () => {
    mockLocation();

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
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

async function makeContext({user = {}, exps = {}}: TestContext = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

afterAll(() => {
    mirror.destroy();
});

const checkPrice = (priceElement?: HTMLElement | null) => {
    const price: number = parseFloat(priceElement && priceElement.textContent);

    expect(Number.isFinite(price)).toBe(true);
    expect(priceElement && priceElement.textContent.includes(MONTH_POSTFIX)).toBe(true);
};

describe('OrderCard', () => {
    describe('Станция по подписке', () => {
        describe('Список заказов', () => {
            const user = {
                isAuth: true,
            };
            const widgetProps: Options = {
                props: {
                    orderId: ORDER_ID,
                    isFold: true,
                },
            };

            // SKIPPED MARKETFRONT-96354 флапает на > 5%
            [].forEach(isExpEnabled => {
                const exps = {
                    [EXP_FLAG_ID]: isExpEnabled,
                };
                const expStatus = isExpEnabled ? 'Эксперимент включен' : 'Эксперимент выключен';

                // eslint-disable-next-line jest/valid-describe
                describe(`заказ в статусе PROCESSING. ${expStatus}`, () => {
                    // https://testpalm.yandex-team.ru/bluemarket/testcases/4172
                    let container: HTMLElement;

                    beforeAll(async () => {
                        await makeContext({exps, user});
                        await kadavrLayer.setState(
                            'Checkouter.collections.order',
                            ORDERS.PROCESSING
                        );

                        const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                        container = widgetContainer;
                    });

                    it('Под названием товара указана цена с суффиксом "/мес"', async () => {
                        const priceElement = container.querySelector(PriceBasePO.root);

                        checkPrice(priceElement);
                    });

                    it('Есть кнопка отменить заказ', async () => {
                        const cancelButton = container.querySelector(CancellationButtonPO.root);

                        expect(cancelButton).toBeTruthy();
                    });

                    it('Нет ссылки Документы по заказу', async () => {
                        const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                        expect(documentsLink).toBeFalsy();
                    });

                    it('Нет кнопки В корзину', async () => {
                        const reorderButton = container.querySelector(CartButtonPO.root);

                        expect(reorderButton).toBeFalsy();
                    });
                });
            });
            [true, false].forEach(isExpEnabled => {
                const exps = {
                    [EXP_FLAG_ID]: isExpEnabled,
                };
                const expStatus = isExpEnabled ? 'Эксперимент включен' : 'Эксперимент выключен';
                // eslint-disable-next-line jest/valid-describe
                describe(`заказ в статусе DELIVERY. ${expStatus}`, () => {
                    // https://testpalm.yandex-team.ru/bluemarket/testcases/4172
                    let container: HTMLElement;

                    beforeAll(async () => {
                        await makeContext({exps, user});
                        await kadavrLayer.setState(
                            'Checkouter.collections.order',
                            ORDERS.DELIVERY
                        );

                        const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                        container = widgetContainer;
                    });

                    it('Под названием товара указана цена с суффиксом "/мес"', async () => {
                        const priceElement = container.querySelector(PriceBasePO.root);

                        checkPrice(priceElement);
                    });

                    it('Есть кнопка отменить заказ', async () => {
                        const cancelButton = container.querySelector(CancellationButtonPO.root);

                        expect(cancelButton).toBeTruthy();
                    });

                    it('Нет ссылки Документы по заказу', async () => {
                        const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                        expect(documentsLink).toBeFalsy();
                    });

                    it('Нет кнопки В корзину', async () => {
                        const reorderButton = container.querySelector(CartButtonPO.root);

                        expect(reorderButton).toBeFalsy();
                    });
                });

                // eslint-disable-next-line jest/valid-describe
                describe(`заказ в статусе DELIVERED. ${expStatus}`, () => {
                    // https://testpalm.yandex-team.ru/bluemarket/testcases/4172
                    let container: HTMLElement;

                    beforeAll(async () => {
                        await makeContext({exps, user});
                        await kadavrLayer.setState(
                            'Checkouter.collections.order',
                            ORDERS.DELIVERED
                        );

                        const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                        container = widgetContainer;
                    });

                    it('Под названием товара указана цена с суффиксом "/мес"', async () => {
                        const priceElement = container.querySelector(PriceBasePO.root);

                        checkPrice(priceElement);
                    });

                    it('Нет кнопки отменить заказ', async () => {
                        const cancelButton = container.querySelector(CancellationButtonPO.root);

                        expect(cancelButton).toBeFalsy();
                    });

                    it('Нет ссылки Документы по заказу', async () => {
                        const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                        expect(documentsLink).toBeFalsy();
                    });

                    it('Нет кнопки В корзину', async () => {
                        const reorderButton = container.querySelector(CartButtonPO.root);

                        expect(reorderButton).toBeFalsy();
                    });
                });
            });
        });

        describe('Подробности о заказе', () => {
            const user = {
                isAuth: true,
            };
            const widgetProps: Options = {
                props: {
                    orderId: ORDER_ID,
                    isFold: false,
                },
            };

            [true, false].forEach(isExpEnabled => {
                const exps = {
                    [EXP_FLAG_ID]: isExpEnabled,
                };
                const expStatus = isExpEnabled ? 'Эксперимент включен' : 'Эксперимент выключен';

                // eslint-disable-next-line jest/valid-describe
                describe(`заказ в статусе PROCESSING. ${expStatus}`, () => {
                    // https://testpalm.yandex-team.ru/bluemarket/testcases/4175
                    let container: HTMLElement;

                    beforeAll(async () => {
                        await makeContext({exps, user});
                        await kadavrLayer.setState(
                            'Checkouter.collections.order',
                            ORDERS.PROCESSING
                        );

                        const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                        container = widgetContainer;
                    });

                    it('Под названием товара указана цена с суффиксом "/мес"', async () => {
                        const priceElement = container.querySelector(PriceBasePO.root);

                        checkPrice(priceElement);
                    });

                    it('Товары: указана цена с суффиксом "/мес"', async () => {
                        const price = container.querySelector(OrderTotalPO.itemsValue);

                        checkPrice(price);
                    });

                    it('Итого: указана цена с суффиксом "/мес"', async () => {
                        const totalPrice = container.querySelector(OrderTotalPO.priceValue);

                        checkPrice(totalPrice);
                    });

                    it('Есть кнопка отменить заказ', async () => {
                        const cancelButton = container.querySelector(CancellationButtonPO.root);

                        expect(cancelButton).toBeTruthy();
                    });

                    it('Нет ссылки Документы по заказу', async () => {
                        const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                        expect(documentsLink).toBeFalsy();
                    });

                    it('Нет кнопки В корзину', async () => {
                        const reorderButton = container.querySelector(CartButtonPO.root);

                        expect(reorderButton).toBeFalsy();
                    });
                });

                // eslint-disable-next-line jest/valid-describe
                describe(`заказ в статусе DELIVERY. ${expStatus}`, () => {
                    // https://testpalm.yandex-team.ru/bluemarket/testcases/4175
                    let container: HTMLElement;

                    beforeAll(async () => {
                        await makeContext({exps, user});
                        await kadavrLayer.setState(
                            'Checkouter.collections.order',
                            ORDERS.DELIVERY
                        );

                        const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                        container = widgetContainer;
                    });

                    it('Под названием товара указана цена с суффиксом "/мес"', async () => {
                        const priceElement = container.querySelector(PriceBasePO.root);

                        checkPrice(priceElement);
                    });

                    it('Товары: указана цена с суффиксом "/мес"', async () => {
                        const price = container.querySelector(OrderTotalPO.itemsValue);

                        checkPrice(price);
                    });

                    it('Итого: указана цена с суффиксом "/мес"', async () => {
                        const totalPrice = container.querySelector(OrderTotalPO.priceValue);

                        checkPrice(totalPrice);
                    });

                    it('Есть кнопка отменить заказ', async () => {
                        const cancelButton = container.querySelector(CancellationButtonPO.root);

                        expect(cancelButton).toBeTruthy();
                    });

                    it('Нет ссылки Документы по заказу', async () => {
                        const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                        expect(documentsLink).toBeFalsy();
                    });

                    it('Нет кнопки В корзину', async () => {
                        const reorderButton = container.querySelector(CartButtonPO.root);

                        expect(reorderButton).toBeFalsy();
                    });
                });

                // eslint-disable-next-line jest/valid-describe
                describe(`заказ в статусе DELIVERED. ${expStatus}`, () => {
                    // https://testpalm.yandex-team.ru/bluemarket/testcases/4175
                    let container: HTMLElement;

                    beforeAll(async () => {
                        await makeContext({exps, user});
                        await kadavrLayer.setState(
                            'Checkouter.collections.order',
                            ORDERS.DELIVERED
                        );

                        const {container: widgetContainer} = await apiaryLayer.mountWidget(widgetPath, widgetProps);
                        container = widgetContainer;
                    });

                    it('Под названием товара указана цена с суффиксом "/мес"', async () => {
                        const priceElement = container.querySelector(PriceBasePO.root);

                        checkPrice(priceElement);
                    });

                    it('Товары: указана цена с суффиксом "/мес"', async () => {
                        const price = container.querySelector(OrderTotalPO.itemsValue);

                        checkPrice(price);
                    });

                    it('Итого: указана цена с суффиксом "/мес"', async () => {
                        const totalPrice = container.querySelector(OrderTotalPO.priceValue);

                        checkPrice(totalPrice);
                    });

                    it('Нет кнопки отменить заказ', async () => {
                        const cancelButton = container.querySelector(CancellationButtonPO.root);

                        expect(cancelButton).toBeFalsy();
                    });

                    it('Нет ссылки Документы по заказу', async () => {
                        const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                        expect(documentsLink).toBeFalsy();
                    });

                    it('Нет кнопки В корзину', async () => {
                        const reorderButton = container.querySelector(CartButtonPO.root);

                        expect(reorderButton).toBeFalsy();
                    });
                });
            });
        });
    });
});
