import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';

// pageObjects
import MultiOrdersHead from '@self/root/src/widgets/parts/OrderConfirmation/components/MultiOrdersHead/__pageObject';
import OrderPoll from '@self/root/src/widgets/content/OrderPollPopup/__pageObject';
import Title from '@self/root/src/uikit/components/Title/__pageObject';
import OrderDetails from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderDetails/__pageObject';
import YandexPlusInfo from '@self/root/src/widgets/parts/OrderConfirmation/components/YandexPlusInfo/__pageObject';

import {generateCashbackDetailsCollectionsMock} from '@self/root/src/resolvers/checkout/mocks/resolveOrdersCashbackDetails';
// scenario
import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';
// constants
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';

import {SOLO_ORDER, MULTI_ORDER, UNPAID_SOLO_ORDER} from './fixtures';

const accessibilitySuite = makeSuite('Проверка доступности блока', {
    params: {
        title: 'Элемент <Title> который проверяем на доступность',
        expectedTitle: 'Ожидаемый текст у блока',
        expectedTag: 'Ожидаемый тег у блока',
    },
    story: {
        'текст блока содержит корректный текст': makeCase({
            async test() {
                await this[this.params.title].getTitle().should.eventually.to.be.equal(
                    this.params.expectedTitle,
                    `Текст заголовка должен содержать "${this.params.expectedTitle}"`
                );
            },
        }),
        'тег блока содержит ожидаемое значение': makeCase({
            async test() {
                await this[this.params.title].getTag().should.eventually.to.be.equal(
                    this.params.expectedTag,
                    `Тег заголовка должен быть "${this.params.expectedTag}"`
                );
            },
        }),
    },
});

export default makeSuite('Доступность', {
    issue: 'MARKETFRONT-58842',
    feature: 'Доступность. Спасибо за заказ',
    environment: 'kadavr',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                multiOrdersHead: () => this.createPageObject(MultiOrdersHead, {
                    parent: this.confirmationPage,
                }),
                orderStatusTitle: () => this.createPageObject(Title, {
                    parent: this.multiOrdersHead,
                }),
                orderPoll: () => this.createPageObject(OrderPoll, {
                    parent: this.thisOrderConfigrmation,
                }),
                orderPollLink: () => this.createPageObject(Title, {
                    parent: this.orderPoll,
                }),

                orderDetails: () => this.createPageObject(OrderDetails, {
                    parent: this.confirmationPage,
                }),
                orderTitle: () => this.createPageObject(Title, {
                    parent: this.orderDetails,
                }),

                yandexPlusInfo: () => this.createPageObject(YandexPlusInfo, {
                    parent: this.confirmationPage,
                }),
                yandexPlusInfoTitle: () => this.createPageObject(Title, {
                    parent: this.yandexPlusInfo,
                }),
            });
        },
        'заказ успешно оплачен': mergeSuites(
            {
                async beforeEach() {
                    const [{cashbackEmitInfo, orderId}] = SOLO_ORDER;

                    await this.browser.yaScenario(
                        this,
                        prepareThankPage,
                        {
                            region: 213,
                            orders: SOLO_ORDER,
                            paymentOptions: {
                                paymentType: PAYMENT_TYPE.PREPAID,
                                paymentMethod: PAYMENT_METHOD.YANDEX,
                                status: ORDER_STATUS.PENDING,
                            },
                        },
                        generateCashbackDetailsCollectionsMock({
                            totalAmount: cashbackEmitInfo.totalAmount,
                            orders: [{cashbackAmount: cashbackEmitInfo.totalAmount, orderId}],
                        })
                    );
                },
            },
            prepareSuite(accessibilitySuite, {
                suiteName: 'заголовок содержит корректную информацию.',
                meta: {
                    id: 'tbd',
                },
                params: {
                    title: 'orderStatusTitle',
                    expectedTitle: 'Заказ оформлен!',
                    expectedTag: 'h1',
                },
            }),
            prepareSuite(accessibilitySuite, {
                suiteName: 'ссылка на попап оценки заказа содержит доступную информацию',
                meta: {
                    id: 'tbd',
                },
                params: {
                    title: 'orderPollLink',
                    expectedTitle: 'Оценить, как прошёл заказ',
                    expectedTag: 'h2',
                },
            }),
            prepareSuite(accessibilitySuite, {
                suiteName: 'заголовок блока с составом заказа содержит доступную информацию',
                meta: {
                    id: 'tbd',
                },
                params: {
                    title: 'orderTitle',
                    expectedTitle: 'Состав заказа',
                    expectedTag: 'h2',
                },
            }),
            prepareSuite(accessibilitySuite, {
                suiteName: 'заголовок блока с информацией о баллах плюса содержит доступную информацию',
                meta: {
                    id: 'tbd',
                },
                params: {
                    title: 'yandexPlusInfoTitle',
                    expectedTitle: 'Баллы придут с заказом',
                    expectedTag: process.env.PLATFORM === 'touch' ? 'div' : 'h2',
                },
            })
        ),
        'мультизаказ оплачен': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareThankPage, {
                        region: 213,
                        orders: MULTI_ORDER,
                        paymentOptions: {
                            paymentType: PAYMENT_TYPE.PREPAID,
                            paymentMethod: PAYMENT_METHOD.YANDEX,
                            status: ORDER_STATUS.PENDING,
                        },
                    });
                },
            },
            prepareSuite(accessibilitySuite, {
                suiteName: 'заголовок содержит корректную информацию.',
                meta: {
                    id: 'tbd',
                },
                params: {
                    title: 'orderStatusTitle',
                    expectedTitle: 'Все посылки оформлены!',
                    expectedTag: 'h1',
                },
            })
        ),
        'заказ не оплачен': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareThankPage, {
                        region: 213,
                        orders: UNPAID_SOLO_ORDER,
                        paymentOptions: {
                            paymentType: PAYMENT_TYPE.PREPAID,
                            paymentMethod: PAYMENT_METHOD.YANDEX,
                            status: ORDER_STATUS.UNPAID,
                        },
                    });
                },
            },
            prepareSuite(accessibilitySuite, {
                suiteName: 'заголовок содержит корректную информацию.',
                meta: {
                    id: 'tbd',
                },
                params: {
                    title: 'orderStatusTitle',
                    expectedTitle: 'Заказ не оплачен',
                    expectedTag: 'h1',
                },
            })
        ),
    },
});
