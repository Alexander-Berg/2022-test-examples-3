import {makeCase, makeSuite, mergeSuites} from 'ginny';

export default makeSuite('Основной экран (доставка в процессе)', {
    feature: 'Где мой курьер?',
    story: mergeSuites({
        'Отображается': makeCase({
            id: 'bluemarket-3176',
            async test() {
                await this.courierTrackingPage.getVisibilityStatus()
                    .should.eventually.be.equal(
                        true,
                        'Страница должна отображаться'
                    );
            },
        }),

        'Отрисовывает элементы': makeCase({
            id: 'bluemarket-3178',
            async test() {
                await this.bottomDrawer.getVisibilityStatus()
                    .should.eventually.be.equal(
                        true,
                        'Страница должна отображать Bottom Drawer'
                    );

                await this.courierTrackingMap.getVisibilityStatus()
                    .should.eventually.be.equal(
                        true,
                        'Страница должна отображать карту'
                    );

                await this.deliveryInfo.getVisibilityStatus()
                    .should.eventually.be.equal(
                        true,
                        'Информация о доставке должна быть видна при свернутом Bottom Drawer'
                    );
            },
        }),

        'По умолчанию': {
            'контент модалок не виден': makeCase({
                id: 'bluemarket-3180',
                async test() {
                    await this.orderRescheduleDialog.getVisibilityStatus()
                        .should.eventually.be.equal(
                            false,
                            'Контент модалки для переноса заказа не должен быть виден'
                        );

                    await this.orderCancellationDialog.getVisibilityStatus()
                        .should.eventually.be.equal(
                            false,
                            'Контент модалки для отмены заказа не должен быть виден'
                        );
                },
            }),

            'содержимое Bottom Drawer скрыто': makeCase({
                id: 'bluemarket-3179',
                async test() {
                    // eslint-disable-next-line no-unreachable
                    await this.orderCardList.getVisibilityStatus()
                        .should.eventually.be.equal(
                            false,
                            'Карточки заказов должны быть скрыты при свернутом Bottom Drawer'
                        );

                    // eslint-disable-next-line no-unreachable
                    await this.deliveryInProgressView.isButtonMenuVisible()
                        .should.eventually.be.equal(
                            false,
                            'Меню кнопок должно быть скрыто при свернутом Bottom Drawer'
                        );
                },
            }),
        },

        'При открытом Bottom Drawer': {
            async beforeEach() {
                await this.bottomDrawer.swipeUp();
            },

            'видно его содержимое': makeCase({
                id: 'bluemarket-3181',
                async test() {
                    await this.deliveryInfo.getVisibilityStatus()
                        .should.eventually.be.equal(
                            true,
                            'Информация о доставке должна быть видна при развернутом Bottom Drawer'
                        );

                    await this.orderCardList.getVisibilityStatus()
                        .should.eventually.be.equal(
                            true,
                            'Карточки заказов должны быть видны при развернутом Bottom Drawer'
                        );
                },
            }),

            'и скролле вниз, должны быть видны все кнопки': makeCase({
                id: 'bluemarket-3182',
                async test() {
                    await this.bottomDrawer.scrollDownContent();

                    await this.deliveryInProgressView.isButtonMenuVisible()
                        .should.eventually.be.equal(
                            true,
                            'Меню кнопок должно быть видимо при развернутом Bottom Drawer'
                        );

                    await this.deliveryInProgressView.isRescheduleDeliveryButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка переноса доставки должна быть видима'
                        );

                    await this.deliveryInProgressView.isCancelDeliveryButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка отмены заказа должна быть видима'
                        );

                    await this.deliveryInProgressView.isCallSupportButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка звонка в поддержку должна быть видима'
                        );
                },
            }),
        },
    }),
});
