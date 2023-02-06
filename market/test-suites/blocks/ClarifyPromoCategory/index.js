import ClarifyPromoCategory from '@self/platform/spec/page-objects/ClarifyPromoCategory';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок ClarifyPromoCategory
 * @property {PageObject.ClarifyPromoCategory} clarifyPromoCategory
 * @property {PageObject.DealsHubFilters} dealsHubFilters
 */
export default makeSuite('Виджет "Уточните категорию" (промо).', {
    story: {
        'По умолчанию': {
            'не отображается': makeCase({
                issue: 'MOBMARKET-13217',
                id: 'm-touch-3012',
                async test() {
                    const isNotVisible = await this.browser.waitForVisible(ClarifyPromoCategory.root, 1000, true);

                    await this.expect(isNotVisible).to.equal(true, 'Виджет не отображается');
                },
            }),
        },
        'При смене департамента': {
            'отображает листовые категории': makeCase({
                issue: 'MOBMARKET-13217',
                id: 'm-touch-2971',
                async test() {
                    await this.dealsHubFilters.selectCategoryByIndex(1);

                    const isVisible = await this.browser.waitForVisible(ClarifyPromoCategory.root, 5000);

                    await this.expect(isVisible).to.equal(true, 'Виджет отображается');
                },
            }),
        },
    },
});
