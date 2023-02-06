import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок OutletBalloon.
 * @param {PageObject.OutletBalloon} outletBalloon
 */
export default makeSuite('Баллун с информацией о торговой точке. Блок с Юридической информацией о доставке.', {
    story: {
        'По умолчанию': {
            'должен быть виден': makeCase({
                feature: 'Карта',
                id: 'm-touch-3603',
                issue: 'MOBMARKET-12681',
                async test() {
                    await this.outletBalloon.scrollToBottom();

                    return this.expect(this.outletBalloon.isDeliveryLegalInfoVisible())
                        .to.equal(true, 'Блок с Юридической информацией о доставке должен быть виден');
                },
            }),
        },
    },
});
