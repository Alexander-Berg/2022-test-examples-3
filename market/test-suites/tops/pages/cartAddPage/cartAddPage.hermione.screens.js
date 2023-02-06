import {prepareSuite, mergeSuites, makeSuite} from '@yandex-market/ginny';
import {
    mergeState,
    createOfferForSku,
    createProductForSku,
    createSku,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import EmbedCartAdd from '@self/root/src/widgets/content/EmbedCartAdd/components/View/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';

import {commonParams} from '@self/project/src/spec/hermione/configs/params';
import embedCartAddScreenTests from '@self/platform/spec/hermione2/test-suites/blocks/embedCartAdd/index.screens';
import {
    offerMock as offerWithDiscount,
    productMock as productWithDiscount,
    skuMock as skuWithDiscount,
} from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Iframe добавления товара в корзину', {
    environment: 'kadavr',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        prepareSuite(embedCartAddScreenTests, {
            pageObjects: {
                view() {
                    return this.browser.createPageObject(EmbedCartAdd);
                },

                preloader() {
                    return this.browser.createPageObject(Preloader);
                },
            },
            hooks: {
                async beforeEach() {
                    await createStateForCardPage.call(this);

                    await this.browser.yaOpenPage(
                        'market:cart-add-embed',
                        {
                            offerId: offerWithDiscount.wareId,
                            recordPageMessages: 1,
                        }
                    );

                    await this.browser.setViewportSize({
                        width: 380,
                        height: 200,
                    });
                },
            },

            params: {
                offerId: offerWithDiscount.wareId,
            },
        })
    ),
});

async function createStateForCardPage() {
    const product = createProductForSku(
        productWithDiscount,
        skuWithDiscount.id,
        productWithDiscount.id
    );
    const sku = createSku(
        skuWithDiscount,
        skuWithDiscount.id
    );
    const offer = createOfferForSku(
        offerWithDiscount,
        skuWithDiscount.id,
        offerWithDiscount.wareId
    );
    const state = mergeState([sku, product, offer, {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    }]);
    await this.browser.yaScenario(this, setReportState, {
        state,
    });
}
