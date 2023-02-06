import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import DescribedProductsSuite from '@self/platform/spec/hermione/test-suites/blocks/w-described-products';
import DescribedProducts from '@self/platform/spec/page-objects/w-described-products';

/**
 * Тесты на блок w-collections.
 * @param {PageObject.Collections} collections
 */

export default makeSuite('Блок коллекций', {
    story: mergeSuites(
        prepareSuite(DescribedProductsSuite, {
            pageObjects: {
                describedProducts() {
                    return this.createPageObject(DescribedProducts, {
                        parent: this.collections,
                    });
                },
            },
        })
    ),
});
