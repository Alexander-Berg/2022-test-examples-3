import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {checkDeliveryFeedbackStep, ACTIONS} from '@self/root/src/spec/hermione/scenarios/deliveryFeedback';
import orderConsultationsOrdersWidgetSuite
    from '@self/root/src/spec/hermione/test-suites/blocks/orderConsultations/orderConsultationsOrdersWidget';
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
                        'Новая дата не получена.': mergeSuites(
                            {
                                async beforeEach() {
                                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                        step: 'NEW_DATE_QUESTION',
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
                                'Кнопка "Чат" работает корректно.': mergeSuites(
                                    {
                                        'Текст кнопки корректный.': {
                                            id: 'bluemarket-3723',
                                            issue: 'MARKETFRONT-16609',
                                            async test() {
                                                await this.secondaryButton.getText()
                                                    .should.eventually.be.equal(
                                                        'Чат',
                                                        'Текст второстепенной кнопки должен соответствовать шагу'
                                                    );
                                            },
                                        },
                                    },
                                    prepareSuite(orderConsultationsOrdersWidgetSuite,
                                        {
                                            hooks: {
                                                async beforeEach() {
                                                    await this.setPageObjects({
                                                        orderConsultationButton: () => this.secondaryButton,
                                                    });
                                                },
                                            },
                                        }
                                    )
                                ),
                            }
                        ),
                    }
                ),
            }
        ),
    },
});
