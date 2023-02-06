import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок scrollbox.
 * @param {PageObject.ScrollBox} scrollbox
 */

export default makeSuite('Карусель похожих брендов.', {
    story: {
        'Ссылка на бренд.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3067',
                    issue: 'MARKETVERSTKA-32871',
                    async test() {
                        const url = await this.scrollbox.getItemUrlByIndex(1);

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
