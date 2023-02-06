import {getLinkItem, getGroupGetter} from '../../../../utils/mocks';

// Group getter
export const getGoodsGroup = getGroupGetter('common.sidebar.unified:group.goods');

// Links
export const supplierCatalog = getLinkItem(
    'common.sidebar.unified:catalog',
    'market-partner:html:supplier-catalog:get',
);
export const businessAssortment = getLinkItem(
    'common.sidebar.unified:business-assortment',
    'market-partner:html:business-assortment:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const assortment = getLinkItem('common.sidebar.unified:assortment', 'market-partner:html:assortment:get');
export const shopOfferList = getLinkItem('common.sidebar.unified:catalog', 'market-partner:html:shop-offer-list:get');
export const fulfillmentPrices = getLinkItem(
    'common.sidebar.unified:prices',
    'market-partner:html:fulfillment-prices:get',
);
export const productSources = getLinkItem(
    'common.sidebar.unified:product-sources',
    'market-partner:html:business-product-sources:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);

export const offers = getLinkItem('common.sidebar.unified:offers-by-category', 'market-partner:html:offers:get');
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

export const modelsXLS = getLinkItem('common.sidebar.unified:manage-xls', 'market-partner:html:models-xls:get');
export const contentXLS = getLinkItem(
    'common.sidebar.unified:manage-xls',
    'market-partner:html:business-content-xls:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const businessFeedsUploadHistory = getLinkItem(
    'common.sidebar.unified:business-feeds-upload-history',
    'market-partner:html:business-feeds-upload-history:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
export const shopsIndexing = getLinkItem('common.sidebar.unified:indexing', 'market-partner:html:shops-indexing:get');
export const fulfillmentDocuments = getLinkItem(
    'common.sidebar.unified:goods-documents',
    'market-partner:html:fulfillment-documents:get',
);
export const supplierDocuments = getLinkItem(
    'common.sidebar.unified:goods-documents',
    'market-partner:html:supplier-documents:get',
);
export const batchImageUpload = getLinkItem(
    'common.sidebar.unified:batch-image-upload',
    'market-partner:html:batch-image-upload:get',
    {
        ignorePlatformParams: true,
        urlParamsMap: {
            businessId: 'businessId',
        },
    },
);
