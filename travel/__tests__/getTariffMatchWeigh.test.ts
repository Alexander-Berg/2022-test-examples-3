import getTariffMatchWeigh from '../getTariffMatchWeigh';

describe('getTariffMatchWeigh', () => {
    it('should return 0 if base segment has no tariff keys', () => {
        const baseSegment = {};
        const tariffSegment = {key: 'a'};

        expect(getTariffMatchWeigh(baseSegment, tariffSegment)).toBe(0);
    });

    it('should return 0 if base segment tariff keys array does not contain update segment key', () => {
        const baseSegment = {tariffsKeys: ['a', 'b']};
        const tariffSegment = {key: 'c'};

        expect(getTariffMatchWeigh(baseSegment, tariffSegment)).toBe(0);
    });

    it('should return 1 if base segment tariff keys array contains update segment key', () => {
        const baseSegment = {tariffsKeys: ['a', 'b']};
        const tariffSegment = {key: 'b'};

        expect(getTariffMatchWeigh(baseSegment, tariffSegment)).toBe(1);
    });

    it('should return float if base segment tariff keys array contains one of update segment keys', () => {
        const baseSegment = {tariffsKeys: ['a', 'b']};
        const tariffSegment = {keys: ['x', 'a']};

        expect(getTariffMatchWeigh(baseSegment, tariffSegment)).toBe(0.5);
    });
});
