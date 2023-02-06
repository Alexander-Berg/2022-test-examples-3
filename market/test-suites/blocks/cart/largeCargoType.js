import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';

import items from '@self/root/src/spec/hermione/configs/items';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import DeliveryRemainder from '@self/root/src/components/DeliveryRemainder/__pageObject';

import LargeCartNotification from
    '@self/root/src/widgets/content/cart/CartDeliveryTermsNotifier/components/LargeCartTerms/__pageObject';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    offerMock as largeCargoTypeOfferMock,
    skuMock as largeCargoTypeSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Крупногабарит', {
    feature: 'КГТ',
    environment: 'kadavr',
    params: {
        region: 'Регион',
    },
    story: mergeSuites(
        makeSuite('Интеграционные тесты.', {
            environment: 'testing',
            story: {
                async beforeEach() {
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);

                    return this.setPageObjects({
                        orderTotal: () => this.createPageObject(OrderTotal),
                        deliveryRemainder: () => this.createPageObject(DeliveryRemainder, {
                            root: `${DeliveryRemainder.root}[data-auto="term-threshold"]`,
                        }),
                        largeCartNotification: () => this.createPageObject(LargeCartNotification),
                    });
                },
                'КГТ от 500 рублей до трешхолда.': {
                    beforeEach() {
                        return this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageBySkuId',
                            {
                                items: [{
                                    skuId: items.largeCargoLowCost.skuId,
                                }],
                                region: this.params.region,
                            }
                        );
                    },
                    'Нет блока с прогрессом до бесплатной доставки': makeCase({
                        id: 'bluemarket-2596',
                        issue: 'BLUEMARKET-5558',
                        async test() {
                            return this.deliveryRemainder.isExisting()
                                .should.eventually.be.equal(false, 'Блока с прогрессом быть не должно');
                        },
                    }),
                },
                'КГТ от трешхолда.': {
                    async beforeEach() {
                        await this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageBySkuId',
                            {
                                items: [{
                                    skuId: items.largeCargo.skuId,
                                    offerId: items.largeCargo.offerId,
                                }],
                                region: this.params.region,
                            }
                        );
                    },
                    'Нет пройденного прогресса до бесплатной доставки': makeCase({
                        id: 'bluemarket-2596',
                        issue: 'BLUEMARKET-5558',
                        test() {
                            return this.deliveryRemainder.isExisting()
                                .should.eventually.be.equal(false, 'Блока с прогрессом быть не должно');
                        },
                    }),
                },
            },
        }),
        {
            beforeEach() {
                return this.setPageObjects({
                    largeCartNotification: () => this.createPageObject(LargeCartNotification),
                });
            },
            'КГТ от 500 рублей до трешхолда.': {
                async beforeEach() {
                    const offerLargeCargoType = createOffer(largeCargoTypeOfferMock, largeCargoTypeOfferMock.wareId);
                    const reportState = mergeState([offerLargeCargoType]);

                    const carts = [
                        buildCheckouterBucket({
                            items: [{
                                skuMock: largeCargoTypeSkuMock,
                                offerMock: {
                                    ...largeCargoTypeOfferMock,
                                    prices: {
                                        currency: 'RUR',
                                        value: 10000,
                                        isDeliveryIncluded: false,
                                        rawValue: 10000,
                                    },
                                },
                                weight: 100000,
                            }],
                        }),
                    ];

                    await this.browser.yaScenario(
                        this,
                        prepareMultiCartState,
                        carts,
                        {existingReportState: reportState}
                    );

                    const skuLargeCargoType = {
                        ...largeCargoTypeSkuMock,
                        offers: {
                            items: [largeCargoTypeOfferMock],
                        },
                    };

                    return this.browser.yaScenario(
                        this,
                        'cart.prepareCartPageBySkuId',
                        {
                            items: [{skuId: largeCargoTypeSkuMock.id}],
                            region: this.params.region,
                            reportSkus: [skuLargeCargoType],
                        }
                    );
                },
                'Есть нотификашка про платную доставку для КГТ в корзине': makeCase({
                    id: 'bluemarket-2596',
                    issue: 'BLUEMARKET-5558',
                    test() {
                        return this.largeCartNotification.isVisible()
                            .should.eventually.be.equal(true, 'Нотификашка есть');
                    },
                }),
            },
        }
    ),
});
