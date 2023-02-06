import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на юридический дисклеймер о покупке лекарственны средств.
 * @property {PageObject.ShopsInfo} shopsInfo
 */
export default makeSuite('Дисклеймер о покупке лекарственных средств', {
    feature: 'Здоровье на Маркете',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    const isVisible = await this.shopsInfo.drugsDisclaimer.isVisible();

                    return this.expect(isVisible).to.be.equal(true, 'Дисклеймер присутствует на странице');
                },
            }),
        },
    },
});
