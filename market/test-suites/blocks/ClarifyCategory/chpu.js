import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на ЧПУ блока ClarifyCategory.
 * @param {PageObject.ClarifyCategory} clarifyCategory
 */
export default makeSuite('Визуальный блок уточнения категорий', {
    environment: 'testing',
    story: {
        'Ссылка на категорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3024',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        const url = await this.clarifyCategory.getUrlCategoryByIndex(1);

                        return this.expect(url).to.be.link({
                            pathname: '/catalog--.*/\\d+/list',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});
