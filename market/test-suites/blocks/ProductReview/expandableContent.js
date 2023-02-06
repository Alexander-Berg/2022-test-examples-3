import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на разворачиваемый контент у отзыва.
 *
 * @param {PageObject.ReviewContent} reviewContent
 */
export default makeSuite('Блок с раскрывающимся контентом отзыва.', {
    params: {
        productId: 'ID продукта, который отображен в сниппете.',
        reviewId: 'ID отзыва.',
    },
    story: {
        'По умолчанию': {
            'контент должен быть свернут': makeCase({
                id: 'm-touch-1825',
                test() {
                    return this.reviewContent.isCollapsed()
                        .should.eventually.be.equal(true, 'Контент блока свернут');
                },
            }),
        },

        'Кнопка "Читать полностью".': {
            'При нажатии': {
                'должна раскрывать весь контент': makeCase({
                    id: 'm-touch-1843',
                    test() {
                        return this.reviewContent.clickExpanderControl()
                            .then(() => this.reviewContent.isExpanded())
                            .should.eventually.to.be.equal(true, 'Контент блока развернут');
                    },
                }),
                'должна менять свой текст на "Скрыть"': makeCase({
                    id: 'm-touch-1843',
                    test() {
                        return this.reviewContent.clickExpanderControl()
                            .then(() => this.reviewContent.getExpanderControlText())
                            .should.eventually.be.equal('Скрыть', 'Надпись на кнопке изменилась');
                    },
                }),
            },
        },

        'Кнопка "Скрыть".': {
            beforeEach() {
                // Предвариетльно готовим компонент к тестированию
                return this.reviewContent
                    // Раскрываем контент
                    .clickExpanderControl();
            },

            'При нажатии': {
                'должна сворачивать контент блока': makeCase({
                    id: 'm-touch-1844',
                    test() {
                        return this.reviewContent.clickExpanderControl()
                            .then(() => this.reviewContent.isCollapsed())
                            .should.eventually.be.equal(true, 'Контент блока свернут');
                    },
                }),
                'должна менять свой текст на "Читать полностью"': makeCase({
                    id: 'm-touch-1844',
                    test() {
                        return this.reviewContent.clickExpanderControl()
                            .then(() => this.reviewContent.getExpanderControlText())
                            .should.eventually.be.equal('Читать полностью', 'Кнопка "Читать полностью" отображается');
                    },
                }),
            },
        },
    },
});
