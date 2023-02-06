import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {mergeState, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import returnOptionsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterMoscowReturnOptions';
import returnOutletsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import ReturnMapOutletInfo from '@self/root/src/widgets/parts/ReturnCandidate/widgets/ReturnMapOutletInfo/__pageObject';

import orderReturnReasonSuite from './returnReason';
import orderReturnPrepaidSuite from './prepaid';
import orderReturnPostpaidSuite from './postpaid';
import bankAccountFormSuite from './bankAccountForm';
import returnPhoto from './returnPhoto';


const ID = 11111;

export default makeSuite('Форма заявления на возврат.', {
    params: {
        items: 'Товары',
        paymentType: 'Тип оплаты',
    },
    defaultParams: {
        items: [{
            id: ID,
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            count: 5,
        }],
    },
    feature: 'Форма заявления на возрат.',
    issue: 'BLUEMARKET-4950',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    returnsPage: () => this.createPageObject(ReturnsPage),
                    returnMap: () => this.createPageObject(PlacemarkMap, {parent: this.returnsForm}),
                    returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo, {parent: this.returnsForm}),
                });

                const shopId = 101;
                const shopInfo = createShopInfo({
                    returnDeliveryAddress: 'hello, there!',
                }, shopId);

                await this.browser.yaScenario(this, setReportState, {
                    state: mergeState(
                        [shopInfo],
                        {
                            data: {
                                results: returnOutletsMock,
                                search: {results: []},
                            },
                        }
                    ),
                });

                await this.browser.setState(
                    'Checkouter.returnOptions',
                    returnOptionsMock
                );

                await this.browser.setState(
                    'Checkouter.returnableItems',
                    this.params.items.map(item => ({
                        ...item,
                        itemId: item.id,
                    }))
                );

                await this.browser.setState('schema', {
                    mdsPictures: [{
                        groupId: 3723,
                        imageName: '2a000001654282aec0648192ce44a1708325',
                    }],
                });

                return this.browser.yaScenario(this, 'checkoutResource.prepareOrder', {
                    region: this.params.region,
                    orders: [{
                        items: this.params.items,
                        deliveryType: 'DELIVERY',
                        shopId,
                    }],
                    paymentType: this.params.paymentType,
                    paymentMethod: this.params.paymentType === 'PREPAID' ? 'YANDEX' : 'CASH_ON_DELIVERY',
                    status: 'DELIVERED',
                })
                    .then(result => this.browser.yaProfile('pan-topinambur', PAGE_IDS_COMMON.CREATE_RETURN, {
                        orderId: result.orders[0].id,
                        type: 'refund',
                    }));
            },
        },

        prepareSuite(orderReturnReasonSuite, {}),
        prepareSuite(orderReturnPrepaidSuite, {}),
        prepareSuite(orderReturnPostpaidSuite, {}),
        prepareSuite(bankAccountFormSuite, {}),
        prepareSuite(returnPhoto, {})
    ),
});
