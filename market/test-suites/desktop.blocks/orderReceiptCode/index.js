import assert from 'assert';
import {makeSuite, makeCase} from 'ginny';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';
import {brandedOutletMock} from '@self/root/market/src/spec/hermione/test-suites/outlet/helpers';
import OrderReceiptCode from '@self/root/src/components/OrderHeader/OrderReceiptCode/__pageObject';

const ORDER_ID = 1234567890;
const ORDER_OUTLET_ID = brandedOutletMock.id;
const ORDER_COLLECTION = {
    [ORDER_ID]: {
        id: ORDER_ID,
        status: 'PICKUP',
        substatus: 'PICKUP_SERVICE_RECEIVED',
        deliveryType: 'PICKUP',
        delivery: {
            type: 'PICKUP',
            purpose: 'PICKUP',
            verificationPart: {
                barcodeData: 'Secret',
                verificationCode: '123-456-789',
            },
            outletId: ORDER_OUTLET_ID,
        },
    },
};

export default makeSuite('Код для получения посылки в постаматe и ПВЗ', {
    id: 'bluemarket-4110',
    environment: 'kadavr',
    feature: 'Код для получения посылки в постаматe и ПВЗ в списке заказов',
    issue: 'MARKETFRONT-51592',
    defaultParams: {
        isAuthWithPlugin: true,
        isAuth: true,
    },
    story: {
        async beforeEach() {
            assert(this.params.pageId, 'Param pageId must be defined in order to run this suite');

            this.setPageObjects({
                orderReceiptCode: () => this.createPageObject(OrderReceiptCode),
            });

            await this.browser.setState('Checkouter.collections.order', ORDER_COLLECTION);
            await this.browser.yaScenario(this, setReportState, {
                state: {
                    data: {
                        results: [brandedOutletMock],
                        search: {results: []},
                        blueTariffs: deliveryConditionMock,
                    },
                    collections: {
                        outlet: {[ORDER_OUTLET_ID]: brandedOutletMock},
                    },
                },
            });
            await this.browser.yaOpenPage(this.params.pageId, {orderId: ORDER_ID});
        },
        'Код для получения посылки в постаматe отображается': makeCase({
            async test() {
                await this.orderReceiptCode.isRootVisible()
                    .should.eventually.to.be.equal(true, 'Код отображается');
            },
        }),
    },
});
