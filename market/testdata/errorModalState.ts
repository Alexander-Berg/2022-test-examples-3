import {mockType} from 'spec/jest';

import type {OffersState, PageState} from 'shared/pages/Assortment';
import type {ErrorItem} from 'shared/types/assortment';
import type {WarehouseOfCampaignState} from 'shared/types/warehouseOfCampaign';
import {ASSORTMENT_DATA_SOURCE, MOCK_EMPTY_MEASUREMENTS, CATEGORY_CHANGE_STATUS} from 'shared/constants/assortment';

type ErrorModalWarehousesBubblePageState = Pick<
    PageState,
    'businessId' | 'offerErrors' | 'placementModels' | 'warehouseOfCampaign' | 'feature'
> & {
    offers: Partial<OffersState>;
};

const OFFER_ID = '60047186004723';
const SHOP_ID = 10783476;
const CAMPAIGN_ID = 1001091826;
const BUSINESS_ID = 10785678;

const createBasePageState = (): ErrorModalWarehousesBubblePageState => ({
    offers: {
        servicePartsByOfferIds: {},
        entities: {
            offers: {
                [OFFER_ID]: {
                    id: OFFER_ID,
                    name: '',
                    description: '',
                    categoryName: '',
                    marketApprovedCategoryName: '',
                    marketCategoryChangeStatus: CATEGORY_CHANGE_STATUS.forbidden,
                    mainPictureUrl: {
                        url: null,
                        status: 1,
                    },
                    prices: {
                        commonPrice: '777',
                        lastValidPrice: '',
                        priceWithoutDiscount: '',
                        currency: 'RUR',
                        isLastEditedFromLinkFeed: false,
                        vat: 7,
                    },
                    publishingStatus: 1,
                    offerStatus: 8,
                    contentStatus: 6,
                    isContentStatusActual: true,
                    warehouseId: '48585',
                    wareMd5: '',
                    availability: 2,
                    enrichedOfferModelId: 0,
                    partnerStock: '',
                    partnerStockTimestamp: null,
                    marketStock: '',
                    marketStockTimestamp: null,
                    isStockFromLinkFeed: false,
                    measurements: MOCK_EMPTY_MEASUREMENTS,
                    pictureActualUrlToSourceUrlMap: {},
                },
            },
        },
    },
    businessId: BUSINESS_ID,
    offerErrors: {
        offerErrorsModal: {
            openedOfferId: OFFER_ID,
            isSolveProblemInProgress: false,
            isSolvePopupOpen: false,
        },
        errorStateByOfferId: {
            [OFFER_ID]: {
                uniqVerdicts: {
                    errors: [],
                    warnings: [],
                },
                selectedCampaignId: SHOP_ID,
                selectedLevel: 'ALL',
                currentItem: 1,
                common: {
                    errors: [],
                    warnings: [],
                    hasCommonGlobalError: false,
                },
                byCampaign: {
                    [SHOP_ID]: {
                        errors: [],
                        warnings: [],
                    },
                },
            },
        },
        status: 'done',
        offersContentErrors: {},
    },
    placementModels: {
        serviceParts: [],
        detailsByModel: {
            [SHOP_ID]: {
                partnerApplication: {
                    status: '2',
                    vatInfo: {
                        taxSystem: 0,
                        vat: 7,
                        vatSource: 1,
                        deliveryVat: 7,
                    },
                },
                isUsingLinkFeed: false,
                homeRegionName: 'Москва',
                isUsingApiStocks: false,
                isUsingPriceStockLinkFeed: false,
                warehouseId: 48585,
            },
        },
        models: [
            {
                internalName: 'Фиолетовый',
                partnerId: SHOP_ID,
                campaignId: CAMPAIGN_ID,
                businessId: BUSINESS_ID,
                type: 'SUPPLIER',
                placementTypes: ['FULFILLMENT', 'CROSSDOCK'],
                clientId: 1352044292,
                connected: false,
                model: 'CROSSDOCK',
            },
        ],
        status: 'done',
    },
    warehouseOfCampaign: {
        warehouse: {},
        warehousesIdsByCampaignId: {},
    },
    feature: {
        allowedFeatureFlags: {
            allowedSolveCrossdockOrDropshipProblems: true,
            allowedSolveMigrationProblems: true,
            allowedSolveNotDropshipAndNotCrossdockProblems: false,
        },
    },
});

type GetStateWithErrorsParams = {
    errors: ErrorItem[];
    warehouseOfCampaign: WarehouseOfCampaignState;
};

const getStateWithErrors = ({
    errors,
    warehouseOfCampaign,
}: GetStateWithErrorsParams): ErrorModalWarehousesBubblePageState => {
    const state = createBasePageState();
    const offerErrorState = state.offerErrors.errorStateByOfferId[OFFER_ID];

    offerErrorState.byCampaign[SHOP_ID].errors = errors;
    offerErrorState.uniqVerdicts.errors = errors;

    state.warehouseOfCampaign = warehouseOfCampaign;

    return state;
};

const verdictWithSingleWarehouseId: ErrorItem = {
    type: 'VERDICT',
    isRelevant: true,
    dataSource: ASSORTMENT_DATA_SOURCE.UNKNOWN_SOURCE,
    item: {
        namespace: 'mboc.ci.error',
        code: 'mboc.error.excel-value-is-required',
        params: [
            {
                name: 'header',
                value: 'Вес в упаковке в килограммах',
            },
        ],
        text: "Отсутствует значение для колонки 'Вес в упаковке в килограммах'",
        level: 3,
        details: '{"header":"Вес в упаковке в килограммах"}',
    },
    relatedShopIds: [SHOP_ID],
    relatedWarehouseIds: [48585],
};

const pageState1: ErrorModalWarehousesBubblePageState = getStateWithErrors({
    errors: [verdictWithSingleWarehouseId],
    warehouseOfCampaign: {
        warehouse: {
            '48585': {
                id: 48585,
                name: 'Фиолетовый FBY+',
            },
        },
        warehousesIdsByCampaignId: {
            [CAMPAIGN_ID]: [48585],
        },
    },
});

const verdictWithMultipleWarehouseId: ErrorItem = {
    type: 'VERDICT',
    isRelevant: true,
    dataSource: ASSORTMENT_DATA_SOURCE.UNKNOWN_SOURCE,
    item: {
        namespace: 'mboc.ci.error',
        code: 'mboc.error.excel-value-is-required',
        params: [
            {
                name: 'header',
                value: 'Вес в упаковке в килограммах',
            },
        ],
        text: "Отсутствует значение для колонки 'Вес в упаковке в килограммах'",
        level: 3,
        details: '{"header":"Вес в упаковке в килограммах"}',
    },
    relatedShopIds: [SHOP_ID],
    relatedWarehouseIds: [48585, 48586],
};

const pageState2: ErrorModalWarehousesBubblePageState = getStateWithErrors({
    errors: [verdictWithMultipleWarehouseId],
    warehouseOfCampaign: {
        warehouse: {
            '48585': {
                id: 48585,
                name: 'Склад 1',
            },
            '48586': {
                id: 48586,
                name: 'Склад 2',
            },
        },
        warehousesIdsByCampaignId: {
            [CAMPAIGN_ID]: [48585, 48586],
        },
    },
});

const confirmPriceVerdict: ErrorItem = {
    type: 'VERDICT',
    isRelevant: true,
    dataSource: ASSORTMENT_DATA_SOURCE.UNKNOWN_SOURCE,
    item: {
        namespace: 'shared.indexer.error.codes',
        code: '49w',
        params: [
            {
                name: 'diffPrice',
                value: '90%',
            },
            {
                name: 'oldPrice',
                value: '50000',
            },
            {
                name: 'code',
                value: '49w',
            },
            {
                name: 'newPrice',
                value: '5000',
            },
        ],
        level: 3,
        details: '{"diffPrice":"90%","oldPrice":"50000","code":"49w","newPrice":"5000"}',
    },
    relatedShopIds: [SHOP_ID],
    relatedWarehouseIds: [],
    relateShopParams: [mockType({campaignId: 123, businessId: 456, partnerId: SHOP_ID})],
};

const pageState3: ErrorModalWarehousesBubblePageState = getStateWithErrors({
    errors: [confirmPriceVerdict],
    warehouseOfCampaign: {
        warehouse: {},
        warehousesIdsByCampaignId: {},
    },
});

export {pageState1, pageState2, pageState3};
