import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на seo, блока n-snippet-cell2
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Гридовый сниппет продукта.', {
    feature: 'SEO',
    environment: 'kadavr',
    story: {
        'Атрибут "title" заголовка сниппета.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-2252',
                    issue: 'MARKETVERSTKA-25502',
                    test() {
                        const {snippetCell2} = this;

                        return Promise.all([
                            snippetCell2.getTitle(),
                            snippetCell2.getTitleAttributeForTitleLink(),
                        ])
                            .then(([title, titleAttribute]) => this.expect(titleAttribute)
                                .be.equal(
                                    title,
                                    `Атрибут title совпадает с названием ${title}`
                                )
                            );
                    },
                }),
            },
        },
        'Атрибут "title" главного изображения.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-1255',
                    issue: 'MARKETVERSTKA-25502',
                    test() {
                        const {snippetCell2} = this;

                        return Promise.all([
                            snippetCell2.getTitle(),
                            snippetCell2.getTitleAttributeForImage(),
                        ])
                            .then(([title, titleAttribute]) => this.expect(titleAttribute)
                                .be.equal(
                                    title,
                                    `Атрибут title изображения совпадает с названием ${title}`
                                )
                            );
                    },
                }),
            },
        },
        'Атрибут "alt" главного изображения.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-2251',
                    issue: 'MARKETVERSTKA-25502',
                    test() {
                        const {snippetCell2} = this;

                        return Promise.all([
                            snippetCell2.getTitle(),
                            snippetCell2.getAltAttributeForImage(),
                        ])
                            .then(([title, altAttribute]) => this.expect(altAttribute)
                                .be.equal(
                                    title,
                                    `Атрибут alt изображения совпадает с названием ${title}`
                                )
                            );
                    },
                }),
            },
        },
    },
});
