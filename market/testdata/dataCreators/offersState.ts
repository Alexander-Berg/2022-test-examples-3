import {STATUS} from '@yandex-market/b2b-core/shared/types/status';

import type {OffersState, FiltersProps} from 'shared/pages/Assortment';

import {TABS} from 'shared/pages/Assortment';

export const createEmptyFilters = (): FiltersProps => ({
    categories: {
        rootId: {
            id: 'rootId',
            name: 'rootId',
            children: [],
        },
    },
    contentStatuses: {
        rootId: {
            id: 'rootId',
            name: 'rootId',
            children: [],
        },
    },
    offerStatuses: {
        rootId: {
            id: 'rootId',
            name: 'rootId',
            children: [],
        },
    },

    errorAndWarningGroups: [],
    errorAndWarningMapForEncode: {
        groups: {},
    },
    priceErrorCode: 1234,
});

export const createEmptyOffersState = (): OffersState => ({
    replyPendingOffers: {},
    savedOffers: {},
    offersWithPriceBeingEdited: {},
    offersWithPartnerStockBeingEdited: {},
    servicePartsByOfferIds: {},
    offers: [],
    paging: {currentPage: 1, offersPerPage: 20, totalOffers: 0},
    entities: {
        offers: {},
    },
    offersCount: {
        errorAndWarningGroups: {status: STATUS.done},
        archive: {count: null, status: STATUS.done},
        currentPlacement: {count: null, status: STATUS.done},
        priceConfirmation: {count: null, status: STATUS.done},
        offers: {count: null, status: STATUS.done},
        otherPlacement: {count: null, status: STATUS.done},
        withErrors: {count: null, status: STATUS.done},
        noStocks: {count: null, status: STATUS.done},
    },
    status: STATUS.done,

    filters: createEmptyFilters(),

    selectedTab: TABS.offers,
    stocksEnabled: true,
});
