import {STATUS} from '@yandex-market/b2b-core/shared/types/status';

import type {MultipleOffersActionsState} from 'shared/pages/Assortment';

export const createProcessMultipleOffersState = (): MultipleOffersActionsState => ({
    actionType: null,
    offerIdsForAction: [],
    isModalOpen: false,
    modalStatus: STATUS.done,
    isActionBarActive: false,
    isSingleAction: false,
});
