import {makeSuite, makeCase, mergeSuites} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import {buildFormattedRoundedWeightWithoutSpaces} from '@self/root/src/entities/cargo';
import {
    skuMock as kettleSkuMock,
    offerMock as kettleOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {
    skuMock as largeCargoTypeSkuMock,
    offerMock as largeCargoTypeOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

const offerKettle = createOffer(kettleOfferMock, kettleOfferMock.wareId);
const offerLargeCargoTypeOffer = createOffer(largeCargoTypeOfferMock, largeCargoTypeOfferMock.wareId);
const reportState = mergeState([offerKettle, offerLargeCargoTypeOffer]);
const skuKettle = {
    ...kettleSkuMock,
    offers: {
        items: [kettleOfferMock],
    },
};
const skuLargeCargoTypeSkuMock = {
    ...largeCargoTypeSkuMock,
    offers: {
        items: [largeCargoTypeOfferMock],
    },
};

export default makeSuite('Вес в корзине', {
    feature: 'Вес в корзине',
    environment: 'kadavr',
    id: 'bluemarket-2803',
    issue: 'BLUEMARKET-7148',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.setPageObjects({
                    orderTotal: () => this.createPageObject(OrderTotal),
                });

                await buildCurrentSpecState.call(this);

                await this.browser.yaScenario(this, 'cart.prepareCartPageBySkuId', {
                    items: this.params.items,
                    region: this.params.region,
                    reportSkus: [skuKettle, skuLargeCargoTypeSkuMock],
                });

                return this.orderTotal.isWeightVisible().should.eventually.be.equal(
                    true, 'Вес заказа должен отображаться');
            },
        },

        makeSuite('Отображение веса заказа в саммари.', {
            story: {
                'Отображается вес товара': makeCase({
                    defaultParams: {
                        items: [{skuId: kettleSkuMock.id}],
                    },
                    test() {
                        return checkWeight.call(this);
                    },
                }),

                'Отображается суммарный вес двух одинаковых товаров': makeCase({
                    defaultParams: {
                        items: [{
                            skuId: kettleSkuMock.id,
                            count: 2,
                        }],
                    },
                    test() {
                        return checkWeight.call(this);
                    },
                }),


                'Отображается суммарный вес двух разных товаров': makeCase({
                    defaultParams: {
                        items: [
                            {skuId: kettleSkuMock.id},
                            {skuId: largeCargoTypeSkuMock.id},
                        ],
                    },
                    test() {
                        return checkWeight.call(this);
                    },
                }),
            },
        })
    ),
});

async function checkWeight() {
    const expectedValue = await getExpectedCartWeight.call(this);
    const itemsWeight = await this.orderTotal.getItemsWeightValueText();

    return this.expect(itemsWeight).eventually.equal(`${expectedValue}`,
        `Отображается вес заказа равный ${expectedValue}`);
}

async function getExpectedCartWeight() {
    const {checkouterState} = await buildCurrentSpecState.call(this);

    const carts = Object.values(checkouterState.cart || []);

    return buildFormattedRoundedWeightWithoutSpaces(carts[0].additionalCartInfo
        .reduce((acc, cartInfo) => acc + cartInfo.weight, 0));
}

function buildCurrentSpecState() {
    const cart = buildCheckouterBucket({
        items: this.params.items.map(item => ({
            skuMock: item.skuId === kettleSkuMock.id ? kettleSkuMock : largeCargoTypeSkuMock,
            offerMock: item.skuId === kettleSkuMock.id ? kettleOfferMock : largeCargoTypeOfferMock,
            count: item.count || 1,
            weight: (item.weight || 1) * 1000 * (item.count || 1),
        })),
        additionalCartInfo: this.params.items.map(item => ({
            weight: (item.weight || 1) * 1000 * (item.count || 1),
            width: 50,
            height: 50,
            depth: 50,
        })),
    });

    return this.browser.yaScenario(
        this,
        prepareMultiCartState,
        [cart],
        {existingReportState: reportState}
    );
}
