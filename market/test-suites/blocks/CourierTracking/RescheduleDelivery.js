
import {makeCase, makeSuite, mergeSuites} from 'ginny';

export default makeSuite('Перенос доставки', {
    environment: 'kadavr',
    feature: 'Где мой курьер?',
    story: mergeSuites({
        'По нажатию на кнопку переноса доставки': {
            async beforeEach() {
                await this.bottomDrawer.swipeUp();

                await this.bottomDrawer.scrollDownContent();

                await this.deliveryInProgressView.clickOnRescheduleDeliveryButton();

                await this.orderRescheduleDialog.waitForBecomeVisible(3000);
            },

            'должен появиться контент модалки переноса доставки': makeCase({
                id: 'bluemarket-3198',
                async test() {
                    await this.orderRescheduleDialog.getVisibilityStatus()
                        .should.eventually.be.equal(
                            true,
                            'Контент модалки для переноса заказа должен быть виден'
                        );
                },
            }),

            'должны быть видны элементы контента': makeCase({
                id: 'bluemarket-3199',
                async test() {
                    await this.orderRescheduleDialog.isSubmitButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка подтверждения переноса сроков доставки должна быть видна'
                        );

                    await this.orderRescheduleDialog.isCancelButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка подтверждения отмены сроков доставки должна быть видна'
                        );

                    await this.orderRescheduleDialog.isDayDropdownVisible()
                        .should.eventually.be.equal(
                            true,
                            'Выпадающий список для выбора дня переноса доставки должен быть виден'
                        );

                    await this.orderRescheduleDialog.isTimeDropdownVisible()
                        .should.eventually.be.equal(
                            true,
                            'Выпадающий список для выбора времени переноса доставки должен быть виден'
                        );

                    await this.orderRescheduleModal.isCloseModalButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка закрытия модалки для переноса доставки должна быть видна'
                        );
                },
            }),


            'и при подтверждении переноса доставки,': {
                async beforeEach() {
                    await this.orderRescheduleDialog.clickOnSubmitButton();
                    await this.orderRescheduleDialog.waitForBecomeVisible(2000, true);
                },

                'должен отображаться экран-заглушка с информацией о переносе доставки': makeCase({
                    id: 'bluemarket-3203',
                    async test() {
                        await this.orderRescheduleDialog.getVisibilityStatus()
                            .should.eventually.be.equal(
                                false,
                                'Контент модалки для переноса заказа не должен быть виден'
                            );

                        await this.courierTrackingPage.isDeliveryViewByStatusVisible('RESCHEDULED')
                            .should.eventually.be.equal(
                                true,
                                'Должен отображаться DeliveryRescheduledView, так как доставка перенесена'
                            );
                    },
                }),
            },

            'и при отмене переноса доставки,': {
                async beforeEach() {
                    await this.orderRescheduleDialog.clickOnCancelButton();
                    await this.orderRescheduleDialog.waitForBecomeVisible(1000, true);
                },

                'должен отображаться прежний экран с доставкой в процессе': makeCase({
                    id: 'bluemarket-3200',
                    async test() {
                        await this.orderRescheduleDialog.getVisibilityStatus()
                            .should.eventually.be.equal(
                                false,
                                'Контент модалки для переноса заказа не должен быть виден'
                            );

                        await this.courierTrackingPage.isDeliveryViewByStatusVisible('IN_PROGRESS')
                            .should.eventually.be.equal(
                                true,
                                'Должен отображаться DeliveryInProgressView, так как доставка не была перенесена'
                            );
                    },
                }),
            },

            'и при закрытии модалки,': {
                async beforeEach() {
                    await this.orderRescheduleModal.clickOnCloseModalButton();
                    await this.orderRescheduleDialog.waitForBecomeVisible(1000, true);
                },

                'должен отображаться прежний экран с доставкой в процессе': makeCase({
                    id: 'bluemarket-3201',
                    async test() {
                        await this.orderRescheduleDialog.getVisibilityStatus()
                            .should.eventually.be.equal(
                                false,
                                'Контент модалки для переноса заказа не должен быть виден'
                            );

                        await this.courierTrackingPage.isDeliveryViewByStatusVisible('IN_PROGRESS')
                            .should.eventually.be.equal(
                                true,
                                'Должен отображаться DeliveryInProgressView, так как доставка не была перенесена'
                            );
                    },
                }),
            },
        },
    }),
});
