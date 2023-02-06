import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// group getter
export const getPromotionGroup = getGroupGetter('common.sidebar.unified:group.promotion');

// links
export const externalAdvertisement = getLinkItem(
    'common.sidebar.unified:external-advertisement',
    'market-partner:html:external-advertisement:get',
);
export const vendorPromotion = getLinkItem('common.sidebar.unified:vendor-promotion', 'external:vendor-promotion');
export const auction = getLinkItem('common.sidebar.unified:auction', 'market-partner:html:auction:get');
export const strategies = getLinkItem('common.sidebar.unified:strategies', 'market-partner:html:strategies:get');
export const fulfillmentPromos = getLinkItem(
    'common.sidebar.unified:promos',
    'market-partner:html:fulfillment-promos:get',
);
export const fulfillmentLoyalty = getLinkItem(
    'common.sidebar.unified:loyalty',
    'market-partner:html:fulfillment-loyalty:get',
);

export const shopInShop = getLinkItem(
    'common.sidebar.unified:business-shop-in-shop',
    'market-partner:html:business-shop-in-shop:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);

export const promos = getLinkItem(
    'common.sidebar.supplier:fulfillment-promos',
    'market-partner:html:fulfillment-promos:get',
);

export const costsAndSales = getLinkItem(
    'common.sidebar.unified:costs-and-sales',
    'market-partner:html:costs-and-sales:get',
);
export const statCpa = getLinkItem('common.sidebar.unified:stat-strategies', 'market-partner:html:stat-cpa:get');
export const shopsStatDate = getLinkItem('common.sidebar.unified:stat-date', 'market-partner:html:shops-stat-date:get');
export const shopsStatOperations = getLinkItem(
    'common.sidebar.unified:stat-operations',
    'market-partner:html:shops-stat-operations:get',
);
export const businessCashback = getLinkItem(
    'common.sidebar.unified:business-cashback',
    'market-partner:html:business-cashback-redirect:get',
);

export const externalBusinessCashback = getLinkItem(
    'common.sidebar.unified:business-cashback',
    'market-partner:html:business-cashback:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);

export const installment = getLinkItem('common.sidebar.unified:installment', 'market-partner:html:installment:get');

// group
export const promotionGroup = getPromotionGroup([
    auction,
    strategies,
    fulfillmentPromos,
    fulfillmentLoyalty,
    promos,
    costsAndSales,
    statCpa,
    shopsStatDate,
    shopsStatOperations,
    businessCashback,
    installment,
]);
