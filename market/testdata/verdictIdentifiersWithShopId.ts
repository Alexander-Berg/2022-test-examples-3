const OFFER_ID = 'offer1';
const BUSINESS_ID = 999;

export const verdictIdentifiersWithShopId1 = [
    {
        shopId: 1,
        warehouseId: 111,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 2,
        warehouseId: 221,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 2,
        warehouseId: 222,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 2,
        warehouseId: 223,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
];

export const shopIdentifierMap1 = {
    2: {
        shopId: 2,
        relatedWarehouseIds: [221, 222, 223],
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    1: {
        shopId: 1,
        relatedWarehouseIds: [111],
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
};

export const verdictIdentifiersWithShopId2 = [
    {
        shopId: 1,
        warehouseId: 111,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 1,
        warehouseId: 0,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 1,
        warehouseId: 113,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 2,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 2,
        warehouseId: 221,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 2,
        warehouseId: 222,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 3,
        warehouseId: 331,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 3,
        warehouseId: 332,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 4,
        warehouseId: 441,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    {
        shopId: 4,
        warehouseId: 0,
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
];

export const shopIdentifierMap2 = {
    1: {
        shopId: 1,
        relatedWarehouseIds: [],
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    2: {
        shopId: 2,
        relatedWarehouseIds: [],
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    3: {
        shopId: 3,
        relatedWarehouseIds: [331, 332],
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
    4: {
        shopId: 4,
        relatedWarehouseIds: [],
        offerId: OFFER_ID,
        businessId: BUSINESS_ID,
    },
};
