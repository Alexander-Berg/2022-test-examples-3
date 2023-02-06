import {createCheckDeliveryAvailableState} from '@yandex-market/kadavr/mocks/Report/helpers/region';
import {
    moscow,
} from '@self/root/src/spec/hermione/kadavr-mock/report/region';

export const MINIMAL_DATA_STATE = {
    data: {
        search: {
            total: 1,
        },
    },
};

export const GRID_VIEW_DATA_STATE = {
    data: {
        search: {
            view: 'grid',
        },
    },
};

export const LIST_VIEW_DATA_STATE = {
    data: {
        search: {
            view: 'list',
        },
    },
};

export const AVAILABLE_DELIVERY_STATE = createCheckDeliveryAvailableState({
    regions: [
        moscow,
    ],
});
