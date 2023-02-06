import {makeSuite, makeCase} from 'ginny';

import {
    offerMock as largeCargoTypeOfferMock,
    skuMock as largeCargoTypeSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {ITEM_PRICE} from '@self/root/src/spec/hermione/configs/cart/checkouter';

import LargeCartNotification from
    '@self/root/src/widgets/content/cart/CartDeliveryTermsNotifier/components/LargeCartTerms/__pageObject';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

export default makeSuite('Тяжёлая корзина', {
    feature: 'Тяжёлая корзина',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                notification: () => this.createPageObject(LargeCartNotification),
            });

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: largeCargoTypeSkuMock,
                        offerMock: {
                            ...largeCargoTypeOfferMock,
                            prices: {
                                currency: 'RUR',
                                value: ITEM_PRICE,
                                isDeliveryIncluded: false,
                                rawValue: ITEM_PRICE,
                            },
                        },
                        count: 31,
                        weight: largeCargoTypeOfferMock.weight,
                    }],
                }),
            ];

            const state = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            return this.browser.yaScenario(this, 'cart.prepareCartPageBySkuId', {
                items: [{
                    skuId: largeCargoTypeSkuMock.id,
                }],
                region: this.params.region,
                reportSkus: state.reportSkus,
            });
        },
        'Отображается плашка для тяжёлых корзин': makeCase({
            id: 'bluemarket-2759',
            issue: 'BLUEMARKET-6389',
            test() {
                return this.expect(this.notification.isVisible()).to.be.equal(
                    true, 'Плашка для тяжелых корзин должна отображаться');
            },
        }),
    },
});
