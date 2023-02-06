import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {
    createProduct,
    mergeState,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import DealsStickerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-deals-sticker';
// page-objects
import DealsSticker from '@self/platform/spec/page-objects/DealsSticker';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';

// mocks
import indexPageMock from '../fixtures/index-page';

const PRODUCT_ID = 1;
const OFFER_ID = 2;
const SLUG = 'telephone';
const PRODUCT = {
    slug: SLUG,
    type: 'model',
};
const OFFER = {
    entity: 'offer',
    benefit: {
        type: 'default',
    },
    showUid: OFFER_ID,
    promo: {
        type: 'cheapest-as-gift',
        itemsInfo: {
            count: 3,
        },
    },
    feeShow: 'qwerty',
};

export default makeSuite('ScrollBox N=N+1', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(DealsStickerSuite, {
            pageObjects: {
                dealsSticker() {
                    return this.createPageObject(DealsSticker, {
                        root: ScrollBox.snippet,
                    });
                },
            },
            meta: {
                id: 'marketfront-4253',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [indexPageMock]
                    );

                    const reportState = mergeState([
                        createProduct(PRODUCT, PRODUCT_ID),
                        createOfferForProduct(OFFER, PRODUCT_ID, OFFER_ID),
                        {
                            data: {
                                search: {
                                    total: 1,
                                },
                            },
                        },
                    ]);
                    await this.browser.setState('report', reportState);

                    await this.browser.yaOpenPage('market:index');
                },
            },
        })
    ),
});
