import {makeCase, makeSuite} from 'ginny';
import {formatDate} from '@self/root/src/spec/utils/formatDate';
import {formatDaysInterval} from '@self/root/src/utils/datetime';
import {MSEC_IN_DAY} from '@self/root/src/constants/ttl';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import {skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import {alcohol as alcoOutlet} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {recipient} from '@self/root/src/spec/hermione/configs/checkout/formData/office-address-and-recipient';

import {OrderHeader} from '@self/root/src/components/OrderHeader/__pageObject';
import {OrderInfo} from '@self/root/src/components/OrderInfo/__pageObject';
import {OrderInfoLine} from '@self/root/src/components/OrderInfo/OrderInfoLine/__pageObject';
import StoragePeriod from '@self/root/src/components/StoragePeriod/__pageObject';

import {checkStoragePeriod} from '@self/root/src/spec/hermione/scenarios/storagePeriod';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

const storagePeriod = 3;

const order = {
    orderId: 123456,
    items: [{
        skuId: skuMock.id,
        count: 1,
        buyerPrice: 500,
    }],
    recipient,
    deliveryType: 'PICKUP',
    outletId: alcoOutlet.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 0,
        dates: {
            fromDate: formatDate(),
            // Сегодня + 2 дня
            toDate: formatDate(new Date(Date.now() + (MSEC_IN_DAY * 2))),
        },
        outletStoragePeriod: storagePeriod,
        outlet: alcoOutlet,
    },
};

module.exports = makeSuite('Сроки хранения.', {
    id: 'bluemarket-3366',
    issue: 'BLUEMARKET-10247',
    environment: 'kadavr',
    feature: 'C&C',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderHeader: () => this.createPageObject(OrderHeader, {
                    parent: this.orderCard,
                }),
                orderInfo: () => this.createPageObject(OrderInfo, {
                    parent: this.orderCard,
                }),
                infoStoragePeriod: () => this.createPageObject(OrderInfoLine, {
                    root: OrderInfo.storagePeriod,
                }),
                headerStoragePeriod: () => this.createPageObject(StoragePeriod, {
                    parent: this.orderHeader,
                }),
            });

            const defaultState = mergeState([
                {
                    data: {
                        results: [
                            {...alcoOutlet, storagePeriod},
                        ],
                        search: {results: []},
                    },
                },
            ]);

            await this.browser.yaScenario(this, setReportState, {state: defaultState});

            await this.browser.yaScenario(
                this,
                prepareOrder,
                {
                    status: 'DELIVERY',
                    region: this.params.region,
                    orders: [order],
                    paymentType: 'PREPAID',
                    paymentMethod: 'CASH_ON_DELIVERY',
                }
            );

            return this.browser.yaOpenPage(this.params.pageId, {orderId: order.orderId});
        },
        'Блок "Данные доставки"': {
            'содержит информацию о сроках хранения заказа': makeCase({
                async test() {
                    await this.infoStoragePeriod.getTitleText()
                        .should.eventually.be.equal(
                            'Срок хранения',
                            'Блок "Данные доставки" должен содержать блок "Срок хранения" с корректным заголовком'
                        );

                    return this.browser.yaScenario(
                        this,
                        checkStoragePeriod,
                        {
                            storagePeriodPO: this.infoStoragePeriod,
                            expectedText: formatDaysInterval(storagePeriod, storagePeriod),
                        }
                    );
                },
            }),
        },
    },
});
