import type {TariffsSettings} from 'shared/settings/tariffs';

export const createDefaultTariffState = (): TariffsSettings => ({
    enableTariffs: false,
    enableAdditionalTariffs: false,
    displayedSecondaryTariffs: {
        fulfillment: [],
        dropship: [],
        dropshipBySeller: [],
        express: [],
    },
});
