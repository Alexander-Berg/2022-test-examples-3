import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок i-slider.
 * @param {PageObject.Slider} slider
 */

export default makeSuite('Слайдер.', {
    feature: 'Слайдер',
    story: {
        'Слайды.': {
            'При нажатии стрелок навигации': {
                'корректно прокручиваются': makeCase({
                    id: 'marketfront-886',
                    params: {
                        size: 'Количество выводимых элементов на слайдере',
                    },
                    test() {
                        const slider = this.slider;

                        return slider
                            .clickRight()
                            .then(() => slider.isItemDisplayed(this.params.size + 1))
                            .should.eventually.be.equal(true, 'Элемент за стрелкой стал виден')
                            .then(() => slider.isItemDisplayed(1, false))
                            .should.eventually.be.equal(false, 'Первый элемент не виден')
                            .then(() => slider.clickLeft())
                            .then(() => slider.isItemDisplayed(1))
                            .should.eventually.be.equal(true, 'Снова виден первый элемент');
                    },
                }),
            },
        },
    },
});
