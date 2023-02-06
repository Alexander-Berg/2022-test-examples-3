import {makeSuite} from 'ginny';
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import FilterPrice from '@self/platform/spec/page-objects/FilterPrice';
import FilterCheckbox from '@self/platform/spec/page-objects/FilterCheckbox';
import FilterDeliveryType from '@self/platform/spec/page-objects/FilterDeliveryType';

/**
 * Тест на взаимодействие фильтров друг с другом.
 */
export default makeSuite('Взаимодействие фильтров друг с другом.', {
    environment: 'testing',
    story: {
        beforeEach() {
            this.setPageObjects({
                snippetList: () => this.createPageObject(SnippetList),
                filterPrice: () => this.createPageObject(FilterPrice),
                filterCheckbox: () => this.createPageObject(
                    FilterCheckbox,
                    {
                        root: '[data-autotest-id="manufacturer_warranty"]',
                    }
                ),
                filterDeliveryType: () => this.createPageObject(FilterDeliveryType),
            });
        },
    },
});
