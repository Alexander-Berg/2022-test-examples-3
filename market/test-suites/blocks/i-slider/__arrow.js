import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на элемент i-slider__arrow.
 * @param {PageObject.Slider} slider
 */

export default makeSuite('Кнопки слайдера.', {
    feature: 'Карусель',
    story: {
        'Кнопка "влево".': {
            'При клике на кнопку "вправо"': {
                'становится доступна': makeCase({
                    id: 'marketfront-887',
                    test() {
                        const slider = this.slider;

                        return slider
                            .isPrevDisabled()
                            .should.eventually.be.equal(true, 'Кнопка "влево" недоступна')
                            .then(() => slider.clickRight())
                            .then(() => slider.isPrevDisabled())
                            .should.eventually.be.equal(false, 'Кнопка "влево" доступна');
                    },
                }),
            },
        },
    },
});
