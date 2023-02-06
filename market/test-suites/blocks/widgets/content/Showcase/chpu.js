import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки на Showcase.
 * @param {PageObject.showcase} showcase
 */
export default makeSuite('ЧПУ ссылки в виджете "Популярные бренды".', {
    story: {
        'Сниппет бренда.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3067',
                    issue: 'MARKETVERSTKA-32871',
                    async test() {
                        const url = await this.showcase.getFirstItemUrl();

                        return this.expect(url).to.be.link({
                            pathname: 'brands--[\\w-]+/\\d+',
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
