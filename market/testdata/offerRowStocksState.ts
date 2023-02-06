import {mockType} from 'spec/jest';
import {
    MOCK_EMPTY_MEASUREMENTS,
    OFFER_SHOW_CASE_PRICES_DEFAULT_STATE,
    CATEGORY_CHANGE_STATUS,
} from 'shared/constants/assortment';

import type {OffersState, PageState, OfferDto} from 'shared/pages/Assortment';
import {PLACEMENT_TYPE, RESULT_STATUS} from '@yandex-market/b2b-core/shared/constants';
import {CARD_STATUS} from 'shared/constants/datacamp';

export type OfferRowStocksPageState = Pick<
    PageState,
    | 'businessId'
    | 'offerErrors'
    | 'placementModels'
    | 'warehouseOfCampaign'
    | 'feature'
    | 'offerShowCasePrices'
    | 'settings'
    | 'tariffsSettings'
    | 'offerChangesHistoryReportState'
    | 'contentTemplates'
    | 'migrationStatus'
    | 'batchAddPlacementModal'
    | 'processMultipleOffers'
> & {
    offers: Partial<OffersState>;
};

export const OFFER_ID = '60047186004723';
const SHOP_ID = 10265019;
const CAMPAIGN_ID = 1001091826;
const BUSINESS_ID = 10785678;

const createBasePageState = (offer: OfferDto): OfferRowStocksPageState => ({
    offerShowCasePrices: OFFER_SHOW_CASE_PRICES_DEFAULT_STATE,
    offerChangesHistoryReportState: {},
    settings: {
        stocksVisibleForModels: Object.values(PLACEMENT_TYPE),
        safeNumberOfOffersCountToDisplayLastPage: 10000,
        enableSaasCreationTsSortRelevance: true,
        isBusinessAssortmentActive: false,
        isAssortmentErrorFridgeEnabled: false,
        isPriceQuarantineActive: false,
    },
    tariffsSettings: {
        enableTariffs: false,
        enableAdditionalTariffs: false,
        displayedSecondaryTariffs: {
            fulfillment: [],
            dropship: [],
            dropshipBySeller: [],
            express: [],
        },
    },
    contentTemplates: mockType({}),
    offers: {
        paging: mockType({}),
        replyPendingOffers: {},
        savedOffers: {},
        offersWithPriceBeingEdited: {},
        offersWithPartnerStockBeingEdited: {},
        servicePartsByOfferIds: {
            [OFFER_ID]: [SHOP_ID],
        },
        offers: [OFFER_ID],
        entities: {
            offers: {
                [OFFER_ID]: offer,
            },
        },
        offersCount: {
            errorAndWarningGroups: {status: 'done'},
            archive: {count: null, status: 'done'},
            currentPlacement: {count: null, status: 'done'},
            priceConfirmation: {count: null, status: 'done'},
            offers: {count: null, status: 'done'},
            otherPlacement: {count: null, status: 'done'},
            withErrors: {count: null, status: 'done'},
            noStocks: {count: null, status: 'done'},
        },
    },
    processMultipleOffers: {
        actionType: null,
        offerIdsForAction: [],
        isModalOpen: false,
        modalStatus: 'done',
        isActionBarActive: false,
        isSingleAction: false,
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
                isUsingPriceStockLinkFeed: false,
                homeRegionName: 'Москва',
                isUsingApiStocks: false,
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
    migrationStatus: {switched: false},
    batchAddPlacementModal: {
        isOpen: false,
        isHeaderModalActive: false,
    },
});

export const baseState = createBasePageState({
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
    partnerStock: '1',
    partnerStockTimestamp: 423423,
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithCardAndEmptyStocks = createBasePageState({
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
    contentStatus: 1,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: '',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '',
    partnerStockTimestamp: null,
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithCardAndRecentlyUpdatedStock = createBasePageState({
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
    contentStatus: 1,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: '',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '12',
    partnerStockTimestamp: Date.now(),
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithCardAndWayAgoUpdatedStock = createBasePageState({
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
    contentStatus: 1,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: '',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '12',
    partnerStockTimestamp: Date.now() - 40 * 60 * 1000,
    marketStock: '12',
    marketStockTimestamp: Date.now() - 16 * 60 * 1000,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithPublishedStatusAndCard = createBasePageState({
    id: OFFER_ID,
    name: 'OfferName',
    description: '',
    categoryName: 'Category',
    marketApprovedCategoryName: 'Category',
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
    offerStatus: RESULT_STATUS.PUBLISHED,
    contentStatus: CARD_STATUS.HAS_CARD_MARKET,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: 'XlnsRO9Mrf94hjPE0PrxnQ',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '',
    partnerStockTimestamp: null,
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithPublishedStatusAndNoCard = createBasePageState({
    id: OFFER_ID,
    name: 'OfferName',
    description: '',
    categoryName: 'Category',
    marketApprovedCategoryName: 'Category',
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
    offerStatus: RESULT_STATUS.PUBLISHED,
    contentStatus: CARD_STATUS.NO_CARD_CANT_CREATE,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: 'XlnsRO9Mrf94hjPE0PrxnQ',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '',
    partnerStockTimestamp: null,
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithNotPublishedStatusAndCard = createBasePageState({
    id: OFFER_ID,
    name: 'OfferName',
    description: '',
    categoryName: 'Category',
    marketApprovedCategoryName: 'Category',
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
    offerStatus: RESULT_STATUS.NOT_PUBLISHED_DISABLED_AUTOMATICALLY,
    contentStatus: CARD_STATUS.HAS_CARD_MARKET,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: '',
    marketSku: '100861795160',
    marketModelId: '100861795160',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '',
    partnerStockTimestamp: null,
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});

export const baseStateWithNotPublishedStatusAndNoCard = createBasePageState({
    id: OFFER_ID,
    name: 'OfferName',
    description: '',
    categoryName: 'Category',
    marketApprovedCategoryName: 'Category',
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
    offerStatus: RESULT_STATUS.NOT_PUBLISHED_DISABLED_AUTOMATICALLY,
    contentStatus: CARD_STATUS.NO_CARD_NEED_CONTENT,
    isContentStatusActual: true,
    warehouseId: '48585',
    wareMd5: '',
    marketSku: '100861795160',
    marketModelId: '100861795160',
    availability: 2,
    enrichedOfferModelId: 0,
    partnerStock: '',
    partnerStockTimestamp: null,
    marketStock: '',
    marketStockTimestamp: null,
    isStockFromLinkFeed: true,
    measurements: MOCK_EMPTY_MEASUREMENTS,
    pictureActualUrlToSourceUrlMap: {},
});
