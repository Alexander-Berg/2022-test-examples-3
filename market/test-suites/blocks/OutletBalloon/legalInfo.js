import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок OutletBalloon.
 * @param {PageObject.OutletBalloon} outletBalloon
 */
export default makeSuite('Баллун с информацией о торговой точке. Блок с Юридической информацией.', {
    story: {
        'По умолчанию': {
            'должен быть виден': makeCase({
                feature: 'Карта',
                id: 'm-touch-2765',
                issue: 'MOBMARKET-12057',
                async test() {
                    await this.outletBalloon.scrollToBottom();

                    return this.expect(this.outletBalloon.isLegalInfoVisible())
                        .to.equal(true, 'Блок с Юридической информацией должен быть виден');
                },
            }),
        },
    },
});
