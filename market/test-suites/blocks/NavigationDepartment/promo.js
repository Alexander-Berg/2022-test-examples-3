import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок NavigationDepartment департамента с промо (hasPromo: true).
 * @param {PageObject.NavigationDepartment} navigationDepartment
 */
export default makeSuite('Промо департамент навигационного дерева.', {
    feature: 'Навигационное дерево',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'заголовок является ссылкой на департамент': makeCase({
                id: 'marketfront-1232',
                issue: 'MARKETVERSTKA-25878',

                async test() {
                    const link = await this.navigationDepartment.getDepartmentTitleLink();
                    return this.expect(link).to.be.link({
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
});
