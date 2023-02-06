import {prepareSuite, makeSuite} from 'ginny';

import CollectionItemSuite from '@self/platform/spec/hermione/test-suites/blocks/b-collection-item';
import CollectionItem from '@self/platform/spec/page-objects/b-collection-item';

/**
 * Тесты на блок w-described-products.
 * @param {PageObject.DescribedProducts} describedProducts
 */
export default makeSuite('Блок список продуктов подборки', {
    story: prepareSuite(CollectionItemSuite, {
        pageObjects: {
            collectionItem() {
                return this.createPageObject(CollectionItem, {
                    parent: this.describedProducts,
                    root: CollectionItem.getItemByIndex(1),
                });
            },
        },
    }),
});
