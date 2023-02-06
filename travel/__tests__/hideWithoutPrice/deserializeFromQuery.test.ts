import hideWithoutPrice from '../../hideWithoutPrice';

describe('hideWithoutPrice.deserializeFromQuery', () => {
    it('seats: y', () => {
        const value = hideWithoutPrice.deserializeFromQuery({seats: 'y'});

        expect(value).toBe(true);
    });

    it('seats: Y', () => {
        const value = hideWithoutPrice.deserializeFromQuery({seats: 'Y'});

        expect(value).toBe(true);
    });

    it('seats: unknown', () => {
        const value = hideWithoutPrice.deserializeFromQuery({seats: 'unknown'});

        expect(value).toBe(false);
    });

    it('no "seats" param', () => {
        // @ts-ignore
        const value = hideWithoutPrice.deserializeFromQuery({foo: 'bar'});

        expect(value).toBe(false);
    });
});
