import {STATUS} from '@yandex-market/b2b-core/shared/types/status';

import type {PageState} from 'shared/pages/Assortment';

import {OFFER_SHOW_CASE_PRICES_DEFAULT_STATE, OFFER_TARIFFS_DEFAULT_STATE} from 'shared/constants/assortment';

import {createEmptyPlacementModels} from 'shared/widgets/LoadPlacementModels/spec/testData/dataCreators';

import {createEmptyOffersState} from './offersState';
import {createEmptyOfferErrorsState} from './offerErrorsState';
import {createDefaultSettingsState} from './settingsState';
import {createProcessMultipleOffersState} from './processMultipleOffersState';
import {createDefaultTariffState} from './tariffsSettings';

const BUSINESS_ID = 10785678;

export const createCommonPageState = (): PageState => ({
    businessId: BUSINESS_ID,
    isUsingPriceStockLinkFeed: false,
    contentTemplates: {
        isPopupOpen: false,
        status: STATUS.done,
    },
    feature: {
        allowedFeatureFlags: {
            allowedSolveCrossdockOrDropshipProblems: true,
            allowedSolveMigrationProblems: true,
            allowedSolveNotDropshipAndNotCrossdockProblems: true,
        },
    },
    offers: createEmptyOffersState(),
    processMultipleOffers: createProcessMultipleOffersState(),
    batchAddPlacementModal: {
        isOpen: false,
        isHeaderModalActive: false,
    },
    priceWarningModal: {
        i18nParams: {change: null, quantity: null},
        isOpen: false,
        offerId: '',
    },
    offerErrors: createEmptyOfferErrorsState(),
    offerShowCasePrices: OFFER_SHOW_CASE_PRICES_DEFAULT_STATE,
    offerTariffs: OFFER_TARIFFS_DEFAULT_STATE,
    migrationStatus: {switched: false},
    countSskuDynamicPriceOffers: 0,
    priceRecommendations: {},
    settings: createDefaultSettingsState(),
    tariffsSettings: createDefaultTariffState(),
    warehouseOfCampaign: {
        warehouse: {},
        warehousesIdsByCampaignId: {},
    },
    offerChangesHistoryReportState: {},
    warehouseGroup: null,
    placementModels: createEmptyPlacementModels(),
});
