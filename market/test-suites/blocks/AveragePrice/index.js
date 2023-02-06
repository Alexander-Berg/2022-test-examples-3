import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на виджет средней цены
 * @param {PageObject.AveragePrice} averagePrice
 */
export default makeSuite('Виджет средней цены.', {
    feature: 'Подписка на снижение цены',
    story: {
        'Кнопка "Следить за снижением цены"': {
            'присутствует на странице.': makeCase({
                id: 'm-touch-2250',
                issue: 'MOBMARKET-8972',
                test() {
                    return this.averagePrice.subscribeButton.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кнопка "Следить за снижением цены" находится в блоке средней цены'
                        );
                },
            }),
        },
    },
});
