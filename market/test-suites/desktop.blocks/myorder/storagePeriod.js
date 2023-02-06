import {makeCase, makeSuite} from 'ginny';
import dayjs from 'dayjs';
import {
    mergeState,
    createSku,
    createOfferForSku,
    createProductForSku,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {formatDate, formatDateReverse} from '@self/root/src/spec/utils/formatDate';
import {formatDaysInterval, formatPartialDateRange} from '@self/root/src/utils/datetime';
import {MSEC_IN_DAY} from '@self/root/src/constants/ttl';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import {skuMock, offerMock, productMock, outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import {recipient} from '@self/root/src/spec/hermione/configs/checkout/formData/office-address-and-recipient';

import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import StoragePeriod from '@self/root/src/components/Orders/DeliveryInfo/OrderDeliveryStoragePeriod/__pageObject';

import {checkStoragePeriod} from '@self/root/src/spec/hermione/scenarios/storagePeriod';

const storagePeriod = 3;

module.exports = makeSuite('Сроки хранения.', {
    id: 'bluemarket-3361',
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
                order: () => this.createPageObject(OrderCard),
                storagePeriod: () => this.createPageObject(StoragePeriod, {
                    parent: this.order,
                }),
            });

            const order = {
                orderId: 123456,
                items: [{
                    skuId: skuMock.id,
                    count: 1,
                    buyerPrice: 500,
                }],
                recipient,
                deliveryType: 'PICKUP',
                outletId: outletMock.id,
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
                    outlet: outletMock,
                    outletStorageLimitDate: this.params.outletStorageLimitDate,
                },
            };

            const product = createProductForSku(productMock, skuMock.id, productMock.id);
            const sku = createSku(skuMock, skuMock.id);
            const offer = createOfferForSku(offerMock, skuMock.id, offerMock.wareId);
            const state = mergeState([sku, product, offer, {
                data: {
                    results: [
                        {...outletMock, storagePeriod},
                    ],
                    search: {results: []},
                },
            }]);

            await this.browser.yaScenario(this, setReportState, {state});

            await this.browser.yaScenario(
                this,
                'checkoutResource.prepareOrder',
                {
                    status: 'DELIVERED',
                    region: this.params.region,
                    orders: [order],
                    paymentType: 'PREPAID',
                    paymentMethod: 'CASH_ON_DELIVERY',
                }
            );

            return this.browser.yaOpenPage(this.params.pageId, {orderId: order.orderId});
        },

        'Информация о заказе': {
            'содержит информацию о сроках хранения заказа в точке поставщика.': {
                'Срок хранения': makeCase({
                    defaultParams: {
                        outletStorageLimitDate: null,
                    },

                    async test() {
                        const expectedText = `Срок хранения:\n${formatDaysInterval(storagePeriod, storagePeriod)}`;

                        await this.order.isVisible()
                            .should.eventually.be.equal(true, 'Блок с подтверждением заказа должен отображаться');

                        return this.browser.yaScenario(
                            this,
                            checkStoragePeriod,
                            {
                                storagePeriodPO: this.storagePeriod,
                                expectedText,
                            }
                        );
                    },
                }),

                'Дата хранения': makeCase({
                    defaultParams: {
                        outletStorageLimitDate: formatDateReverse(new Date(Date.now() + (MSEC_IN_DAY * 2))),
                    },

                    async test() {
                        const expectedText = `Срок хранения:\n${formatPartialDateRange(null, dayjs(this.params.outletStorageLimitDate))}`;

                        await this.order.isVisible()
                            .should.eventually.be.equal(true, 'Блок с подтверждением заказа должен отображаться');

                        return this.browser.yaScenario(
                            this,
                            checkStoragePeriod,
                            {
                                storagePeriodPO: this.storagePeriod,
                                expectedText,
                            }
                        );
                    },
                }),
            },
        },
    },
});
