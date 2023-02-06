import {TABS} from 'shared/pages/Assortment';
import {STATUS} from '@yandex-market/b2b-core/shared/types/index';

export const globalState = {
    page: {
        offers: {
            filters: {
                errorAndWarningGroups: [
                    {
                        identifier: 'addImage',
                        type: 'error',
                        offersCount: 15,
                        errors: ['1', '2'],
                        errorCodes: [1, 2],
                    },
                    {
                        identifier: 'notAppropriateImage',
                        type: 'warning',
                        offersCount: 30,
                        errors: ['1', '2'],
                        errorCodes: [1, 2],
                    },
                ],
            },
            offersCount: {
                errorAndWarningGroups: {status: STATUS.done},
            },
        },
    },
    params: {
        activeTab: TABS.withErrors,
        errorAndWarningFilter: '',
    },
    campaign: {},
};
