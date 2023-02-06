import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок n-carousel.
 * @param {PageObject.Carousel} carousel
 */

export default makeSuite('Блок фильтрации карусели.', {
    feature: 'Структура страницы',
    defaultParams: {
        activeByDefault: 1,
        applyingFilterIndex: 2,
    },
    story: {
        'Кнопки фильтрации.': {
            'По-умолчанию': {
                'выбрана первая.': makeCase({
                    id: 'marketfront-911',
                    params: {
                        activeByDefault: 'Номер фильтра, активного по-умолчанию',
                    },
                    test() {
                        const {activeByDefault} = this.params;

                        return this.carousel
                            .getActiveCarouselItemIndex()
                            .should.eventually.be.equal(activeByDefault, 'Активный элемент карусели')
                            .then(() => this.carousel.getActiveFilterIndex())
                            .should.eventually.be.equal(activeByDefault, 'Активный фильтр');
                    },
                }),
            },

            'При нажатии': {
                'переключается содержимое': makeCase({
                    id: 'marketfront-912',
                    params: {
                        applyingFilterIndex: 'Номер фильтра, на который переключаем',
                    },
                    test() {
                        const {applyingFilterIndex} = this.params;

                        return this.carousel
                            .applyFilterByIndex(applyingFilterIndex)
                            .then(() => this.carousel.getActiveFilterIndex())
                            .should.eventually.be.equal(applyingFilterIndex, 'Активный фильтр')
                            .then(() => this.carousel.getActiveCarouselItemIndex())
                            .should.eventually.be.equal(applyingFilterIndex, 'Активный элемент карусели');
                    },
                }),
            },
        },
    },
});
