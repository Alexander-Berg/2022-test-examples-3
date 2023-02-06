import {
    checkTooltipIsNotCreated,
} from '@self/root/src/widgets/content/PromoTooltip/common/__spec__/checkMethods';

import {MOCKS_DATA} from '../mockData';

export const TEST_CASES = [{
    caseName: 'В таче не отображается',
    mockData: MOCKS_DATA.plus_with_balance_with_delivery,
    method: checkTooltipIsNotCreated,
}];
