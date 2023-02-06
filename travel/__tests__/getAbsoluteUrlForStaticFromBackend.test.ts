import {PRODUCTION_PARTNER_LOGO_SRC, DEV_PARTNER_LOGO_SRC} from '../constants';

import getAbsoluteUrlForStaticFromBackend from '../getAbsoluteUrlForStaticFromBackend';

const pathName = 'data/img1.png';

describe('getPartnerLogoUrl', () => {
    it('Вернёт url лого для прода', () => {
        expect(getAbsoluteUrlForStaticFromBackend(pathName, true)).toBe(
            PRODUCTION_PARTNER_LOGO_SRC + pathName,
        );
    });

    it('Вернёт url лого для всего остального', () => {
        expect(getAbsoluteUrlForStaticFromBackend(pathName, false)).toBe(
            DEV_PARTNER_LOGO_SRC + pathName,
        );
    });
});
