import type {UnitedOffer} from '~/app/bcm/datacamp/Backend/types';
import type {BusinessOffersResult} from '~/app/bcm/datacamp/Client/StrollerClient/types';

import getUnitedOffer from './getUnitedOffer';

export default (draft: Partial<UnitedOffer> = {}, shopId?: number): BusinessOffersResult => ({
    offers: [getUnitedOffer(draft, shopId)],
    limit: 10,
    offset: 0,
    total: 1,
});
