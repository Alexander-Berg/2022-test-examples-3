import {assocPath, pipe} from 'ramda';

import {TABS} from 'shared/pages/Assortment';

import {createCommonPageState} from './dataCreators/createCommonPageState';

const pageState = pipe(assocPath(['offers', 'selectedTab'], TABS.priceConfirmation))(createCommonPageState());

export const globalState = {
    page: pageState,
    params: {
        activeTab: TABS.priceConfirmation,
    },
    campaign: {},
};
