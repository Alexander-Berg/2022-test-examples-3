import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.AutomaticallyCalculatedDeliveryDisclaimer} disclaimer
 */
export default makeSuite('Авторасчёт доставки', {
    feature: 'Автоматический расчёт доставки',
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    const isVisible = await this.disclaimer.isExisting();

                    return this.expect(isVisible).to.be.equal(true, 'Дисклеймер присутствует на странице');
                },
            }),
        },
    },
});
