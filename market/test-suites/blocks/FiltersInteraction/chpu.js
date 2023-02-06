import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на взаимодействие фильтров с деревом категорий.
 * @property {PageObject} searchIntent
 */
export default makeSuite('Ссылки в меню уточнения категорий.', {
    story: {
        'Ссылка на категорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3023',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        const url = await this.searchIntent.getSearchIntentLinkHref();

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
