
import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {DELIVERY_CANCELLATION_REASON_MAP} from '@self/root/src/entities/courierTracking/constants';

export default makeSuite('Отмена доставки', {
    environment: 'kadavr',
    feature: 'Где мой курьер?',
    story: mergeSuites({
        'По нажатию на кнопку отмены доставки': {
            async beforeEach() {
                await this.bottomDrawer.swipeUp();

                await this.bottomDrawer.scrollDownContent();

                await this.deliveryInProgressView.clickOnCancelDeliveryButton();

                await this.orderCancellationDialog.waitForBecomeVisible(1000);
            },

            'должен появиться контент модалки отмены доставки': makeCase({
                id: 'bluemarket-3191',
                async test() {
                    await this.orderCancellationDialog.getVisibilityStatus()
                        .should.eventually.be.equal(
                            true,
                            'Контент модалки для отмены заказа должен быть виден'
                        );
                },
            }),

            'должны быть видны элементы контента': makeCase({
                id: 'bluemarket-3192',
                async test() {
                    await this.orderCancellationModal.isCloseModalButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка закрытия модалки для отмены доставки должна быть видна'
                        );

                    await this.orderCancellationDialog.isRadioButtonsMenuVisible()
                        .should.eventually.be.equal(
                            true,
                            'Меню выбора причины для отмены доставки должно быть видно'
                        );

                    await this.orderCancellationDialog.isSubmitButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка подтверждения отмены доставки должна быть видна'
                        );
                },
            }),

            'блок ввода комментария виден только если выбрана причина отмены "Другое"': makeCase({
                id: 'bluemarket-3183',
                async test() {
                    const statusesWithoutCommentTextField = [
                        DELIVERY_CANCELLATION_REASON_MAP.CHANGED_MIND,
                        DELIVERY_CANCELLATION_REASON_MAP.HAVE_ANOTHER_ONE,
                        DELIVERY_CANCELLATION_REASON_MAP.FOUND_CHEAPER,
                        DELIVERY_CANCELLATION_REASON_MAP.DELIVERY_NOT_ACCEPTABLE,
                    ];

                    const deliveryCancellationTextFieldExtistenceTestCases = statusesWithoutCommentTextField.map(
                        status => async () => {
                            await this.orderCancellationDialog.clickOnReasonRadioButtonByStatus(
                                status
                            );

                            await this.orderCancellationDialog.isCommentTextFieldExisting()
                                .should.eventually.be.equal(
                                    false,
                                    `Блок ввода комментария не должен рендерится при отмене по причине ${status}`
                                );
                        }
                    );

                    const testCasesPromise = () => deliveryCancellationTextFieldExtistenceTestCases.reduce(
                        (finalPromise, test) => finalPromise.then(test), Promise.resolve()
                    );
                    await testCasesPromise();

                    await this.orderCancellationDialog.clickOnReasonRadioButtonByStatus(
                        DELIVERY_CANCELLATION_REASON_MAP.OTHER
                    );

                    await this.orderCancellationDialog.isCommentTextFieldVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок ввода комментария должен быть виден'
                        );
                },
            }),

            'и без выбора причины отмены, кнопка подтверждения отмены доставки должна быть неактивна': makeCase({
                id: 'bluemarket-3193',
                async test() {
                    await this.orderCancellationDialog.isSubmitButtonDisabled()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка подтверждения отмены доставки должна быть неактивна'
                        );
                },
            }),

            'и при выборе причины отмены, и при подтверждении отмены доставки,': {
                async beforeEach() {
                    await this.orderCancellationDialog.clickOnReasonRadioButtonByStatus(
                        DELIVERY_CANCELLATION_REASON_MAP.CHANGED_MIND
                    );
                    await this.orderCancellationDialog.clickOnSubmitButton();
                    await this.orderCancellationDialog.waitForBecomeVisible(2000, true);
                },

                'должен отображаться экран-заглушка с информацией об отмене доставки': makeCase({
                    id: 'bluemarket-3194',
                    async test() {
                        await this.orderCancellationDialog.getVisibilityStatus()
                            .should.eventually.be.equal(
                                false,
                                'Контент модалки для отмены заказа не должен быть виден'
                            );

                        await this.courierTrackingPage.isDeliveryViewByStatusVisible('NOT_DELIVERED')
                            .should.eventually.be.equal(
                                true,
                                'Должен отображаться FailedDeliveryView, так как доставка отменена'
                            );
                    },
                }),
            },

            'и при закрытии модалки,': {
                async beforeEach() {
                    await this.orderCancellationModal.clickOnCloseModalButton();
                    await this.orderCancellationDialog.waitForBecomeVisible(1000, true);
                },

                'должен отображаться прежний экран с доставкой в процессе': makeCase({
                    id: 'bluemarket-3197',
                    async test() {
                        await this.orderCancellationDialog.getVisibilityStatus()
                            .should.eventually.be.equal(
                                false,
                                'Контент модалки для отмены заказа не должен быть виден'
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
