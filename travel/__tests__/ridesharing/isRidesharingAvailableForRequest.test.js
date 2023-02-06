import {RU, BY} from '../../../countries';

import isRidesharingAvailableForRequest from '../../ridesharing/isRidesharingAvailableForRequest';

const ruPoint = {country: {code: RU}};
const byPoint = {country: {code: BY}};
const flags = {__ridesharingPartnersDisabled: false};

describe('isRidesharingAvailableForRequest', () => {
    it('Вернёт false если отсутствует один из пунктов', () => {
        expect(
            isRidesharingAvailableForRequest({
                from: ruPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(false);
        expect(
            isRidesharingAvailableForRequest({
                to: ruPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(false);
    });

    it('Вернёт false если осуществляется поиск на прошлую дату', () => {
        expect(
            isRidesharingAvailableForRequest({
                from: ruPoint,
                to: ruPoint,
                searchForPastDate: true,
                flags,
            }),
        ).toBe(false);
    });

    it('Вернёт false если обе точки находятся в Беларуси', () => {
        expect(
            isRidesharingAvailableForRequest({
                from: byPoint,
                to: byPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(false);
    });

    it('Вернёт true для любых других поисков с актуальной датой', () => {
        expect(
            isRidesharingAvailableForRequest({
                from: byPoint,
                to: ruPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(true);
        expect(
            isRidesharingAvailableForRequest({
                from: ruPoint,
                to: byPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(true);
        expect(
            isRidesharingAvailableForRequest({
                from: ruPoint,
                to: ruPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(true);
    });

    it('Вернёт false если ББК выключен', () => {
        expect(
            isRidesharingAvailableForRequest({
                from: byPoint,
                to: ruPoint,
                searchForPastDate: false,
                flags,
            }),
        ).toBe(true);
        expect(
            isRidesharingAvailableForRequest({
                from: byPoint,
                to: ruPoint,
                searchForPastDate: false,
                flags: {__ridesharingPartnersDisabled: true},
            }),
        ).toBe(false);
    });
});
