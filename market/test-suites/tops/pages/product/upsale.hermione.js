import {makeSuite, prepareSuite, mergeSuites} from '@yandex-market/ginny';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

// suites
import UpsaleSuite from '@self/platform/spec/hermione2/test-suites/blocks/Upsale';
// mocks
import upsalePopupCmsMarkup from '@self/root/src/spec/hermione/kadavr-mock/tarantino/upsale/popupContent';
// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import CartPopup from '@self/platform/widgets/content/CartPopup/__pageObject';
// constants
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {createOffers, phoneProductWithCPADefaultOffer, phoneProductRoute} from './fixtures/product';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Карточка товара', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-85112',
    story: mergeSuites(
        /**
         * @expFlag touch_km_upsale_popup
         * @ticket MARKETFRONT-82563
         * @start
         */
        prepareSuite(UpsaleSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Tarantino.data.result', [upsalePopupCmsMarkup]);

                    await this.browser.setState('report', mergeReportState([
                        phoneProductWithCPADefaultOffer,
                        createOffers({count: 12}),
                    ]));

                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, phoneProductRoute);
                },
            },
            pageObjects: {
                defaultOffer() {
                    return this.browser.createPageObject(DefaultOffer);
                },
                cartPopup() {
                    return this.browser.createPageObject(CartPopup);
                },
            },
        })
        /**
         * @expFlag touch_km_upsale_popup
         * @end
         */
    ),
});
