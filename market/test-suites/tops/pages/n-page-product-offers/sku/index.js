import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
// suites
import ProductOffersWithSkuSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersFiltersSkuSuite/productOffersWithSkuSuite';
import ProductOffersWithoutSkuSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersFiltersSkuSuite/productOffersWithoutSkuSuite.js';
import ColorPicker from '@self/platform/components/Filter/ColorPicker/__pageObject';

import SnippetList from '@self/platform/widgets/content/productOffers/Results/__pageObject';
import Price from '@self/platform/components/Price/__pageObject';
import Title from '@self/platform/components/PageCardTitle/Title/__pageObject';
import MiniCard from '@self/platform/components/PageCardTitle/MiniCard/__pageObject';
import MoreOffersLink from '@self/platform/components/MoreOffersLink/__pageObject';
import FilterRadio from '@self/platform/spec/page-objects/FilterRadio';
import productWithSkuFiltersFixture from '../../n-page-product/fixtures/productWithSkuFilters';


const suiteOptions = {
    params: {
        skuState: productWithSkuFiltersFixture.skuState,
        skuTitle: productWithSkuFiltersFixture.SKU_TITLE,
        initialPageUrl: productWithSkuFiltersFixture.route,
        skuYellowId: productWithSkuFiltersFixture.skuYellowId,
        enumFilterId: productWithSkuFiltersFixture.ENUM_FILTER_ID,
    },
    hooks: {
        async beforeEach() {
            await this.browser.setState('report', productWithSkuFiltersFixture.initState);
            return this.browser.yaOpenPage('market:product-offers', productWithSkuFiltersFixture.route);
        },
        async afterEach() {
            return this.browser.yaLogout();
        },
    },
    pageObjects: {
        filterColors() {
            return this.createPageObject(ColorPicker);
        },
        snippetList() {
            return this.createPageObject(SnippetList, {
                root: '[data-zone-name="snippetList"]',
            });
        },
        price() {
            return this.createPageObject(Price);
        },
        title() {
            return this.createPageObject(Title);
        },
        miniCard() {
            return this.createPageObject(MiniCard);
        },
        moreOffersLink() {
            return this.createPageObject(MoreOffersLink);
        },
        filterRadio() {
            return this.createPageObject(FilterRadio);
        },
    },
};

export default makeSuite('SKU', {
    environment: 'kadavr',
    feature: 'Фильтры',
    story: mergeSuites(
        prepareSuite(ProductOffersWithSkuSuite, suiteOptions),
        prepareSuite(ProductOffersWithoutSkuSuite, suiteOptions)
    ),
});
