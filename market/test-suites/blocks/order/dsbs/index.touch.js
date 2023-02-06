import {
    mergeSuites,
    prepareSuite,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// Page Objects
import MyOrdersHelper from '@self/root/src/widgets/content/orders/OrderHelper/components/MyOrdersHelper/__pageObject';

// Suites
import OrderInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderInfo';

import {
    ORDER_ID,
    SHOP_ORDER_ID,
    DELIVERY_SERVICE_ID,
    TRACK_CODE,
    order,
    deliveryServiceInfo,
} from './mock';


module.exports = makeSuite('DSBS. Заказ', {
    feature: 'DSBS',
    environment: 'kadavr',
    id: 'bluemarket-3944',
    issue: 'MARKETFRONT-32913',

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    myOrdersHelper: () => this.createPageObject(MyOrdersHelper),
                });

                const checkouterCollections = await this.browser.yaScenario(
                    this,
                    'checkoutResource.createKadavrCheckouterOrdersCollections',
                    {
                        orders: [order],
                    },
                    {
                        fulfilment: false,
                        rgb: 'WHITE',
                        paymentType: 'PREPAID',
                        paymentMethod: 'YANDEX',
                        paymentStatus: 'HOLD',
                        status: 'DELIVERY',
                        creationDate: '01-01-2021 12:00:00',
                        shopOrderId: SHOP_ORDER_ID,
                    },
                    {
                        deliveryServiceInfo: {
                            [DELIVERY_SERVICE_ID]: deliveryServiceInfo,
                        },
                    }
                );
                await this.browser.setState('Checkouter.collections', checkouterCollections);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
            },
        },

        prepareSuite(OrderInfoSuite, {
            params: {
                trackingId: `${ORDER_ID}/${SHOP_ORDER_ID}`,
                status: 'Уже в пути',
                trackCode: TRACK_CODE,
                deliveryServiceName: 'Beta_Post_Online',
                deliveryServicePhone: '8 (800) 555-55-55',
                deliveryServiceWebsite: 'localhost:8080',
            },
        })
    ),
});
