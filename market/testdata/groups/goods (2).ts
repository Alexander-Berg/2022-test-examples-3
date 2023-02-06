import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// Group getter
export const getGoodsGroup = getGroupGetter('common.sidebar.unified:group.goods');

// Links
export const feedsList = getLinkItem('common.sidebar.shop:feeds-list', 'market-partner:html:feeds-list:get');
export const supplierCatalog = getLinkItem(
    'common.sidebar.unified:catalog',
    'market-partner:html:supplier-catalog:get',
);
export const assortment = getLinkItem('common.sidebar.unified:catalog', 'market-partner:html:assortment:get');
export const shopOfferList = getLinkItem('common.sidebar.unified:catalog', 'market-partner:html:shop-offer-list:get');
export const fulfillmentPrices = getLinkItem(
    'common.sidebar.unified:prices',
    'market-partner:html:fulfillment-prices:get',
);
export const fulfillmentHiddenSku = getLinkItem(
    'common.sidebar.unified:hidden-offers',
    'market-partner:html:fulfillment-hidden-sku:get',
);
export const hiddenOffers = getLinkItem(
    'common.sidebar.unified:hidden-offers',
    'market-partner:html:hidden-offers:get',
);
export const productSources = getLinkItem(
    'common.sidebar.unified:product-sources',
    'market-partner:html:product-sources:get',
);

export const offers = getLinkItem('common.sidebar.unified:offers-by-category', 'market-partner:html:offers:get');
export const discountOffers = getLinkItem(
    'common.sidebar.unified:discount-offers',
    'market-partner:html:discount-offers:get',
);
export const externalMarketOffers = getLinkItem('common.sidebar.unified:market-offers', 'external:market-offers', {
    urlParams: {
        tld: 'tld',
    },
});
export const externalBeruSearch = getLinkItem('common.sidebar.unified:market-offers', 'external:beru-search');
export const externalMarketBusiness = getLinkItem('common.sidebar.unified:market-offers', 'external:market-business', {
    ignorePlatformTypeOnMapping: true,
    urlParams: {
        tld: 'tld',
    },
    urlParamsMap: {
        businessId: 'businessId',
    },
});

export const newModels = getLinkItem('common.sidebar.unified:new-models', 'market-partner:html:new-models:get');
export const skuSearch = getLinkItem('common.sidebar.unified:sku-search', 'market-partner:html:sku-search:get');
export const modelsXLS = getLinkItem('common.sidebar.unified:manage-xls', 'market-partner:html:models-xls:get');
export const contentXLS = getLinkItem('common.sidebar.unified:manage-xls', 'market-partner:html:content-xls:get');
export const shopsIndexing = getLinkItem('common.sidebar.unified:indexing', 'market-partner:html:shops-indexing:get');
export const fulfillmentDocuments = getLinkItem(
    'common.sidebar.unified:goods-documents',
    'market-partner:html:fulfillment-documents:get',
);
export const supplierDocuments = getLinkItem(
    'common.sidebar.unified:goods-documents',
    'market-partner:html:supplier-documents:get',
);

// Services
export const priceLabsBrokerDeattachedCard = getLinkItem(
    'common.sidebar.shop:pricelabs-broker-deattached-card',
    'external:pricelabs-broker-deattached-card',
    {
        urlParams: {
            campaignId: 'id',
        },
    },
);
export const priceLabsDashboardAll = getLinkItem(
    'common.sidebar.shop:pricelabs-dashboard-all',
    'external:pricelabs-dashboard-all',
    {
        urlParams: {
            campaignId: 'id',
        },
    },
);
