import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты блока n-snippet-card2
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Листовой сниппета продукта.', {
    feature: 'SEO',
    environment: 'kadavr',
    story: {
        'Атрибут title заголовка сниппета.': {
            'По умолчанию': {
                'должен совпадать с названием товара': makeCase({
                    id: 'marketfront-2252',
                    issue: 'MARKETVERSTKA-25502',
                    test() {
                        const {snippetCard2} = this;

                        return Promise.all([
                            snippetCard2.getTitle(),
                            snippetCard2.getTitleAttributeForTitleLink(),
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
        'Аттрибут title главного изображения.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-1255',
                    issue: 'MARKETVERSTKA-25502',
                    test() {
                        const {snippetCard2} = this;

                        return Promise.all([
                            snippetCard2.getTitle(),
                            snippetCard2.getTitleAttributeForImage(),
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
        'Аттрибут alt главного изображения.': {
            'По умолчанию': {
                'совпадает с названием товара': makeCase({
                    id: 'marketfront-2251',
                    issue: 'MARKETVERSTKA-25502',
                    test() {
                        const {snippetCard2} = this;

                        return Promise.all([
                            snippetCard2.getTitle(),
                            snippetCard2.getAltAttributeForImage(),
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
