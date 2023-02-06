import type {PageSettings} from 'shared/pages/Assortment';

export const createDefaultSettingsState = (): PageSettings => ({
    stocksVisibleForModels: [],
    safeNumberOfOffersCountToDisplayLastPage: 10000,
    enableSaasCreationTsSortRelevance: true,
    isBusinessAssortmentActive: true,
    isAssortmentErrorFridgeEnabled: true,
    isPriceQuarantineActive: true,
});
