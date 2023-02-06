import {mergeSuites, makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок PopularBrandsCarousel.
 * @param {PageObject.PopularBrandsCarousel} popularBrandsCarousel
 * @param {PageObject.ScrollBox} scrollBox
 */

export default makeSuite('Карусель.', {
    feature: 'Структура страницы',
    environment: 'kadavr',
    story: mergeSuites(
        {
            'Заголовок.': {
                'По-умолчанию': {
                    'содержит ожидаемый текст': makeCase({
                        id: 'marketfront-910',
                        params: {
                            title: 'Текст, ожидаемый в заголовке карусели',
                        },
                        test() {
                            return this.popularBrandsCarousel.title.getText().then(text => text.trim())
                                .should.eventually.be.equal(this.params.title, 'Заголовок совпадает с ожидаемым');
                        },
                    }),
                },
            },
            'По-умолчанию': {
                'содержит корректные ссылки': makeCase({
                    id: 'marketfront-913',
                    params: {
                        url: 'URL элемента карусели изображений',
                    },
                    test() {
                        return this.browser.allure.runStep(
                            'Проверяем ссылку с карусели изображений',
                            () => this.scrollBox.getItemUrlByIndex(1)
                                .should.eventually.be.link(this.params.url, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                }));
                    },
                }),

                'содержит слайды с изображениями': makeCase({
                    id: 'marketfront-914',
                    test() {
                        return this.popularBrandsCarousel.waitForImagesVisible()
                            .should.eventually.be.equal(true, 'Изображения появились');
                    },
                }),
            },
        }
    ),
});
