import {mockType} from 'spec/jest';

import {MAPPING_PARTNER_DECISION, PARTNER_PROCESSING_STATE} from '@yandex-market/b2b-core/shared/constants/datacamp';
import type {MappingPartnerDecision} from 'shared/bcm/indexator/datacamp/types';

import type {PageState, PreparedBusinessOffersData} from 'shared/pages/BusinessAssortment/types';

export const OFFER_ID = '777';
export const mockedCategoriesList = {
    '22': {
        children: [],
        id: '22',
        isLeaf: true,
        name: 'Цифровая техника',
        parentId: 'rootId',
    },
    rootId: {
        children: ['22'],
        id: 'rootId',
        isLeaf: false,
        name: 'rootId',
    },
};
export const mockedContentStatusesTree = {
    '1': {
        id: '1',
        name: '1',
        parentId: 'rootId',
        isLeaf: true,
    },
    rootId: {
        id: 'rootId',
        name: 'rootId',
        children: ['1'],
    },
};

export const basePageState: PageState = {
    offers: {
        offerInfoByOfferId: {},
        offerIds: [],
        status: 'done',
    },
    placementModels: mockType({models: []}),
    offersErrors: {
        modal: {
            openedOfferId: null,
            isSolveProblemInProgress: false,
            isSolvePopupOpen: false,
        },
        errorStateByOfferId: {},
        status: 'done',
        offersContentErrorsByOfferId: {},
    },
    warehouses: {
        warehousesDataByWarehouseId: {},
        warehousesIdsByCampaignId: {},
    },
    features: {
        allowedSolveCrossdockOrDropshipProblems: true,
        allowedSolveMigrationProblems: true,
        allowedSolveNotDropshipAndNotCrossdockProblems: true,
    },
    filter: {
        contentStatuses: mockedContentStatusesTree,
        errorAndWarningMapForEncode: {groups: {}},
        categories: mockedCategoriesList,
    },
    settings: {
        safeNumberOfOffersCountToDisplayLastPage: 5000,
        maxCategoryItemsToShowCount: 1000,
        enableSaasCreationTsSortRelevance: true,
        showMappingsTab: true,
        dateFilterSettings: true,
        contentTemplatesIsEnable: true,
        isCategoryChangeAvailable: true,
        isAssortmentErrorFridgeEnabled: false,
        isAssortmentAddDrawerVisible: false,
    },
    processMultipleOffers: {
        actionType: null,
        isModalOpen: false,
        modalStatus: 'done',
        offersIdsForAction: [],
        isActionBarActive: false,
        isSingleAction: false,
    },
    modelsRecommendationsStatus: 'done',
    feedLink: {
        precessingStatus: 'done',
        hasCampaignWithFeedLink: false,
    },
    reports: {
        isGenerated: false,
        report: null,
        reportType: null,
    },
    contentTemplates: {
        isPopupOpen: false,
        status: 'done',
    },
    tabs: {
        offersCount: {
            offers: {
                status: 'done',
                count: 18,
            },
            mappings: {
                status: 'done',
                count: 3,
            },
        },
        selectedTab: 'mappings',
    },
    tariffs: {},
};

export const mockResolvedBusinessOffersData = mockType<PreparedBusinessOffersData>({
    offerInfoByOfferId: {
        [OFFER_ID]: {
            id: OFFER_ID,
            name: 'Boneco U650, белый',
            categoryName: 'Бытовая техника',
            mainPictureData: {
                url: '//avatars.mds.yandex.net/get-mpic/96484/img_id6044325936561335404/200x200',
                status: 1,
            },
            contentStatus: 1,
            marketSku: '100439210800',
            marketModelId: '217372756',
            servicePartsIds: [],
            servicePartsInfoByShopId: {},
            offerModels: {},
            servicePrices: {},
            isRemovingOffer: false,
            pictureActualUrlToSourceUrlMap: {},
            marketPicture: {
                url: '//avatars.mds.yandex.net/get-mpic/96484/img_id6044325936561335404/200x200',
                status: 1,
            },
            marketTitle: 'Boneco U650, белый',
            marketCategoryName: 'Очистители и увлажнители воздуха',
            mappingProcessingState: 1,
        },
    },
    offerIds: [OFFER_ID],
    offersPaging: {
        currentPage: 1,
        offersPerPage: 50,
        totalOffers: 18,
    },
    offersContentErrorsByOfferId: {},
});

const createProcessMappingResolverResponse = (decision: MappingPartnerDecision) => ({
    [OFFER_ID]: {
        mappingProcessingDecision: decision,
        mappingProcessingState: PARTNER_PROCESSING_STATE.PROCESSED,
    },
});

export const denyMappingResolverResponse = createProcessMappingResolverResponse(MAPPING_PARTNER_DECISION.DENY);
export const approveMappingResolverResponse = createProcessMappingResolverResponse(MAPPING_PARTNER_DECISION.APPROVE);
