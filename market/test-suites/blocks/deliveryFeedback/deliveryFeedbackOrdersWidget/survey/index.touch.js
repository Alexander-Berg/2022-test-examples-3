import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {checkDeliveryFeedbackStep, ACTIONS} from '@self/root/src/spec/hermione/scenarios/deliveryFeedback';
import DeliveryFeedbackPopup from '@self/root/src/widgets/content/DeliveryFeedbackPopup/components/Content/__pageObject';
import OrderPreviewCard from '@self/root/src/widgets/content/DeliveryFeedbackPopup/components/OrderPreviewCard/__pageObject';
import Image from '@self/root/src/uikit/components/Image/__pageObject';
import RatingControl from '@self/root/src/uikit/components/RatingControl/__pageObject';
import {DELIVERY_FEEDBACK_STEPS} from '@self/root/src/entities/deliveryFeedback';
import orderConsultationsOrdersWidgetSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderConsultations/orderConsultationsOrdersWidget';
import {validateOrderDataParams} from '../../utils';

module.exports = makeSuite('Прохождение опроса.', {
    environment: 'kadavr',
    feature: 'Виджет "Заказ у меня"',
    params: {
        orderData: 'Параметры текущего заказа',
    },
    story: {
        async beforeEach() {
            validateOrderDataParams(this.params.orderData);

            this.setPageObjects({
                popup: () => this.createPageObject(DeliveryFeedbackPopup),
                orderPreviewCard: () => this.createPageObject(OrderPreviewCard, {root: DeliveryFeedbackPopup.orderPreviewCard}),
                orderPreviewCardImage: () => this.createPageObject(Image, {root: OrderPreviewCard.image}),
            });

            this.switchToPopup = async () => {
                await this.popup.waitForVisible(3000);

                this.setPageObjects({
                    ratingControl: () => this.createPageObject(RatingControl, {root: DeliveryFeedbackPopup.ratingControl}),
                    primaryButton: () => this.popup.primaryButton,
                    secondaryButton: () => this.popup.secondaryButton,
                    title: () => this.popup.title,
                    description: () => this.popup.description,
                });
            };
        },
        'Сценарий "Заказ у меня"': makeCase({
            id: 'bluemarket-3719',
            issue: 'MARKETFRONT-16609',
            async test() {
                await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                    step: DELIVERY_FEEDBACK_STEPS.IS_DELIVERED_QUESTION,
                    action: ACTIONS.PRIMARY,
                });

                await this.switchToPopup();

                await this.orderPreviewCard.isVisible()
                    .should.eventually.be.equal(
                        true,
                        'Превью заказа отображается'
                    );

                await this.orderPreviewCard.name.isVisible()
                    .should.eventually.be.equal(
                        true,
                        'Название айтема заказа отображается'
                    );

                await this.orderPreviewCardImage.getSrc()
                    .should.eventually.match(
                        new RegExp(`${this.params.orderData.orderItemImage.replace('.', '\\.')}`),
                        'Картинка айтема заказа отображается'
                    );

                await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                    step: DELIVERY_FEEDBACK_STEPS.USER_RECEIVED,
                    action: ACTIONS.GRADE,
                });
            },
        }),
        'Сценарий "Еще не получил': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                        step: DELIVERY_FEEDBACK_STEPS.IS_DELIVERED_QUESTION,
                        action: ACTIONS.SECONDARY,
                    });

                    await this.switchToPopup();
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
                                'Кнопка "Чат с продавцом" работает корректно.': mergeSuites(
                                    {
                                        'Текст кнопки корректный.': {
                                            id: 'bluemarket-3723',
                                            issue: 'MARKETFRONT-16609',
                                            async test() {
                                                await this.secondaryButton.getText()
                                                    .should.eventually.be.equal(
                                                        'Чат с продавцом',
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
