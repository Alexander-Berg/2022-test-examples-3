import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {productMock, offerMock} from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {createState} from '@self/root/src/spec/utils/kadavr';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {sku, offer} from '@self/root/src/spec/hermione/kadavr-mock/report/with-accessories';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

import scrollboxMetrikaSuite from './scrollBox';
import schemas from './schemas';

module.exports = makeSuite('Метрика', {
    feature: 'Метрика',
    environment: 'kadavr',
    defaultParams: {
        goalNamePrefix: 'CART-PAGE',
        items: [{
            skuId: sku.id,
            offerId: offer.id,
        }],
        region: region['Москва'],
    },
    story: mergeSuites(
        prepareSuite(scrollboxMetrikaSuite, {
            suiteName: '"Часто бывает нужно".',
            meta: {
                issue: 'BLUEMARKET-5630',
                id: 'bluemarket-2604',
            },
            params: {
                selector: '[data-zone-data*=\'"cmsWidgetId":"CommonlyPurchased"\']',
                payloadWidgetSchema: {
                    ...schemas.CommonlyPurchasedProducts.root,
                    cmsWidgetId: 'CommonlyPurchased',
                },
                payloadSnippetSchema: schemas.CommonlyPurchasedProducts.snippet,
            },
            hooks: {
                async beforeEach() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-49185');
                    /** данные для мока репорта */
                    // eslint-disable-next-line no-unreachable
                    const reportResult = createState({product: productMock, offer: offerMock}, 6);
                    /** мокаем данные для виджета */
                    await this.browser.yaScenario(this, setReportState, {state: reportResult});
                    /** открываем страницу корзины */
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
                },
            },
        })
    ),
});
