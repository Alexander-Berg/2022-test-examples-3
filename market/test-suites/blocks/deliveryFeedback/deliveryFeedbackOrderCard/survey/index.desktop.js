import {makeCase, makeSuite, mergeSuites} from 'ginny';
import {checkDeliveryFeedbackStep, ACTIONS} from '@self/root/src/spec/hermione/scenarios/deliveryFeedback';
import {DELIVERY_FEEDBACK_STEPS} from '@self/root/src/entities/deliveryFeedback';
import {validateOrderDataParams} from '../../utils';

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
        'Сценарий "Заказ у меня".': makeCase({
            id: 'bluemarket-3719',
            issue: 'MARKETFRONT-16609',
            async test() {
                await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                    step: DELIVERY_FEEDBACK_STEPS.IS_DELIVERED_QUESTION,
                    action: ACTIONS.PRIMARY,
                });

                await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                    step: DELIVERY_FEEDBACK_STEPS.USER_RECEIVED,
                    action: ACTIONS.GRADE,
                });
            },
        }),
        'Сценарий "Еще не получил".': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                        step: DELIVERY_FEEDBACK_STEPS.IS_DELIVERED_QUESTION,
                        action: ACTIONS.SECONDARY,
                    });
                },
            },
            {
                'Вопрос про новую дату доставки.': mergeSuites(
                    {
                        'Если новая дата получена.': mergeSuites(
                            {
                                async beforeEach() {
                                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                        step: DELIVERY_FEEDBACK_STEPS.NEW_DATE_QUESTION,
                                        action: ACTIONS.PRIMARY,
                                    });
                                },
                            },
                            {
                                'Кнопка "Понятно" работает корректно.': makeCase({
                                    id: 'bluemarket-3722',
                                    issue: 'MARKETFRONT-16609',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: DELIVERY_FEEDBACK_STEPS.NEW_DATE_RECEIVED,
                                            action: ACTIONS.PRIMARY,
                                        });
                                    },
                                }),
                            }
                        ),
                        'Если новая дата не получена.': mergeSuites(
                            {
                                async beforeEach() {
                                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                        step: DELIVERY_FEEDBACK_STEPS.NEW_DATE_QUESTION,
                                        action: ACTIONS.SECONDARY,
                                    });
                                },
                            },
                            {
                                'Кнопка "Понятно" работает корректно.': makeCase({
                                    id: 'bluemarket-3723',
                                    issue: 'MARKETFRONT-16609',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: DELIVERY_FEEDBACK_STEPS.NEW_DATE_MISSED,
                                            action: ACTIONS.PRIMARY,
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
