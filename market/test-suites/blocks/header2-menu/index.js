import {makeSuite, makeCase} from 'ginny';
import _ from 'lodash';

const getCompareCategory = id => ({
    categoryId: id,
    lastUpdate: 1481721770000,
    items: [
        {
            productId: 13012182,
            lastUpdate: 1481721770000,
        },
    ],
});

const getComparisonCategoriesByCount = count => _.times(count, getCompareCategory);

const reloadReactPage = browser => browser.yaPageReload(5000, ['state']);

/**
 * Тесты на блок header2-user-menu
 * @param {PageObject.Header2Menu} headerMenu
 */

export default makeSuite('Меню пользователя в шапке', {
    environment: 'kadavr',
    issue: 'MARKETVERSTKA-25318',
    id: 'marketfront-1586',
    story: {
        'Счетчик "Сравнение"': {
            'от 1 и более категорий': {
                async beforeEach() {
                    await this.browser.setState(
                        'persComparison',
                        {data: {comparisons: getComparisonCategoriesByCount(4)}}
                    );
                    await reloadReactPage(this.browser); // this.browser.yaPageReload();
                },
                async test() {
                    await this.headerMenu.waitForCompareItemVisible();
                    const count = await this.headerMenu.getCompareItemCount();
                    return this.expect(count).to.be.equal(4, 'отображается числом');
                },
            },
            'без добавленных ': {
                async beforeEach() {
                    await this.browser.setState(
                        'persComparison',
                        {data: {comparisons: []}}
                    );
                    await reloadReactPage(this.browser);
                },
                'категорий': makeCase({
                    test() {
                        return this.expect(this.headerMenu.isForCompareItemVisible())
                            .to.be.equal(false, 'счетчик не виден');
                    },
                }),
            },
        },
    },
});
