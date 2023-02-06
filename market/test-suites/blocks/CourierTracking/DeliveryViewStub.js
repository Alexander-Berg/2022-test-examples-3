import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';

export default makeSuite('Cтраница-заглушка', {
    feature: 'Где мой курьер?',
    params: {
        title: 'Название на странице-заглушке',
        description: 'Описание на странице-заглушке',
        courierTracking: 'Объект трекинга курьера, который приходит от API',
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
            async test() {
                await this.courierTrackingPage.getVisibilityStatus()
                    .should.eventually.be.equal(
                        true,
                        'Страница должна отображаться'
                    );
            },
        }),

        'Отображает правильный текст в названии': makeCase({
            async test() {
                const {title} = this.params;

                await this.deliveryStatusViewStub.getTitleText()
                    .should.eventually.be.equal(
                        title,
                        `Текст названия на странице должен быть равен "${title}"`
                    );
            },
        }),

        'Отображает правильный текст в описании': makeCase({
            async test() {
                const {description} = this.params;

                this.deliveryStatusViewStub.getDescriptionText()
                    .should.eventually.be.equal(
                        description,
                        `Текст описания на странице-заглушке должен быть равен "${description}"`
                    );
            },
        }),
    }),
});
