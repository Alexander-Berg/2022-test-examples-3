import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок OutletBalloon.
 * @param {PageObject.OutletBalloon} outletBalloon
 */
export default makeSuite('Баллун с информацией о торговой точке. Блок с самовывозом.', {
    story: {
        'По умолчанию': {
            'должен быть виден': makeCase({
                feature: 'Карта',
                id: 'm-touch-3605',
                issue: 'MOBMARKET-12681',
                async test() {
                    await this.outletBalloon.scrollToBottom();

                    return this.expect(this.outletBalloon.isDeliveryVisible())
                        .to.equal(true, 'Блок с иформацией о доставке');
                },
            }),
        },
    },
});
