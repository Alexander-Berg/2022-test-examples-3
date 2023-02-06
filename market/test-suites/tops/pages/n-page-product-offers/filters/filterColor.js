import {prepareSuite, makeSuite} from 'ginny';
import {
    createFilter,
    createFilterValue,
    createEntityFilter,
    createEntityFilterValue,
    createOffer,
    createProduct,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
// suites
import FilterListSuite from '@self/platform/spec/hermione/test-suites/blocks/FilterList';
// page-objects
import FilterColors from '@self/platform/spec/page-objects/FilterColors';
import SnippetList from '@self/platform/widgets/content/productOffers/Results/__pageObject';

import filterMock from './mocks/filter.mock';
import offersMocks from './mocks/offers.mock';
import productMock from './mocks/product.mock';


const createOfferState = (offerId, filterId, filterOptions, colorFilterIndex) => {
    const offer = createOffer(offersMocks[offerId], offerId);
    const offerFilter = createEntityFilter(filterOptions, 'offer', offerId, filterId);
    const {id: valueId, ...valueOptions} = filterMock.values[colorFilterIndex];
    const offerFilterValue = createEntityFilterValue(valueOptions, offerId, filterId, valueId);

    return mergeState([
        offer,
        offerFilter,
        offerFilterValue,
    ]);
};

export default makeSuite('Фильтр цветов', {
    environment: 'kadavr',
    story: prepareSuite(FilterListSuite, {
        meta: {
            id: 'marketfront-700',
            issue: 'MARKETVERSTKA-23955',
        },
        params: {
            queryParamName: 'glfilter',
        },
        hooks: {
            async beforeEach() {
                const {id: filterId, ...filterOptions} = filterMock.options;
                const {id: productId, ...productOptions} = productMock;
                const product = createProduct(productOptions, productId);
                const filter = createFilter(filterOptions, filterId);
                const filterValues = filterMock.values.map(value =>
                    createFilterValue(value, filterId, value.id));
                const offers = productMock.offers.items.map((offerId, index) =>
                    createOfferState(offerId, filterId, filterOptions, index));

                const state = mergeState([
                    product,
                    filter,
                    ...filterValues,
                    ...offers,
                    {
                        data: {
                            search: {
                                total: offers.length,
                            },
                        },
                    },
                ]);

                await this.browser.setState('report', state);

                return this.browser.yaOpenPage('market:product-offers', {
                    productId: productMock.id,
                    slug: productMock.slug,
                });
            },
        },
        pageObjects: {
            filterList() {
                return this.createPageObject(FilterColors);
            },
            snippetList() {
                return this.createPageObject(SnippetList);
            },
        },
    }),
});
