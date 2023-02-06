import {mergeSuites, makeSuite, prepareSuite} from 'ginny';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';
import {
    productWithExpressOfferState,
    modelPageRoute,
} from '@self/project/src/spec/hermione/fixtures/express';

// suites
import DefaultOfferSuite
    from '@self/root/src/spec/hermione/test-suites/blocks/express/defaultOffer';

// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';

export default makeSuite('ДО', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', productWithExpressOfferState);
                return this.browser.yaOpenPage(
                    PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                    modelPageRoute
                );
            },
        },
        prepareSuite(DefaultOfferSuite, {
            meta: {
                id: 'marketfront-5042',
                issue: 'MARKETFRONT-54990',
            },
            params: {
                expectedWithPrice: true,
                expectedExpressBadgeHeading: 'Express-доставка',
                expectedExpressBadgeText: 'Сегодня',
                expectedExpressBadgeHeadingColor: '#8365fc', // Экспрессный фиолетовый
                expectedExpressBadgeMessageColor: '#212121', // Чёрный
                expectedPaymentTypes: 'оплата онлайн',
                expectedButtonCaption: 'Добавить в корзину',
                expectedButtonColor: '#8365fc', // Экспрессный фиолетовый
                expectedShopName: 'Экспрессович',
                expectedWithComplainButtonOnHover: false,
            },
            pageObjects: {
                defaultOffer() {
                    return this.createPageObject(DefaultOffer);
                },
            },
        })
    ),
});
