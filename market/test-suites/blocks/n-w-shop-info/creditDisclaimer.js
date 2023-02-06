import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на юридический дисклеймер покупки в кредит.
 * @property {PageObject.ShopsInfo} shopsInfo
 */
export default makeSuite('Дисклеймер с информацией о покупке в кредит', {
    feature: 'Кредиты на Маркете',
    environment: 'kadavr',
    story: {
        'По умолчанию присутствует на странице': makeCase({
            async test() {
                const isVisible = await this.shopsInfo.creditDisclaimer.isVisible();

                return this.expect(isVisible).to.be.equal(true, 'Дисклеймер присутствует на странице');
            },
        }),
    },
});
