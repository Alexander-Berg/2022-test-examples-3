import {makeCase, makeSuite, mergeSuites} from 'ginny';
import {checkDeliveryFeedbackStep, ACTIONS} from '@self/root/src/spec/hermione/scenarios/deliveryFeedback';
import DeliveryFeedbackPopup from '@self/root/src/widgets/content/DeliveryFeedbackPopup/components/Content/__pageObject';
import OrderPreviewCard from '@self/root/src/widgets/content/DeliveryFeedbackPopup/components/OrderPreviewCard/__pageObject';
import Image from '@self/root/src/uikit/components/Image/__pageObject';
import RatingControl from '@self/root/src/uikit/components/RatingControl/__pageObject';
import {validateOrderDataParams} from '../utils';

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
                    step: 'IS_DELIVERED_QUESTION',
                    action: ACTIONS.PRIMARY,
                    orderData: this.params.orderData,
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
                    step: 'USER_RECEIVED',
                    action: ACTIONS.GRADE,
                    orderData: this.params.orderData,
                    isPopup: true,
                });
            },
        }),
        'Сценарий "Еще не получил': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                        step: 'IS_DELIVERED_QUESTION',
                        action: ACTIONS.SECONDARY,
                        orderData: this.params.orderData,
                    });

                    await this.switchToPopup();
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
                                        isPopup: true,
                                    });
                                },
                            },
                            {
                                'Кнопка "Понятно"': makeCase({
                                    id: 'bluemarket-3722',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: 'NEW_DATE_RECEIVED',
                                            action: ACTIONS.PRIMARY,
                                            orderData: this.params.orderData,
                                            isPopup: true,
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
                                        isPopup: true,
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
                                            isPopup: true,
                                        });
                                    },
                                }),
                                'Кнопка "Поддержка"': makeCase({
                                    id: 'bluemarket-3723',
                                    issue: 'MARKETFRONT-16609',
                                    async test() {
                                        await this.browser.yaScenario(this, checkDeliveryFeedbackStep, {
                                            step: 'NEW_DATE_MISSED',
                                            action: ACTIONS.SECONDARY,
                                            orderData: this.params.orderData,
                                            isPopup: true,
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
