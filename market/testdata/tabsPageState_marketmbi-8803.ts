import {STATUS} from '@yandex-market/b2b-core/shared/types/status';

import type {PageState} from 'shared/pages/Assortment';
import type {PlacementModelsState} from 'shared/widgets/LoadPlacementModels';

import {createEmptyOffersState} from './dataCreators/offersState';
import {createCommonPageState} from './dataCreators/createCommonPageState';

const shopId = 11346543;

const placementModels: PlacementModelsState = {
    models: [
        {
            internalName: 'новый светлый',
            partnerId: shopId,
            campaignId: 1001552259,
            businessId: 11163178,
            type: 'SUPPLIER',
            placementTypes: ['FULFILLMENT'],
            clientId: 1351874000,
            connected: false,
            model: 'FULFILLMENT',
        },
    ],
    serviceParts: [],
    detailsByModel: {},
    status: STATUS.done,
};

const pageState: PageState = {
    ...createCommonPageState(),
    offers: {
        ...createEmptyOffersState(),
        offersCount: {
            currentPlacement: {
                status: STATUS.done,
                count: 80,
            },
            errorAndWarningGroups: {
                status: STATUS.done,
            },
            otherPlacement: {
                status: STATUS.done,
                count: 1515,
            },
            archive: {
                status: STATUS.done,
                count: 2,
            },
            offers: {
                status: STATUS.done,
                count: null,
            },
            priceConfirmation: {
                status: STATUS.done,
                count: 4,
            },
            withErrors: {
                status: STATUS.done,
                count: 3,
            },
            noStocks: {
                status: STATUS.done,
                count: 4,
            },
        },
        selectedPlacement: 'CURRENT',
    },
    placementModels,
};

export const globalState = {
    page: pageState,
    campaign: {
        datasourceId: shopId,
    },
};
