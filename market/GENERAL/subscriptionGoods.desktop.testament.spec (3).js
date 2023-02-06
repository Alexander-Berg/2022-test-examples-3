// @flow
// flowlint untyped-import:off

import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {EXP_FLAG_ID} from '@self/root/src/resolvers/plus/constants';
import DetailsPriceContentPO from '@self/root/src/components/Orders/OrderDetailsPrice/DetailsPriceContent/__pageObject';
import OrderSectionPO from '@self/root/src/components/Orders/MyOrderSection/__pageObject';
import OrderCancelButtonPO from '@self/root/src/components/Orders/OrderCancelButton/__pageObject';
import ReorderButtonPO from '@self/root/src/components/Orders/OrderReorderButton/__pageObject';
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

    mirror = await makeMirrorDesktop({
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
        const user = {
            isAuth: true,
        };
        const widgetProps: Options = {
            props: {
                orderId: ORDER_ID,
            },
        };

        // SKIPPED MARKETFRONT-96354 флапает на 5%
        [].forEach(isExpEnabled => {
            const exps = {
                [EXP_FLAG_ID]: isExpEnabled,
            };
            const expStatus = isExpEnabled ? 'Эксперимент включен' : 'Эксперимент выключен';

            // eslint-disable-next-line jest/valid-describe
            describe(`заказ в статусе PROCESSING. ${expStatus}`, () => {
                // https://testpalm.yandex-team.ru/bluemarket/testcases/4171
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
                    const priceElement = container.querySelector(DetailsPriceContentPO.root);

                    checkPrice(priceElement);
                });

                it('Способ оплаты: указана цена с суффиксом "/мес"', async () => {
                    const detailsPrice = container.querySelector(OrderSectionPO.detailsPrice);
                    const priceElement = detailsPrice && detailsPrice.querySelector(DetailsPriceContentPO.root);

                    checkPrice(priceElement);
                });

                it('Есть кнопка отменить заказ', async () => {
                    const cancelButton = container.querySelector(OrderCancelButtonPO.root);

                    expect(cancelButton).toBeTruthy();
                });

                it('Нет ссылки Документы по заказу', async () => {
                    const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                    expect(documentsLink).toBeFalsy();
                });

                it('Нет кнопки Повторить заказ', async () => {
                    const reorderButton = container.querySelector(ReorderButtonPO.root);

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
                // https://testpalm.yandex-team.ru/bluemarket/testcases/4171
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
                    const priceElement = container.querySelector(DetailsPriceContentPO.root);

                    checkPrice(priceElement);
                });

                it('Способ оплаты: указана цена с суффиксом "/мес"', async () => {
                    const detailsPrice = container.querySelector(OrderSectionPO.detailsPrice);
                    const priceElement = detailsPrice && detailsPrice.querySelector(DetailsPriceContentPO.root);

                    checkPrice(priceElement);
                });

                it('Есть кнопка отменить заказ', async () => {
                    const cancelButton = container.querySelector(OrderCancelButtonPO.root);

                    expect(cancelButton).toBeTruthy();
                });

                it('Нет ссылки Документы по заказу', async () => {
                    const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                    expect(documentsLink).toBeFalsy();
                });

                it('Нет кнопки Повторить заказ', async () => {
                    const reorderButton = container.querySelector(ReorderButtonPO.root);

                    expect(reorderButton).toBeFalsy();
                });
            });

            // eslint-disable-next-line jest/valid-describe
            describe(`заказ в статусе DELIVERED. ${expStatus}`, () => {
                // https://testpalm.yandex-team.ru/bluemarket/testcases/4171
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
                    const priceElement = container.querySelector(DetailsPriceContentPO.root);

                    checkPrice(priceElement);
                });

                it('Способ оплаты: указана цена с суффиксом "/мес"', async () => {
                    const detailsPrice = container.querySelector(OrderSectionPO.detailsPrice);
                    const priceElement = detailsPrice && detailsPrice.querySelector(DetailsPriceContentPO.root);

                    checkPrice(priceElement);
                });

                it('Нет кнопки отменить заказ', async () => {
                    const cancelButton = container.querySelector(OrderCancelButtonPO.root);

                    expect(cancelButton).toBeFalsy();
                });

                it('Нет ссылки Документы по заказу', async () => {
                    const documentsLink = container.querySelector(ActionLinkPO.documentsLink);

                    expect(documentsLink).toBeFalsy();
                });

                it('Нет кнопки Повторить заказ', async () => {
                    const reorderButton = container.querySelector(ReorderButtonPO.root);

                    expect(reorderButton).toBeFalsy();
                });
            });
        });
    });
});
