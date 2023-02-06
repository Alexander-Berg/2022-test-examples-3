import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import PromocodeBadge from '@self/root/src/components/PromocodeBadge/__pageObject';

import {offerMock, skuMock, prepareKadavrState} from '@self/root/src/spec/hermione/kadavr-mock/report/promocode';
import applyPromocode from '@self/root/src/spec/hermione/test-suites/blocks/cart/promocode';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

export default makeSuite('Акция Промокод на скидку.', {
    feature: 'Акция Промокод на скидку',
    issue: 'MARKETFRONT-23015',
    id: 'marketfront-4207',
    environment: 'kadavr',
    params: {
        region: 'Регион',
    },
    defaultParams: {
        region: region['Москва'],
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.setPageObjects({
                    cartItem: () => this.createPageObject(CartItem, {root: `${CartItem.root}`}),
                    cartItemPromocodeBadge: () => this.createPageObject(PromocodeBadge, {
                        parent: this.cartItem,
                    }),
                });

                const {state} = prepareKadavrState();

                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock,
                            offerMock,
                            count: 3,
                        }],
                    }),
                ];

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts,
                    {
                        existingReportState: state,
                        validationErrors: this.params.validationErrors,
                    }
                );

                return this.browser.yaScenario(this, prepareCartPageBySkuId, {
                    region: this.params.region,
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 3,
                    }],
                    reportSkus: [{
                        ...skuMock,
                        offers: {
                            items: [offerMock],
                        },
                    }],
                });
            },
        },
        makeSuite('Бейдж и скидки.', {
            story: {
                'Акционный бейдж не отображается': makeCase({
                    test() {
                        return this.cartItemPromocodeBadge.isVisible()
                            .should.eventually.to.be.equal(false,
                                'Не должен отображаться бейджик акции Промокод на сниппете товара');
                    },
                }),
            },
        }),
        prepareSuite(applyPromocode, {})
    ),
});
