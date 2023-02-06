import {mergeSuites, makeSuite, makeCase} from 'ginny';
import FilterCounter from '@self/platform/spec/page-objects/FilterCounter';
import OldFilterPrice from '@self/platform/spec/page-objects/FilterPrice';
import FilterRange from '@self/root/src/components/Filter/FilterRange/__pageObject';

/**
 * Тесты на блок FilterCounter.
 * @param {PageObject.FilterCounter} FilterCounter
 * @param {PageObject.FilterColors} FilterColors
 */
export default makeSuite('FilterCounter', {
    feature: 'Попап «Найдено N моделей/предложений»',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    filterCounter: () => this.createPageObject(FilterCounter, {
                        parent: this.filtersAside,
                    }),
                    filterPrice: () => this.createPageObject(FilterRange, {
                        root: FilterRange.priceRoot,
                    }),
                });
            },
            'при изменении фильтра «Цена»': {
                'должен появляться попап': makeCase({
                    id: 'marketfront-614',
                    issue: 'MARKETVERSTKA-24663',
                    test() {
                        return this.filterPrice.setFilterRangeMinInputValue(1)
                            .then(() => this.filterCounter.waitForPopupIsVisible());
                    },
                }),
            },
        }
    ),
});


export const OldFilterCounter = makeSuite('FilterCounter (старые фильтры)', {
    feature: 'Попап «Найдено N моделей/предложений»',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    filterCounter: () => this.createPageObject(FilterCounter, {
                        parent: this.filtersAside,
                    }),
                    filterPrice: () => this.createPageObject(OldFilterPrice, {
                        parent: this.filtersAside,
                    }),
                });
            },
            'при изменении фильтра «Цена»': {
                'должен появляться попап': makeCase({
                    id: 'marketfront-614',
                    issue: 'MARKETVERSTKA-24663',
                    test() {
                        return this.filterPrice.setValue('from', 1).then(
                            () => this.filterCounter.waitForPopupIsVisible()
                        );
                    },
                }),
            },
        }
    ),
});
