import {makeSuite, makeCase} from 'ginny';
import {all} from 'ambar';

/**
 * @param {PageObject.ProductTabs} tabs
 */
export default makeSuite('Вкладки', {
    feature: 'Вкладки',
    story: {
        'По умолчанию': {
            'выбрана вкладка «Описание»': makeCase({
                id: 'marketfront-3480',
                issue: 'MARKETVERSTKA-34562',
                async test() {
                    const isActive = await this.tabs.checkTabIsActive('offer');
                    return this.expect(isActive).to.be.equal(true, 'Выбрана вкладка «Описание»');
                },
            }),
            'содержит заданные вкладки': makeCase({
                id: 'marketfront-3479',
                issue: 'MARKETVERSTKA-34561',
                async test() {
                    const {expectedTabNames} = this.params;

                    const tabNamesPromise = expectedTabNames.map(name => this.tabs.checkTabVisibilityByName(name));
                    const hasAllRequiredTabs = Promise.all(tabNamesPromise).then(all(Boolean));

                    return this.expect(hasAllRequiredTabs).to.be.equal(true, 'Содержит заданные вкладки');
                },
            }),
        },
    },
});
