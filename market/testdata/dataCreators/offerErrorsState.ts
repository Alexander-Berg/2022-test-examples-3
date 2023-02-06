import {STATUS} from '@yandex-market/b2b-core/shared/types/status';

import type {OfferErrorsState} from 'shared/pages/Assortment';

export const createEmptyOfferErrorsState = (): OfferErrorsState => ({
    offerErrorsModal: {
        openedOfferId: null,
        isSolveProblemInProgress: false,
        isSolvePopupOpen: false,
    },
    errorStateByOfferId: {},
    status: STATUS.done,
    offersContentErrors: {},
});
