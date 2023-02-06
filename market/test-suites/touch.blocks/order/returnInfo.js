import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import UnavailableReturn from '@self/root/src/widgets/parts/ReturnCandidate/components/UnavailableReturn/__pageObject';
import {asus, iphone7Plus, alcohol} from '@self/root/src/spec/hermione/configs/checkout/items';
import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';

import {preparePageForReturnInfo} from '@self/root/src/spec/hermione/scenarios/orders';

import {NON_RETURNABLE_REASONS} from '@self/root/src/entities/returnableItem';

import UnavailableText
    // eslint-disable-next-line max-len
    from '@self/root/src/widgets/parts/ReturnCandidate/components/UnavailableReturn/components/UnavailableText/__pageObject';
import MaxReturnTimeReached
    // eslint-disable-next-line max-len
    from '@self/root/src/widgets/parts/ReturnCandidate/components/UnavailableReturn/components/UnavailableText/components/MaxReturnTimeReached/__pageObject';

import availableReturnInfo from './returnOrder/availableReturn';
import unavailableReturnInfo from './returnOrder/unvailableReturn';

module.exports = makeSuite('Информация о возврате', {
    feature: 'Информация о возврате',
    id: 'bluemarket-2831',
    issue: 'BLUEMARKET-7029',
    environment: 'kadavr',
    params: {
        items: 'Товары в заказе',
    },
    defaultParams: {
        items: [{
            skuId: asus.skuId,
            offerId: asus.offerId,
            wareMd5: asus.offerId,
            count: 1,
            id: 123456,
        }, {
            skuId: iphone7Plus.skuId,
            offerId: iphone7Plus.offerId,
            wareMd5: iphone7Plus.offerId,
            count: 1,
            id: 456789,
        }],
        nonReturnableCategoryItems: [{
            skuId: alcohol.skuId,
            offerId: alcohol.offerId,
            wareMd5: alcohol.offerId,
            count: 1,
            id: 987654,
        }],
    },

    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    returnsPage: () => this.createPageObject(ReturnsPage),
                });
            },
        },
        {
            'Обычный заказ (все товары можно вернуть).': mergeSuites(
                {
                    beforeEach() {
                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: this.params.items,
                            returnableItems: this.params.items,
                        });
                    },
                },
                availableReturnInfo
            ),
            'Заказ с 3 товарами: 1 - нельзя вернуть, 2 - истёк срок возврата, 3 - можно вернуть.': mergeSuites(
                {
                    beforeEach() {
                        const returnableItems = this.params.items.slice(0, 1);
                        const nonReturnableItems = [{
                            ...this.params.items[1],
                            nonReturnableReason: NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT,
                        }, {
                            ...this.params.nonReturnableCategoryItems[0],
                            nonReturnableReason: NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY,
                        }];

                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: [...this.params.items, ...this.params.nonReturnableCategoryItems],
                            returnableItems,
                            nonReturnableItems,
                            nonReturnableReasons: [
                                NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT,
                                NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY,
                            ],
                        });
                    },
                },
                availableReturnInfo
            ),
            'Заказ с товаром, который нельзя вернуть.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            unavailableReturnInfo: () => this.createPageObject(UnavailableReturn, {
                                parent: this.returnsPage,
                            }),
                            unavailableText: () => this.createPageObject(UnavailableText, {
                                parent: this.unavailableReturnInfo,
                            }),
                            maxReturnTimeReached: () => this.createPageObject(MaxReturnTimeReached, {
                                parent: this.unavailableText,
                            }),
                        });

                        const nonReturnableItems = [{
                            ...this.params.nonReturnableCategoryItems[0],
                            nonReturnableReason: NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY,
                        }];

                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: nonReturnableItems,
                            nonReturnableItems,
                            nonReturnableReasons: [NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY],
                        });
                    },
                },
                prepareSuite(unavailableReturnInfo, {
                    params: {
                        nonReturnableReasons: [NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY],
                    },
                })
            ),
            'Заказ с товаром, срок возврата которого истёк.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            unavailableReturnInfo: () => this.createPageObject(UnavailableReturn, {
                                parent: this.returnsPage,
                            }),
                            unavailableText: () => this.createPageObject(UnavailableText, {
                                parent: this.unavailableReturnInfo,
                            }),
                            maxReturnTimeReached: () => this.createPageObject(MaxReturnTimeReached, {
                                parent: this.unavailableText,
                            }),
                        });

                        const nonReturnableItems = [{
                            ...this.params.items[1],
                            nonReturnableReason: NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT,
                        }];

                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: nonReturnableItems,
                            nonReturnableItems,
                            nonReturnableReasons: [NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT],
                        });
                    },
                },
                prepareSuite(unavailableReturnInfo, {
                    params: {
                        nonReturnableReasons: [NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT],
                    },
                })
            ),
            'Заказ с товаром, заказанным более 15 дней назад.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            unavailableReturnInfo: () => this.createPageObject(UnavailableReturn, {
                                parent: this.returnsPage,
                            }),
                            unavailableText: () => this.createPageObject(UnavailableText, {
                                parent: this.unavailableReturnInfo,
                            }),
                            maxReturnTimeReached: () => this.createPageObject(MaxReturnTimeReached, {
                                parent: this.unavailableText,
                            }),
                        });

                        const nonReturnableItems = [{
                            ...this.params.items[1],
                            nonReturnableReason: NON_RETURNABLE_REASONS.MAX_RETURN_TIME_LIMIT_REACHED,
                        }];

                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: nonReturnableItems,
                            nonReturnableItems,
                            nonReturnableReasons: [NON_RETURNABLE_REASONS.MAX_RETURN_TIME_LIMIT_REACHED],
                        });
                    },
                },
                prepareSuite(unavailableReturnInfo, {
                    params: {
                        nonReturnableReasons: [NON_RETURNABLE_REASONS.MAX_RETURN_TIME_LIMIT_REACHED],
                    },
                })
            ),
            'Заказ с товарами, один из которых нельзя вернуть, а срок возврата второго истёк.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            unavailableReturnInfo: () => this.createPageObject(UnavailableReturn, {
                                parent: this.returnsPage,
                            }),
                            unavailableText: () => this.createPageObject(UnavailableText, {
                                parent: this.unavailableReturnInfo,
                            }),
                            maxReturnTimeReached: () => this.createPageObject(MaxReturnTimeReached, {
                                parent: this.unavailableText,
                            }),
                        });

                        const nonReturnableItems = [{
                            ...this.params.items[1],
                            nonReturnableReason: NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT,
                        }, {
                            ...this.params.nonReturnableCategoryItems[0],
                            nonReturnableReason: NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY,
                        }];

                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: nonReturnableItems,
                            nonReturnableItems,
                            nonReturnableReasons: [
                                NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT,
                                NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY,
                            ],
                        });
                    },
                },
                prepareSuite(unavailableReturnInfo, {
                    params: {
                        nonReturnableReasons: [
                            NON_RETURNABLE_REASONS.RETURNABLE_CATEGORY_WITH_TIME_LIMIT,
                            NON_RETURNABLE_REASONS.NOT_RETURNABLE_CATEGORY,
                        ],
                    },
                })
            ),
            'Заказ с товарами, по которым уже прошёл успешный возврат.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            unavailableReturnInfo: () => this.createPageObject(UnavailableReturn, {
                                parent: this.returnsPage,
                            }),
                            unavailableText: () => this.createPageObject(UnavailableText, {
                                parent: this.unavailableReturnInfo,
                            }),
                            maxReturnTimeReached: () => this.createPageObject(MaxReturnTimeReached, {
                                parent: this.unavailableText,
                            }),
                        });

                        const nonReturnableItems = this.params.items.map(item => ({
                            ...item,
                            nonReturnableReason: NON_RETURNABLE_REASONS.ALREADY_REFUNDED,
                        }));

                        return this.browser.yaScenario(this, preparePageForReturnInfo, {
                            orderItems: nonReturnableItems,
                            nonReturnableItems,
                            nonReturnableReasons: [NON_RETURNABLE_REASONS.ALREADY_REFUNDED],
                        });
                    },
                },
                prepareSuite(unavailableReturnInfo, {
                    params: {
                        nonReturnableReasons: [NON_RETURNABLE_REASONS.ALREADY_REFUNDED],
                    },
                })
            ),
        }
    ),
});
