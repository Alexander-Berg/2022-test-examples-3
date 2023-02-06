import pricePresence from '../../hideWithoutPrice';

describe('pricePresence.isDefaultValue', () => {
    it('show segments with price only', () => {
        const result = pricePresence.isDefaultValue(true);

        expect(result).toBe(false);
    });

    it('show all segments', () => {
        const result = pricePresence.isDefaultValue(false);

        expect(result).toBe(true);
    });
});
