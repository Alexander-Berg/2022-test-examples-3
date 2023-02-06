import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';

export default makeSuite('Перенос заказа по просьбе клиента', {
    environment: 'kadavr',
    feature: 'Где мой курьер?',
    params: {
        title: 'Название на странице-заглушке',
        description: 'Описание на странице-заглушке',
        courierTracking: 'Объект трекинга курьера, который приходит от API',
        popupTitle: 'Заголовок дровера подтверждения согласования переноса заказа',
        thanksTitle: 'Заголовок дровера при подтверждении переноса',
        apologizeTitle: 'Заголовок дровера при опровержении переноса',
    },
    story: mergeSuites({
        async beforeEach() {
            const {courierTracking} = this.params;
            const {id: trackingId} = courierTracking;

            await this.browser.setState('marketLogistics.collections.courierTracking', {
                [trackingId]: courierTracking,
            });

            await this.browser.yaOpenPage(PAGE_IDS_TOUCH.COURIER_TRACKING, {trackingId});
        },

        'Отображается': makeCase({
            id: 'bluemarket-4064',
            async test() {
                await this.courierTrackingPage.getVisibilityStatus()
                    .should.eventually.be.equal(
                        true,
                        'Страница должна отображаться'
                    );
            },
        }),

        'Отрисовывает нужные элементы ': makeCase({
            id: 'bluemarket-4064',
            async test() {
                const {popupTitle} = this.params;

                await this.rescheduleConfirmationDrawer.isPopupActionsVisible()
                    .should.eventually.be.equal(
                        true,
                        'Страница должна содержать блок с кнопками в дровере подтверждения доставки'
                    );

                await this.rescheduleConfirmationDrawer.getPopupHeaderText()
                    .should.eventually.be.equal(
                        popupTitle,
                        `Текст заголовка дровера должен быть равен "${popupTitle}"`
                    );

                await this.rescheduleConfirmationDrawer.isConfirmButtonEnabled()
                    .should.eventually.be.equal(
                        true,
                        'Кнопка подтверждения переноса заказа должна быть доступна'
                    );

                await this.rescheduleConfirmationDrawer.isDeclineButtonEnabled()
                    .should.eventually.be.equal(
                        true,
                        'Кнопка опровержения переноса заказа должна быть доступна'
                    );
            },
        }),

        'При подтверждении согласования переноса': {
            async beforeEach() {
                await this.rescheduleConfirmationDrawer.clickConfirmButton();
                await this.rescheduleConfirmationDrawer.waitForActionsInvisible(3000);
            },

            'должен измениться заголовок': makeCase({
                id: 'bluemarket-4065',
                async test() {
                    const {thanksTitle} = this.params;

                    await this.rescheduleConfirmationDrawer.getPopupSubHeaderText()
                        .should.eventually.be.equal(
                            thanksTitle,
                            `Текст заголовка дровера должен быть равен "${thanksTitle}"`
                        );
                },
            }),
        },

        'При опровержении согласования переноса': {
            async beforeEach() {
                await this.rescheduleConfirmationDrawer.clickDeclineButton();
                await this.rescheduleConfirmationDrawer.waitForActionsInvisible(3000);
            },

            'должен измениться заголовок': makeCase({
                id: 'bluemarket-4066',
                async test() {
                    const {apologizeTitle} = this.params;

                    await this.rescheduleConfirmationDrawer.getPopupSubHeaderText()
                        .should.eventually.be.equal(
                            apologizeTitle,
                            `Текст заголовка дровера должен быть равен "${apologizeTitle}"`
                        );
                },
            }),
        },

    }),
});
