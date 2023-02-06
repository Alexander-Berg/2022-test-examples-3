import {IResultAviaVariant} from 'selectors/avia/utils/denormalization/variant';

import {
    getAviaOfferType,
    EAviaOfferType,
} from 'projects/avia/lib/search/offerType';

const NON_CHARTER_FLIGHT = {
    price: {
        charter: false,
    },
} as IResultAviaVariant;
const CHARTER_FLIGHT = {
    price: {
        charter: true,
    },
} as IResultAviaVariant;

describe('getAviaOfferType', () => {
    it('нет чартерных предложений - вернём null', () => {
        expect(
            getAviaOfferType([NON_CHARTER_FLIGHT, NON_CHARTER_FLIGHT]),
        ).toBeUndefined();
    });

    it('есть обычные и чартерные предложения - вернём "спецпредложение"', () => {
        expect(getAviaOfferType([NON_CHARTER_FLIGHT, CHARTER_FLIGHT])).toBe(
            EAviaOfferType.special,
        );
    });

    it('есть только чартерные предложения - вернём "чартер"', () => {
        expect(getAviaOfferType([CHARTER_FLIGHT, CHARTER_FLIGHT])).toBe(
            EAviaOfferType.charter,
        );
    });
});
