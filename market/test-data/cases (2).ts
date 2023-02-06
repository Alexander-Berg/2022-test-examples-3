import {PLATFORM_TYPE_USER} from '@yandex-market/b2b-core/shared/constants/user';

import {PLATFORM_TYPE} from 'shared/constants/campaign';
import {getExternalPublicFeedUrl} from 'spec/utils/externalFeedUrls';

import shops from './testing-shops';
import users from './users';

export default {
    'refund-popup-check-refund': {
        data: {
            campaignId: shops['autotestmarket-global.yandex.ru'].campaignId,
            shopId: shops['autotestmarket-global.yandex.ru'].shopId,
            orderId: 2255367,
        },
        user: users['autotests.market-partner-web1'],
    },

    'refund-popup-check-all-products': {
        data: {
            orderId: 2120859,
            total: '2990 руб.',
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-user-init-refund': {
        data: {
            orderId: 2105079,
            items: [
                {name: 'Пухлый Усатый Тюлень', count: 1},
                {name: 'Kopi Luwak special001', count: 1},
            ],
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-uncheck-delivery': {
        data: {
            orderId: 2120860,
            total: '2790 руб.',
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-uncheck-several-products': {
        data: {
            orderId: 2120860,
            total: '0 руб.',
            products: ['Пухлый Усатый Тюлень', 'Доставка'],
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-uncheck-all-products': {
        data: {
            orderId: 2120860,
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-change-number-of-product': {
        data: {
            orderId: 2120861,
            startNumber: 3,
            newNumber: 1,
            total: '2990 руб.',
            productName: 'Пухлый Усатый Тюлень',
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-refund-by-product': {
        data: {
            outletId: 10243984,
            region: 2,
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'refund-popup-refund-by-cost': {
        data: {
            items: [{feedId: 200344259, offerId: '54', buyerPrice: 251, count: 1}],
            outletId: 10243984,
            region: 2,
        },
        shop: shops['autotestmarket-6506.yandex.ru'],
    },

    'supplier-prices-url': {
        data: {
            url: getExternalPublicFeedUrl('online-blue-AT-feed-1000573837.xlsm'),
        },
    },

    'red-market-feed': {
        data: {
            login: 'marketdatabuild',
            password: 'Eg7dWj8tu44WX6Z',
            filePath: '../../../test-data/red-feed',
            red: {
                url: 'https://svn.yandex.ru/market/market_tests/testshops/redFeeds/red_feed5.zip',
                fileName: 'test_feed_cpa-cpa-hybrid.xml',
            },
            white: {
                url: 'https://svn.yandex.ru/market/market_tests/testshops/weapons-dealer-feed.xml',
                fileName: 'feed-with-weapons.xml',
            },
        },
    },

    'offshop-list-add-delete': {
        data: {
            name: 'New sale point',
        },
        shops: {
            testing: 'autotest-prepay-ooo',
            production: 'autotests-market-partner-web.yandex.ru',
        },
    },

    'offshop-delivery-service-change': {
        data: {
            name: 'Delivery service test',
            offshopType: 'DEPOT',
            deliveryService: 'FedEx',
            deliveryTime: 1,
        },
        shops: {
            testing: 'auction-autotest.yandex.ru',
            production: 'autotests-market-partner-web1.yandex.ru',
        },
    },

    'offshop-delivery-service-default': {
        data: {
            name: 'Default delivery service test',
            offshopType: 'DEPOT',
            deliveryTime: 1,
        },
        shops: {
            testing: 'testshop20161123121609.yandex.ru',
        },
    },

    'offshop-self-pick-price': {
        data: {
            name: 'Self pick price',
            offshopType: 'DEPOT',
            deliveryCost: 250,
            deliveryCostPriceTo: 250,
            deliveryTime: 1,
        },
        shops: {
            testing: 'autotest-pickup-edit-2.ru',
        },
    },

    'offshop-self-pick-price-free': {
        data: {
            name: 'Self pick price free',
            offshopType: 'DEPOT',
            deliveryCost: 0,
            deliveryTime: 1,
        },
        shops: {
            testing: 'autotest-pickup-edit-3.ru',
        },
    },

    'offshop-delivery-period-valid': {
        data: {
            name: 'Delivery period valid',
            offshopType: 'DEPOT',
            deliveryTime: '1-3',
        },
        shops: {
            testing: 'autotest-pickup-edit-1.ru',
        },
    },

    'offshop-delivery-period-invalid': {
        data: {
            name: 'Delivery period invalid',
            offshopType: 'DEPOT',
            deliveryTime: '1-5',
            city: 'Москва',
            street: 'Красная площадь',
            house: 1,
        },
        shops: {
            testing: 'autotest-pickup-edit-4.ru',
        },
    },

    'offshop-delivery-period-zero': {
        data: {
            name: 'Delivery period invalid',
            offshopType: 'DEPOT',
            deliveryTime: 0,
        },
        shops: {
            testing: 'autotest-pickup-edit-5.ru',
        },
    },

    'offshop-delivery-period-order': {
        data: {
            name: 'Delivery period by order',
            offshopType: 'DEPOT',
            deliveryTime: 0,
            deliveryTimeUnspecified: 1,
        },
        shops: {
            testing: 'autotest-pickup-edit-6.ru',
        },
    },

    'offshop-delivery-period-time-switch': {
        data: {
            name: 'Delivery period with time switched',
            offshopType: 'DEPOT',
            deliveryTime: 10,
            deliveryTimeSwitch: '16:00',
        },
        shops: {
            testing: 'autotest-pickup-edit-7.ru',
        },
    },

    'message-bell-counter-owner': {
        shop: shops['autotest-post-00'],
        user: users['autotests.market-partner-web1'],
        page: 'market-partner:html:shops-dashboard:get',
        query: {
            platformType: PLATFORM_TYPE.SHOP,
            campaignId: shops['autotest-post-00'].campaignId,
        },
        isBellVisible: false,
        needMessage: true,
    },

    'message-bell-counter-manager': {
        shop: shops['autotest-message-00'],
        user: users.autotestmanager,
        page: 'market-partner:html:shops-dashboard:get',
        query: {
            platformType: PLATFORM_TYPE.SHOP,
            campaignId: shops['autotest-message-00'].campaignId,
            euid: shops['autotest-message-00'].contacts.owner.uid,
        },
        isBellVisible: true,
        needMessage: true,
        isManager: true,
    },

    'critical-message-filter': {
        shop: shops['autotest-post-00'],
        user: users['autotests.market-partner-web1'],
        page: 'market-partner:html:message:get',
        query: {
            platformType: PLATFORM_TYPE_USER,
            id: shops['autotest-post-00'].campaignId,
            euid: shops['autotest-post-00'].contacts.owner.uid,
        },
        data: {
            criticalMessageTheme: 'В магазин autotest-post-00 поступил новый заказ 9000',
            regularMessageTheme: 'Счёт магазина (используется автотестами) пополнен',
        },
        needMessage: true,
        needCriticalMessage: true,
    },

    'referee-order-snapshot': {
        data: {
            orderId: 2296224,
        },
    },

    'snapshot-expire': {
        shop: shops['autotestmarket-global.yandex.ru'],
        orderId: 2158486,
    },

    'red-market-sandbox': {
        addressData: {},
        region: 'Москва',
        shop: 'autotest-red-shop-3',
        data: {
            street: 'Elm street',
            house: '13',
            index: '666',
            recipient: 'Freddy Krueger',
            phone: '+7 999 888 77 66',
        },
    },

    'dropship-sandbox-planeshift': {
        addressData: {},
        shop: 'bluepharma.three',
        data: {
            recipient: 'Freddy Krueger',
            phone: '+7 999 888 77 66',
        },
    },

    'dropship-sandbox-main': {
        addressData: {},
        shop: 'bluepharma.two',
        data: {
            recipient: 'Freddy Krueger',
            phone: '+7 999 888 77 66',
        },
    },

    'blue-dropship-sandbox-main': {
        addressData: {},
        region: 'Москва',
        shop: 'rnpn.supplier.one',
        data: {
            street: 'Elm street',
            house: '13',
            recipient: 'Freddy Krueger',
            phone: '+7 999 888 77 66',
        },
    },

    'blue-market-offer': {
        query: 'Nokia 3310 красный dual sim',
        productionId: '100230445428',
        testingId: '100256629422',
        name: ['Телефон Nokia 3310', 'красный'],
    },

    'blue-market-offer-iphone': {
        query: 'iphone',
    },

    'message-allowed-access': {
        shop: shops['autotest-post-02'],
        user: users['autotests.market-partner-web1'],
        user2: users['autotests.market-partner-web2'],
        page: 'market-partner:html:message:get',
        query: {
            platformType: PLATFORM_TYPE_USER,
        },
        needMessage: true,
        alertText: 'Не указан идентификатор сообщения, или сообщение с таким идентификатором не существует.',
    },
};
