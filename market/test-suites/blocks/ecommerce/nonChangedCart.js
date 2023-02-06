import {
    makeSuite,
    makeCase,
} from 'ginny';
import * as vitaminsLowCost from '@self/root/src/spec/hermione/kadavr-mock/report/vitaminsLowCost';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as televisor from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {checkEcommerceDataByEventName} from '@self/root/src/spec/hermione/scenarios/checkEcommerce';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

const skuKettle = {
    ...kettle.skuMock,
    offers: {
        items: [kettle.offerMock],
    },
};
const skuVitaminsLowCost = {
    ...vitaminsLowCost.skuMock,
    offers: {
        items: [vitaminsLowCost.offerMock],
    },
};

const skuTelevisor = {
    ...televisor.skuMock,
    offers: {
        items: [televisor.offerMock],
    },
};

module.exports = makeSuite('Ecommerce. Данные на странице корзины', {
    feature: 'Ecommerce',
    environment: 'kadavr',
    defaultParams: {
        isAuth: false,
    },
    story: {
        async beforeEach() {
            await this.browser.yaScenario(this, prepareMultiCartState, [buildCheckouterBucket({
                items: [{
                    skuMock: vitaminsLowCost.skuMock,
                    offerMock: vitaminsLowCost.offerMock,
                    count: 1,
                }, {
                    skuMock: kettle.skuMock,
                    offerMock: kettle.offerMock,
                    count: 1,
                }, {
                    skuMock: televisor.skuMock,
                    offerMock: televisor.offerMock,
                    count: 1,
                }],
            })]);

            return this.browser.yaScenario(
                this,
                'cart.prepareCartPageBySkuId',
                {
                    items: [{
                        skuId: vitaminsLowCost.skuMock.id,
                        offerId: vitaminsLowCost.offerMock.wareId,
                    }, {
                        skuId: kettle.skuMock.id,
                        offerId: kettle.offerMock.wareId,
                    }, {
                        skuId: televisor.skuMock.id,
                        offerId: televisor.offerMock.wareId,
                    }],
                    region: this.params.region,
                    reportSkus: [skuVitaminsLowCost, skuKettle, skuTelevisor],
                }
            );
        },
        'При загрузке корзины объект dataLayer получает нужные данные': makeCase({
            id: 'bluemarket-3213',
            issue: 'BLUEMARKET-8431',
            async test() {
                const expectedResult = createEcommerceExpectedEventValue(
                    [vitaminsLowCost.offerMock, kettle.offerMock, televisor.offerMock],
                    'CART_INIT'
                );

                await this.browser.yaWaitForPageReady();

                return this.browser.yaScenario(
                    this,
                    checkEcommerceDataByEventName,
                    expectedResult
                );
            },
        }),
    },
});

const createEcommerceExpectedEventValue = (offersMocks, eventName) => {
    const impressions = offersMocks.reduce((acc, offerMock, id) => {
        acc.push(createEcommerceData(offerMock, id));
        return acc;
    }, []);
    const totalPrice = offersMocks.reduce((acc, offerMock) =>
        acc + Number(offerMock.prices.value), 0);

    return [{
        ecommerce: {
            impressions,
            totalPrice,
        },
        event: eventName,
    }];
};

const createEcommerceData = (offerMock, id) => ({
    hid: offerMock.categories[0].id,
    id: offerMock.sku,
    list: 'userCart',
    name: offerMock.titles.raw,
    position: id + 1,
    price: Number(offerMock.prices.value),
    quantity: 1,
});
