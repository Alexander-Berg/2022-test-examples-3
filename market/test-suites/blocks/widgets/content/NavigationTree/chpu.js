import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки в боковом меню.
 * @param {PageObject.navigationTree} navigationTree
 */
export default makeSuite('ЧПУ ссылки в боковом меню.', {
    story: {
        'Ссылка на категорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2991',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        const url = await this.navigationDepartment.getDepartmentTitleLink();

                        return this.expect(url).to.be.link({
                            pathname: '/catalog--.*/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
        'Ссылка на подкатегорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2991',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        const url = await this.navigationDepartment.getSubItemTitleLink(1);

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
