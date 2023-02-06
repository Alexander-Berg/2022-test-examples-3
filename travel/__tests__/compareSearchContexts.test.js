const compareSearchContexts = require.requireActual(
    '../compareSearchContexts',
).default;

const contextA = {
    transportType: 'all',
    originalFrom: {title: 'Москва', key: 'c213'},
    originalTo: {title: 'Екатеринбург', key: 'c54'},
    when: {date: '22-10-2016'},
};

describe('compareSearchContexts', () => {
    it('should return true if both contexts refer to the same object', () => {
        const contextB = contextA;

        expect(compareSearchContexts(contextA, contextB)).toBe(true);
    });

    it('should return false if `from.key` fields are different', () => {
        const contextB = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'not_c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {date: '22-10-2016'},
        };

        expect(compareSearchContexts(contextA, contextB)).toBe(false);
    });

    it('should return false if `to.key` fields are different', () => {
        const contextB = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'not_c54'},
            when: {date: '22-10-2016'},
        };

        expect(compareSearchContexts(contextA, contextB)).toBe(false);
    });

    it('should return false if `transportType` fields are different', () => {
        const contextB = {
            transportType: 'plane',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {date: '22-10-2016'},
        };

        expect(compareSearchContexts(contextA, contextB)).toBe(false);
    });

    it('should return false if `when.date` fields are different', () => {
        const contextB = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {date: '23-10-2016'},
        };

        expect(compareSearchContexts(contextA, contextB)).toBe(false);
    });

    it('should return false if one of `when.date` fields is not specified', () => {
        const contextB = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {special: 'all-days'},
        };

        expect(compareSearchContexts(contextA, contextB)).toBe(false);
    });

    it('should return true if `from.key`, `to.key`, `when.date` and `transportType` fields are the same', () => {
        const contextB = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {date: '22-10-2016'},
            someOtherField: 'someValue',
        };

        expect(compareSearchContexts(contextA, contextB)).toBe(true);
    });

    it('should return true if `when.date` fields are not specified, but `when.special` fields are the same', () => {
        const contextB = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {special: 'all-days'},
        };

        const contextC = {
            transportType: 'all',
            originalFrom: {title: 'Москва', key: 'c213'},
            originalTo: {title: 'Екатеринбург', key: 'c54'},
            when: {special: 'all-days'},
        };

        expect(compareSearchContexts(contextB, contextC)).toBe(true);
    });
});
