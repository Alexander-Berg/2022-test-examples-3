import type {OffersState, PageState} from 'shared/pages/Assortment';
import {TABS} from 'shared/pages/Assortment';
import {CATEGORY_CHANGE_STATUS} from 'shared/constants/assortment';

import {createCommonPageState} from './dataCreators/createCommonPageState';
import {createEmptyFilters, createEmptyOffersState} from './dataCreators/offersState';

const offersState: OffersState = {
    ...createEmptyOffersState(),
    entities: {
        offers: {
            'ADV-1': {
                id: 'ADV-1',
                name: '111;lfkg;lxk',
                description: '1111',
                categoryName: 'Test Category 0',
                marketApprovedCategoryName: 'Test Category 0',
                marketCategoryChangeStatus: CATEGORY_CHANGE_STATUS.available,
                marketCategoryId: '16371365',
                mainPictureUrl: {
                    url:
                        '//avatars.mds.yandex.net/get-marketpictesting/5611959/pic66e3b7d54c4331eafdc847054274b405/orig',
                    status: 1,
                },
                prices: {
                    commonPrice: '2000',
                    lastValidPrice: '200',
                    priceWithoutDiscount: '',
                    currency: 'RUR',
                    isLastEditedFromLinkFeed: true,
                    vat: 7,
                },
                publishingStatus: 3,
                offerStatus: 4,
                contentStatus: 2,
                isContentStatusActual: true,
                warehouseId: '147',
                wareMd5: 'Yg_hyh4yIbB5N_QEgLdCJQ',
                availability: 2,
                marketSku: '100870238291',
                marketModelId: '100870238291',
                enrichedOfferModelId: 0,
                filled: 15,
                filledExpected: 15,
                measurements: {
                    lengthSm: '10',
                    widthSm: '10',
                    heightSm: '10',
                    weightKg: '6',
                },
                marketSkuType: 3,
                pictureActualUrlToSourceUrlMap: {
                    '//avatars.mds.yandex.net/get-marketpictesting/5611959/pic66e3b7d54c4331eafdc847054274b405/orig':
                        '//avatars.mds.yandex.net/get-marketpictesting/5611959/pic66e3b7d54c4331eafdc847054274b405/orig',
                },
                partnerStock: '',
                partnerStockTimestamp: null,
                marketStock: '',
                marketStockTimestamp: null,
                isStockFromLinkFeed: true,
            },
            'fashion-block-17': {
                id: 'fashion-block-17',
                name: 'Брюки EMKA White L US',
                description: 'брюки GG',
                categoryName: 'брюки',
                marketApprovedCategoryName: 'брюки',
                marketCategoryChangeStatus: CATEGORY_CHANGE_STATUS.available,
                marketCategoryId: '7811903',
                mainPictureUrl: {
                    url:
                        '//avatars.mds.yandex.net/get-marketpictesting/5611959/pic66e3b7d54c4331eafdc847054274b405/orig',
                    status: 1,
                },
                prices: {
                    commonPrice: '10000',
                    lastValidPrice: '',
                    priceWithoutDiscount: '',
                    currency: 'RUR',
                    isLastEditedFromLinkFeed: true,
                    vat: 7,
                },
                publishingStatus: 3,
                offerStatus: 4,
                contentStatus: 8,
                isContentStatusActual: true,
                warehouseId: '147',
                wareMd5: '',
                availability: 2,
                enrichedOfferModelId: 0,
                measurements: {
                    lengthSm: '20',
                    widthSm: '20',
                    heightSm: '1',
                    weightKg: '0.2',
                },
                pictureActualUrlToSourceUrlMap: {},
                partnerStock: '',
                partnerStockTimestamp: null,
                marketStock: '',
                marketStockTimestamp: null,
                isStockFromLinkFeed: true,
            },
        },
    },
    offers: ['ADV-1', 'fashion-block-17'],
    servicePartsByOfferIds: {
        'ADV-1': [11346543],
        'fashion-block-17': [11346543],
    },
    filters: {
        ...createEmptyFilters(),
        priceErrorCode: 310584098,
    },
    selectedTab: TABS.priceConfirmation,
    paging: {
        currentPage: 1,
        offersPerPage: 20,
        totalOffers: 2,
    },
};

const pageState: PageState = {
    ...createCommonPageState(),
    offers: offersState,
};

export const globalState = {
    page: pageState,
    params: {
        activeTab: TABS.priceConfirmation,
    },
    campaign: {
        datasourceId: 11346543,
    },
};
