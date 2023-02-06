import {makeCase, makeSuite, mergeSuites} from 'ginny';
import {checkDeliveryFeedbackStep, ACTIONS} from '@self/root/src/spec/hermione/scenarios/deliveryFeedback';
import {validateOrderDataParams} from '../utils';

module.exports = makeSuite('Прохождение опроса', {
    environment: 'kadavr',
    feature: 'Виджет "Заказ у меня"',
    params: {
        orderData: 'Параметры текущего заказа',
    },
    story: {
        async beforeEach() {
            validateOrderDataParams(this.params.orderData);
        },
        'Сценарий "Заказ у меня"': makeCase({
            id: 'bluemarket-3719',
            issue: 'MARKETFRONT-16609',
            async test() {
                await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                    step: 'IS_DELIVERED_QUESTION',
                    action: ACTIONS.PRIMARY,
                    orderData: this.params.orderData,
                });

                await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                    step: 'USER_RECEIVED',
                    action: ACTIONS.GRADE,
                    orderData: this.params.orderData,
                });
            },
        }),
        'Сценарий "Еще не получил"': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                        step: 'IS_DELIVERED_QUESTION',
                        action: ACTIONS.SECONDARY,
                        orderData: this.params.orderData,
                    });
                },
            },
            {
                'Вопрос про новую дату доставки': mergeSuites(
                    {
                        'Новая дата получена': mergeSuites(
                            {
                                async beforeEach() {
                                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                        step: 'NEW_DATE_QUESTION',
                                        action: ACTIONS.PRIMARY,
                                        orderData: this.params.orderData,
                                    });
                                },
                            },
                            {
                                'Кнопка "Понятно"': makeCase({
                                    id: 'bluemarket-3722',
                                    issue: 'MARKETFRONT-16609',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: 'NEW_DATE_RECEIVED',
                                            action: ACTIONS.PRIMARY,
                                            orderData: this.params.orderData,
                                        });
                                    },
                                }),
                            }
                        ),
                        'Новая дата не получена': mergeSuites(
                            {
                                async beforeEach() {
                                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                        step: 'NEW_DATE_QUESTION',
                                        action: ACTIONS.SECONDARY,
                                        orderData: this.params.orderData,
                                    });
                                },
                            },
                            {
                                'Кнопка "Понятно"': makeCase({
                                    id: 'bluemarket-3723',
                                    issue: 'MARKETFRONT-16609',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: 'NEW_DATE_MISSED',
                                            action: ACTIONS.PRIMARY,
                                            orderData: this.params.orderData,
                                        });
                                    },
                                }),
                                'Кнопка "Чат"': makeCase({
                                    id: 'bluemarket-3723',
                                    issue: 'MARKETFRONT-16609',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: 'NEW_DATE_MISSED',
                                            action: ACTIONS.SECONDARY,
                                            orderData: this.params.orderData,
                                        });
                                    },
                                }),
                            }
                        ),
                    }
                ),
            }
        ),
    },
});
