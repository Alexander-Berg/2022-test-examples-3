import CountryCode from '../../../../interfaces/state/transport/CountryCode';

import isRidesharingAvailableForDisplay from '../../ridesharing/isRidesharingAvailableForDisplay';

const ruPoint = {country: {code: CountryCode.ru}};
const byPoint = {country: {code: CountryCode.by}};

const points = {
    from: ruPoint,
    to: ruPoint,
};
const flags = {__ridesharingPartnersDisabled: false};

describe('isRidesharingAvailableForDisplay', () => {
    it('Должен вернуть true для поиска с результатами на не прошедшую дату', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 13,
            },
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(true);
    });

    it('Должен вернуть false для поиска с результатами на прошедшую дату', () => {
        const context = {
            searchForPastDate: true,
            ...points,
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 13,
            },
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(false);
    });

    it('Должен вернуть true если идёт опрос блаблакара', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {},
        };
        const blablacar = {
            querying: true,
            tariff: null,
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(true);
    });

    it('Должен вернуть true если нет результатов на дату, но не закончен опрос на все дни', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 0,
            },
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(true);
    });

    it('Должен вернуть false если нет результатов на дату, но опрос на все дни вернул не null', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 0,
            },
            allDaysCheckComplete: true,
            allDaysCheckResult: {},
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(false);
    });

    it('Должен вернуть true если нет результатов на дату, но опрос на все дни вернул null', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 0,
            },
            allDaysCheckComplete: true,
            allDaysCheckResult: null,
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(false);
    });

    // Ручка для запроса на все дни, если поездок нет, возвращает null,
    // для поиска на дату - структуру с offersCount: 0
    it('Должен вернуть false если в tariff вернулся null', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {
                special: 'all-days',
            },
        };
        const blablacar = {
            querying: false,
            tariff: null,
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(false);
    });

    it('Должен вернуть false если блаблакар вернул "banned: true"', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {
                special: 'all-days',
            },
        };
        const blablacar = {
            querying: false,
            tariff: {},
            banned: true,
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(false);
    });

    it('Должен вернуть false для поиска в котором обе точки находятся в Беларуси', () => {
        const context = {
            searchForPastDate: false,
            from: byPoint,
            to: byPoint,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 2,
            },
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(false);
    });

    it('Должен вернуть true для поиска в котором 1 точка находится в Беларуси', () => {
        const contextFromBy = {
            searchForPastDate: false,
            from: byPoint,
            to: ruPoint,
            when: {},
        };
        const contextToBy = {
            searchForPastDate: false,
            from: ruPoint,
            to: byPoint,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 13,
            },
        };

        expect(
            isRidesharingAvailableForDisplay(contextFromBy, blablacar, flags),
        ).toBe(true);
        expect(
            isRidesharingAvailableForDisplay(contextToBy, blablacar, flags),
        ).toBe(true);
    });

    it('Если показ ББК выключен, то должна вернуть false', () => {
        const context = {
            searchForPastDate: false,
            ...points,
            when: {},
        };
        const blablacar = {
            querying: false,
            tariff: {
                offersCount: 13,
            },
        };

        expect(
            isRidesharingAvailableForDisplay(context, blablacar, flags),
        ).toBe(true);
        expect(
            isRidesharingAvailableForDisplay(context, blablacar, {
                __ridesharingPartnersDisabled: true,
            }),
        ).toBe(false);
    });
});
